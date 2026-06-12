package com.finsight.application.analytics;

import com.finsight.application.config.FinsightFeatureProperties;
import com.finsight.application.finance.InsightService;
import com.finsight.common.exception.AppServiceException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
        Set<String> dismissed = loadDismissedCardIds(userId);

        Map<String, Object> profile = profileService.currentProfile();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> dims = (List<Map<String, Object>>) profile.get("dimensions");
        for (Map<String, Object> dim : dims) {
            String dimId = String.valueOf(dim.get("id"));
            if (dismissed.contains(dimId)) {
                continue;
            }
            double score = ((Number) dim.get("score")).doubleValue();
            if (score >= 60) {
                continue;
            }
            cards.add(recommendation(
                    dimId,
                    80 - (int) score,
                    titleFor(dimId),
                    String.valueOf(dim.get("summary")),
                    List.of(evRef("profile", dimId)),
                    actionsFor(dimId)));
            if (cards.size() >= 3) {
                break;
            }
        }

        if (cards.size() < 3) {
            Map<String, Object> forecast = forecastService.forecast(java.time.YearMonth.now().getYear(), "base");
            @SuppressWarnings("unchecked")
            List<String> deficits = (List<String>) forecast.getOrDefault("deficitMonths", List.of());
            if (!deficits.isEmpty() && !dismissed.contains("cashflow_risk")) {
                cards.add(recommendation("cashflow_risk", 70, "Projected deficit months",
                        "Forecast shows deficit in " + deficits.get(0),
                        List.of(evRef("forecast", deficits.get(0))),
                        List.of(action("View forecast", "open_forecast", "/reports/annual-outlook"))));
            }
        }

        while (cards.size() < 3) {
            for (Map<String, Object> legacy : legacyCards()) {
                String legacyKey = "legacy:" + legacy.get("title");
                if (dismissed.contains(legacyKey)) {
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
        jdbcTemplate.update(
                "insert into fin_recommendation_feedback (id, user_id, card_id, action, created_at, snooze_until) "
                        + "values (?, ?, ?, ?, now(3), ?)",
                UUID.randomUUID().toString(), userId, cardId, action,
                "dismiss".equals(action) ? LocalDateTime.now().plusDays(7) : null);
    }

    private List<Map<String, Object>> legacyCards() throws AppServiceException {
        return insightService.decisionCards();
    }

    private Set<String> loadDismissedCardIds(String userId) {
        if (!tableExists("fin_recommendation_feedback")) {
            return Set.of();
        }
        List<String> ids = jdbcTemplate.queryForList(
                "select card_id from fin_recommendation_feedback "
                        + "where user_id = ? and action = 'dismiss' "
                        + "and (snooze_until is null or snooze_until > now(3))",
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
            String id = card.get("id") != null && !String.valueOf(card.get("id")).isBlank()
                    ? String.valueOf(card.get("id"))
                    : UUID.randomUUID().toString();
            card.put("id", id);
            jdbcTemplate.update(
                    "insert into fin_insight_card (id, user_id, card_type, priority, title, reason, impact_amount, "
                            + "evidence_json, actions_json, created_at, expires_at) "
                            + "values (?, ?, ?, ?, ?, ?, ?, ?, ?, now(3), date_add(now(3), interval 7 day))",
                    id, userId, card.get("type"), card.get("priority"), card.get("title"), card.get("reason"),
                    card.get("impactAmount"),
                    com.alibaba.fastjson.JSON.toJSONString(card.get("evidenceRefs")),
                    com.alibaba.fastjson.JSON.toJSONString(card.get("actions")));
        }
    }

    private static Map<String, Object> recommendation(String type, int priority, String title, String reason,
                                                       List<Map<String, String>> evidence,
                                                       List<Map<String, Object>> actions) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", type);
        m.put("type", type);
        m.put("priority", priority);
        m.put("title", title);
        m.put("reason", reason);
        m.put("impactAmount", BigDecimal.ZERO);
        m.put("evidenceRefs", evidence);
        m.put("actions", actions);
        return m;
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
            case "liquidity_safety" -> List.of(action("Build emergency fund", "open_wealth", "/wealth"));
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

    private static Map<String, Object> mapLegacy(Map<String, Object> legacy) {
        Map<String, Object> m = new LinkedHashMap<>(legacy);
        m.put("reason", legacy.getOrDefault("detail", legacy.get("text")));
        m.put("evidenceRefs", List.of(evRef("insight", String.valueOf(legacy.get("title")))));
        m.put("actions", List.of(action(
                String.valueOf(legacy.getOrDefault("actionLabel", "View")),
                "open_path",
                String.valueOf(legacy.getOrDefault("actionPath", "/dashboard")))));
        m.put("priority", 50);
        m.put("type", legacy.getOrDefault("type", "info"));
        return m;
    }
}
