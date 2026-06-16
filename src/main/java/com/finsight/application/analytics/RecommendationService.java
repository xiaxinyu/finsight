package com.finsight.application.analytics;

import com.finsight.application.config.FinsightFeatureProperties;
import com.finsight.application.finance.InsightService;
import com.finsight.common.exception.AppServiceException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class RecommendationService {

    private final FinsightFeatureProperties features;
    private final InsightService insightService;
    private final FinancialProfileService profileService;
    private final ForecastService forecastService;
    private final TrendAnalysisService trendAnalysisService;
    private final JdbcTemplate jdbcTemplate;

    public RecommendationService(FinsightFeatureProperties features,
                                   InsightService insightService,
                                   FinancialProfileService profileService,
                                   ForecastService forecastService,
                                   TrendAnalysisService trendAnalysisService,
                                   JdbcTemplate jdbcTemplate) {
        this.features = features;
        this.insightService = insightService;
        this.profileService = profileService;
        this.forecastService = forecastService;
        this.trendAnalysisService = trendAnalysisService;
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Map<String, Object>> topRecommendations(String userId) throws Exception {
        if (!features.getAdvisor().isEnabled()) {
            return legacyCards();
        }
        List<Map<String, Object>> cards = new ArrayList<>();
        Set<String> suppressed = loadSuppressedCardIds(userId);

        Map<String, Object> profile = profileService.currentProfile();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> dims = (List<Map<String, Object>>) profile.get("dimensions");
        for (Map<String, Object> dim : dims) {
            String dimId = String.valueOf(dim.get("id"));
            if (suppressed.contains(dimId)) {
                continue;
            }
            double score = ((Number) dim.get("score")).doubleValue();
            if (score >= 60) {
                continue;
            }
            cards.add(recommendationFromDimension(dim));
            if (cards.size() >= 3) {
                break;
            }
        }

        if (cards.size() < 3) {
            Map<String, Object> forecast = forecastService.forecast(YearMonth.now().getYear(), "base");
            @SuppressWarnings("unchecked")
            List<String> deficits = (List<String>) forecast.getOrDefault("deficitMonths", List.of());
            if (!deficits.isEmpty() && !suppressed.contains("cashflow_risk")) {
                cards.add(cashflowRiskCard(forecast, deficits));
            }
        }

        while (cards.size() < 3) {
            for (Map<String, Object> legacy : legacyCards()) {
                String legacyKey = "legacy:" + legacy.get("title");
                if (suppressed.contains(legacyKey)) {
                    continue;
                }
                Map<String, Object> mapped = mapLegacy(legacy);
                mapped.put("id", legacyKey);
                cards.add(mapped);
                if (cards.size() >= 3) {
                    break;
                }
            }
            break;
        }

        persistCards(userId, cards);
        return cards.stream().limit(3).toList();
    }

    public void feedback(String userId, String cardId, String action) {
        LocalDateTime snoozeUntil = switch (action == null ? "" : action) {
            case "dismiss" -> LocalDateTime.now().plusDays(7);
            case "snooze" -> LocalDateTime.now().plusDays(1);
            case "accept" -> null;
            default -> LocalDateTime.now().plusDays(7);
        };
        jdbcTemplate.update(
                "insert into fin_recommendation_feedback (id, user_id, card_id, action, created_at, snooze_until) "
                        + "values (?, ?, ?, ?, now(3), ?)",
                UUID.randomUUID().toString(), userId, cardId, action, snoozeUntil);
    }

    private List<Map<String, Object>> legacyCards() throws AppServiceException {
        return insightService.decisionCards();
    }

    private Set<String> loadSuppressedCardIds(String userId) {
        if (!tableExists("fin_recommendation_feedback")) {
            return Set.of();
        }
        List<String> ids = jdbcTemplate.queryForList(
                "select card_id from fin_recommendation_feedback where user_id = ? and ("
                        + "(action = 'accept') or "
                        + "(action in ('dismiss', 'snooze') and snooze_until is not null and snooze_until > now(3))"
                        + ")",
                String.class,
                userId);
        return new HashSet<>(ids);
    }

    private boolean tableExists(String table) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.tables "
                        + "where table_schema = database() and table_name = ?",
                Integer.class,
                table);
        return count != null && count > 0;
    }

    private void persistCards(String userId, List<Map<String, Object>> cards) {
        if (!tableExists("fin_insight_card")) {
            return;
        }
        for (Map<String, Object> card : cards) {
            String cardKey = card.get("id") != null && !String.valueOf(card.get("id")).isBlank()
                    ? String.valueOf(card.get("id"))
                    : UUID.randomUUID().toString();
            if (card.get("id") == null || String.valueOf(card.get("id")).isBlank()) {
                card.put("id", cardKey);
            }
            String storageId = storageId(userId, cardKey);
            jdbcTemplate.update(
                    "insert into fin_insight_card (id, user_id, card_type, priority, title, reason, impact_amount, "
                            + "evidence_json, actions_json, created_at, expires_at) "
                            + "values (?, ?, ?, ?, ?, ?, ?, ?, ?, now(3), date_add(now(3), interval 7 day)) "
                            + "on duplicate key update "
                            + "user_id = values(user_id), card_type = values(card_type), priority = values(priority), "
                            + "title = values(title), reason = values(reason), impact_amount = values(impact_amount), "
                            + "evidence_json = values(evidence_json), actions_json = values(actions_json), "
                            + "created_at = now(3), expires_at = date_add(now(3), interval 7 day)",
                    storageId, userId, card.get("type"), card.get("priority"), card.get("title"), card.get("reason"),
                    card.get("impactAmount"),
                    com.alibaba.fastjson.JSON.toJSONString(card.get("evidence")),
                    com.alibaba.fastjson.JSON.toJSONString(card.get("actions")));
        }
    }

    static String storageId(String userId, String cardKey) {
        return userId + ":" + cardKey;
    }

    private Map<String, Object> recommendationFromDimension(Map<String, Object> dim) {
        String dimId = String.valueOf(dim.get("id"));
        double score = ((Number) dim.get("score")).doubleValue();
        int priority = Math.max(1, 80 - (int) Math.round(score));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> evidence = (List<Map<String, Object>>) dim.getOrDefault("evidence", List.of());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> dimActions = (List<Map<String, Object>>) dim.getOrDefault("actions", actionsFor(dimId));
        return recommendation(
                dimId,
                priority,
                urgencyFor(priority),
                confidenceFromScore(score),
                titleFor(dimId),
                String.valueOf(dim.get("summary")),
                impactFromDimension(dimId, score, evidence),
                evidenceRefsFrom(evidence),
                dimActions.isEmpty() ? actionsFor(dimId) : dimActions,
                evidence);
    }

    private Map<String, Object> cashflowRiskCard(Map<String, Object> forecast, List<String> deficits) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> months = (List<Map<String, Object>>) forecast.getOrDefault("months", List.of());
        double impact = 0;
        List<Map<String, Object>> evidence = new ArrayList<>();
        for (String ym : deficits) {
            for (Map<String, Object> month : months) {
                if (ym.equals(String.valueOf(month.get("yearMonth")))) {
                    double net = ((Number) month.get("net")).doubleValue();
                    if (net < 0) {
                        impact += Math.abs(net);
                    }
                    evidence.add(evidenceItem("forecast", ym, "Projected net " + ym,
                            "Forecast month runs below zero under base scenario",
                            formatMoney(net)));
                }
            }
        }
        if (evidence.isEmpty()) {
            evidence.add(evidenceItem("forecast", deficits.get(0), "Deficit month",
                    "Forecast projects negative cash flow", deficits.get(0)));
        }
        int priority = 70;
        return recommendation(
                "cashflow_risk",
                priority,
                urgencyFor(priority),
                0.78,
                "Projected deficit months",
                "Forecast shows deficit in " + String.join(", ", deficits),
                BigDecimal.valueOf(impact).setScale(2, RoundingMode.HALF_UP),
                List.of(evRef("forecast", deficits.get(0))),
                List.of(action("View annual outlook", "open_forecast", "/reports/annual-outlook"),
                        action("Cash risk calendar", "open_report", "/reports/cash-risk")),
                evidence);
    }

    private static Map<String, Object> recommendation(String type,
                                                      int priority,
                                                      String urgency,
                                                      double confidence,
                                                      String title,
                                                      String reason,
                                                      BigDecimal impactAmount,
                                                      List<Map<String, String>> evidenceRefs,
                                                      List<Map<String, Object>> actions,
                                                      List<Map<String, Object>> evidence) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", type);
        m.put("type", type);
        m.put("priority", priority);
        m.put("urgency", urgency);
        m.put("confidence", round(confidence));
        m.put("title", title);
        m.put("reason", reason);
        m.put("detail", reason);
        m.put("impactAmount", impactAmount);
        m.put("evidenceRefs", evidenceRefs);
        m.put("evidence", evidence);
        m.put("actions", actions);
        m.put("expiresAt", LocalDateTime.now().plusDays(7).toString());
        return m;
    }

    private static List<Map<String, String>> evidenceRefsFrom(List<Map<String, Object>> evidence) {
        List<Map<String, String>> refs = new ArrayList<>();
        for (Map<String, Object> item : evidence) {
            refs.add(evRef(String.valueOf(item.get("source")), String.valueOf(item.get("ref"))));
        }
        return refs;
    }

    private static Map<String, Object> evidenceItem(String source, String ref, String label, String detail, Object value) {
        Map<String, Object> e = new LinkedHashMap<>();
        e.put("source", source);
        e.put("ref", ref);
        e.put("label", label);
        e.put("detail", detail);
        e.put("value", value);
        return e;
    }

    private static Map<String, String> evRef(String source, String ref) {
        return Map.of("source", source, "ref", ref);
    }

    private static Map<String, Object> action(String label, String type, String path) {
        Map<String, Object> a = new LinkedHashMap<>();
        a.put("label", label);
        a.put("type", type);
        a.put("payload", Map.of("path", path));
        return a;
    }

    private static List<Map<String, Object>> actionsFor(String dimId) {
        return switch (dimId) {
            case "data_trust" -> List.of(action("Review transactions", "open_transactions", "/transactions?unclassified=1"));
            case "liquidity_safety" -> List.of(action("Cash risk report", "open_report", "/reports/cash-risk"));
            default -> List.of(action("Open planning", "adjust_budget", "/planning"));
        };
    }

    private static String titleFor(String dimId) {
        return switch (dimId) {
            case "data_trust" -> "Improve data quality";
            case "liquidity_safety" -> "Strengthen liquidity";
            case "fixed_burden" -> "Reduce fixed burden";
            case "savings_discipline" -> "Boost savings discipline";
            default -> "Review " + dimId.replace('_', ' ');
        };
    }

    private static String urgencyFor(int priority) {
        if (priority >= 70) {
            return "high";
        }
        if (priority >= 50) {
            return "medium";
        }
        return "low";
    }

    private static double confidenceFromScore(double score) {
        return Math.min(0.95, 0.55 + Math.max(0, 60 - score) / 80.0);
    }

    private static BigDecimal impactFromDimension(String dimId, double score, List<Map<String, Object>> evidence) {
        double gap = Math.max(0, 60 - score);
        double base = gap * 400;
        if ("data_trust".equals(dimId) && !evidence.isEmpty()) {
            Object value = evidence.get(0).get("value");
            if (value != null) {
                String text = String.valueOf(value);
                int uncls = parseLeadingInt(text);
                if (uncls > 0) {
                    return BigDecimal.valueOf(uncls * 120.0).setScale(2, RoundingMode.HALF_UP);
                }
            }
        }
        if ("liquidity_safety".equals(dimId)) {
            base = gap * 800;
        }
        if ("fixed_burden".equals(dimId)) {
            base = gap * 600;
        }
        return BigDecimal.valueOf(base).setScale(2, RoundingMode.HALF_UP);
    }

    private static int parseLeadingInt(String text) {
        StringBuilder digits = new StringBuilder();
        for (char c : text.toCharArray()) {
            if (Character.isDigit(c)) {
                digits.append(c);
            } else if (digits.length() > 0) {
                break;
            }
        }
        if (digits.length() == 0) {
            return 0;
        }
        return Integer.parseInt(digits.toString());
    }

    private static Map<String, Object> mapLegacy(Map<String, Object> legacy) {
        double metric = legacy.get("metric") instanceof Number n ? n.doubleValue() : 0;
        double threshold = legacy.get("threshold") instanceof Number n ? n.doubleValue() : 0;
        BigDecimal impact = BigDecimal.valueOf(Math.abs(metric - threshold) * 100)
                .setScale(2, RoundingMode.HALF_UP);
        int priority = "warning".equals(legacy.get("severity")) || "warning".equals(legacy.get("type")) ? 65 : 45;
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", legacy.getOrDefault("type", "info"));
        m.put("priority", priority);
        m.put("urgency", urgencyFor(priority));
        m.put("confidence", 0.72);
        m.put("title", legacy.get("title"));
        m.put("reason", legacy.getOrDefault("detail", legacy.get("text")));
        m.put("detail", legacy.getOrDefault("detail", legacy.get("text")));
        m.put("impactAmount", impact);
        m.put("evidenceRefs", List.of(evRef("insight", String.valueOf(legacy.get("title")))));
        m.put("evidence", List.of(evidenceItem(
                "insight",
                String.valueOf(legacy.get("title")),
                String.valueOf(legacy.get("title")),
                String.valueOf(legacy.getOrDefault("detail", legacy.get("text"))),
                metric)));
        m.put("actions", List.of(action(
                String.valueOf(legacy.getOrDefault("actionLabel", "View")),
                "open_path",
                String.valueOf(legacy.getOrDefault("actionPath", "/dashboard")))));
        m.put("expiresAt", LocalDateTime.now().plusDays(7).toString());
        return m;
    }

    private static double round(double v) {
        return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private static String formatMoney(double amount) {
        return "¥" + BigDecimal.valueOf(amount).setScale(0, RoundingMode.HALF_UP).toPlainString();
    }
}
