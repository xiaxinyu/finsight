package com.finsight.application.analytics;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finsight.application.authentication.AuthenticationFacade;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class MerchantMiningService {

    private static final int MIN_MERCHANT_TXNS = 3;
    private static final int MAX_PROFILES = 200;
    private static final double OPTIMIZABLE_CONFIDENCE = 0.75;

    private final JdbcTemplate jdbcTemplate;
    private final AuthenticationFacade authenticationFacade;
    private final ObjectMapper objectMapper;

    public MerchantMiningService(JdbcTemplate jdbcTemplate,
                                 AuthenticationFacade authenticationFacade,
                                 ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.authenticationFacade = authenticationFacade;
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> refreshProfiles() {
        String userId = userKey();
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "select v.opponent_name, v.transaction_desc, v.amount, v.txn_date "
                        + "from v_transaction_analytics v "
                        + "inner join transaction t on t.id = v.id "
                        + "where v.direction = 'expense' and v.is_transfer = 0 and v.is_refund = 0 "
                        + "and v.amount > 0 and (t.created_by = ? or (? = '_anonymous' and t.created_by is null)) "
                        + "order by v.txn_date desc",
                userId, userId);

        Map<String, MerchantAggregate> aggregates = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            String raw = MerchantNormalizer.rawMerchant(
                    stringVal(row.get("opponent_name")),
                    stringVal(row.get("transaction_desc")));
            String token = MerchantNormalizer.normalizeToken(raw);
            if (token.isEmpty()) {
                continue;
            }
            LocalDate txnDate = toLocalDate(row.get("txn_date"));
            double amount = ((Number) row.get("amount")).doubleValue();
            aggregates.computeIfAbsent(token, MerchantAggregate::new)
                    .add(txnDate, amount, raw);
        }

        List<MerchantAggregate> ranked = aggregates.values().stream()
                .filter(a -> a.txnCount() >= MIN_MERCHANT_TXNS)
                .sorted(Comparator.comparingInt(MerchantAggregate::txnCount).reversed())
                .limit(MAX_PROFILES)
                .toList();

        int upserted = 0;
        int subscriptions = 0;
        for (MerchantAggregate aggregate : ranked) {
            SubscriptionDetector.SubscriptionSignal signal = SubscriptionDetector.detect(aggregate.points());
            boolean suspected = signal.suspectedSubscription();
            if (suspected) {
                subscriptions++;
            }
            String payload = toJson(enrichPayload(signal, aggregate));
            jdbcTemplate.update(
                    "insert into fin_merchant_profile (id, user_id, merchant_token, display_name, is_subscription, "
                            + "avg_amount, txn_count, last_seen, payload_json, updated_at) "
                            + "values (?, ?, ?, ?, ?, ?, ?, ?, ?, now(3)) "
                            + "on duplicate key update display_name=values(display_name), avg_amount=values(avg_amount), "
                            + "txn_count=values(txn_count), is_subscription=values(is_subscription), "
                            + "last_seen=values(last_seen), payload_json=values(payload_json), updated_at=now(3)",
                    UUID.randomUUID().toString(),
                    userId,
                    aggregate.token(),
                    aggregate.displayName(),
                    suspected ? 1 : 0,
                    round(aggregate.avgAmount()),
                    aggregate.txnCount(),
                    Date.valueOf(aggregate.lastSeen()),
                    payload);
            upserted++;
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("upserted", upserted);
        out.put("subscriptions", subscriptions);
        return out;
    }

    public Map<String, Object> subscriptionReport() {
        List<Map<String, Object>> rows = loadProfiles(true);
        double monthlyTotal = 0;
        double optimizable = 0;
        List<Map<String, Object>> subscriptions = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> item = mapSubscription(row);
            subscriptions.add(item);
            double monthly = monthlyEquivalent(item);
            monthlyTotal += monthly;
            if (confidence(item) < OPTIMIZABLE_CONFIDENCE) {
                optimizable += monthly;
            }
        }

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("count", subscriptions.size());
        summary.put("monthlyTotal", round(monthlyTotal));
        summary.put("annualizedTotal", round(monthlyTotal * 12));
        summary.put("optimizableAmount", round(optimizable * 12));

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("subscriptions", subscriptions);
        out.put("summary", summary);
        return out;
    }

    public Map<String, Object> concentration() {
        List<Map<String, Object>> rows = loadProfiles(false);
        double totalSpend = rows.stream()
                .mapToDouble(r -> totalSpend(r))
                .sum();
        List<Map<String, Object>> merchants = new ArrayList<>();
        double top1 = 0;
        double top3 = 0;
        double top5 = 0;
        for (int i = 0; i < rows.size(); i++) {
            Map<String, Object> row = rows.get(i);
            double spend = totalSpend(row);
            double share = totalSpend > 0 ? spend / totalSpend * 100 : 0;
            if (i == 0) {
                top1 = share;
            }
            if (i < 3) {
                top3 += share;
            }
            if (i < 5) {
                top5 += share;
            }
            Map<String, Object> item = new LinkedHashMap<>();
            String token = String.valueOf(row.get("merchant_token"));
            item.put("merchantToken", token);
            item.put("displayName", row.get("display_name"));
            item.put("totalSpend", round(spend));
            item.put("sharePct", round(share));
            item.put("txnCount", row.get("txn_count"));
            item.put("suspectedSubscription", asBool(row.get("is_subscription")));
            item.put("drillDown", drillMerchantYear(token, String.valueOf(row.get("display_name"))));
            merchants.add(item);
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("totalSpend", round(totalSpend));
        out.put("merchantCount", merchants.size());
        out.put("top1SharePct", round(top1));
        out.put("top5SharePct", round(top5));
        out.put("top3SharePct", round(top3));
        out.put("merchants", merchants);
        return out;
    }

    public Map<String, Object> drift(int year) {
        String userId = userKey();
        int priorYear = year - 1;
        Map<String, YearSpend> current = spendByMerchantForYear(userId, year);
        Map<String, YearSpend> prior = spendByMerchantForYear(userId, priorYear);

        List<Map<String, Object>> newMerchants = new ArrayList<>();
        List<Map<String, Object>> growingMerchants = new ArrayList<>();
        List<Map<String, Object>> decliningMerchants = new ArrayList<>();
        List<Map<String, Object>> movers = new ArrayList<>();

        java.util.Set<String> allTokens = new java.util.LinkedHashSet<>();
        allTokens.addAll(current.keySet());
        allTokens.addAll(prior.keySet());

        for (String token : allTokens) {
            double currentSpend = current.getOrDefault(token, YearSpend.empty()).spend();
            double priorSpend = prior.getOrDefault(token, YearSpend.empty()).spend();
            double delta = currentSpend - priorSpend;
            if (Math.abs(delta) < 1 && priorSpend > 0 && currentSpend > 0) {
                continue;
            }
            String displayName = current.containsKey(token)
                    ? current.get(token).displayName()
                    : prior.get(token).displayName();
            Map<String, Object> row = buildDriftRow(token, displayName, currentSpend, priorSpend, delta, year, priorYear);
            movers.add(row);
            if (priorSpend <= 0 && currentSpend > 0) {
                newMerchants.add(row);
            } else if (delta > 0) {
                growingMerchants.add(row);
            } else if (delta < 0) {
                decliningMerchants.add(row);
            }
        }

        Comparator<Map<String, Object>> byAbsDelta = Comparator.comparingDouble(
                (Map<String, Object> r) -> Math.abs(((Number) r.get("deltaAmount")).doubleValue())).reversed();
        movers.sort(byAbsDelta);
        newMerchants.sort(byAbsDelta);
        growingMerchants.sort(byAbsDelta);
        decliningMerchants.sort(byAbsDelta);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("year", year);
        out.put("priorYear", priorYear);
        out.put("movers", movers.stream().limit(20).toList());
        out.put("newMerchants", newMerchants.stream().limit(15).toList());
        out.put("growingMerchants", growingMerchants.stream().limit(15).toList());
        out.put("decliningMerchants", decliningMerchants.stream().limit(15).toList());
        return out;
    }

    /** @deprecated use {@link #subscriptionReport()} */
    public List<Map<String, Object>> subscriptions() {
        return (List<Map<String, Object>>) subscriptionReport().get("subscriptions");
    }

    private List<Map<String, Object>> loadProfiles(boolean subscriptionsOnly) {
        String sql = "select merchant_token, display_name, avg_amount, txn_count, last_seen, is_subscription, payload_json "
                + "from fin_merchant_profile where user_id = ? ";
        if (subscriptionsOnly) {
            sql += "and is_subscription = 1 ";
        }
        sql += "order by avg_amount * txn_count desc limit 100";
        return jdbcTemplate.queryForList(sql, userKey());
    }

    private Map<String, YearSpend> spendByMerchantForYear(String userId, int year) {
        AnalyticsDateRange.HalfOpen range = AnalyticsDateRange.calendarYear(year);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "select v.opponent_name, v.transaction_desc, v.amount "
                        + "from v_transaction_analytics v "
                        + "inner join transaction t on t.id = v.id "
                        + "where v.direction = 'expense' and v.is_transfer = 0 and v.is_refund = 0 "
                        + "and v.amount > 0 and v.txn_date >= ? and v.txn_date < ? "
                        + "and (t.created_by = ? or (? = '_anonymous' and t.created_by is null))",
                range.startInclusive(), range.endExclusive(), userId, userId);

        Map<String, YearSpend> out = new HashMap<>();
        for (Map<String, Object> row : rows) {
            String raw = MerchantNormalizer.rawMerchant(
                    stringVal(row.get("opponent_name")),
                    stringVal(row.get("transaction_desc")));
            String token = MerchantNormalizer.normalizeToken(raw);
            if (token.isEmpty()) {
                continue;
            }
            double amount = ((Number) row.get("amount")).doubleValue();
            out.merge(token, new YearSpend(amount, MerchantNormalizer.displayName(token, raw)),
                    (a, b) -> new YearSpend(a.spend() + b.spend(), a.displayName()));
        }
        return out;
    }

    private Map<String, Object> mapSubscription(Map<String, Object> row) {
        Map<String, Object> payload = parsePayload(row.get("payload_json"));
        Map<String, Object> item = new LinkedHashMap<>();
        String token = String.valueOf(row.get("merchant_token"));
        String displayName = String.valueOf(row.get("display_name"));
        item.put("merchantToken", token);
        item.put("displayName", displayName);
        item.put("avgAmount", row.get("avg_amount"));
        item.put("txnCount", row.get("txn_count"));
        item.put("lastSeen", row.get("last_seen"));
        item.put("suspectedSubscription", asBool(row.get("is_subscription")));
        item.put("cadence", payload.getOrDefault("cadence", "monthly"));
        item.put("confidence", payload.getOrDefault("confidence", 0.8));
        item.put("avgIntervalDays", payload.getOrDefault("avgIntervalDays", 0));
        item.put("amountCv", payload.getOrDefault("amountCv", 0));
        item.put("evidence", payload.getOrDefault("evidence", ""));
        item.put("monthlyEquivalent", round(monthlyEquivalentFromRow(row, payload)));
        item.put("drillDown", drillMerchantYear(token, displayName));
        return item;
    }

    private Map<String, Object> buildDriftRow(String token,
                                              String displayName,
                                              double currentSpend,
                                              double priorSpend,
                                              double delta,
                                              int year,
                                              int priorYear) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("merchantToken", token);
        row.put("displayName", displayName);
        row.put("currentSpend", round(currentSpend));
        row.put("priorSpend", round(priorSpend));
        row.put("deltaAmount", round(delta));
        row.put("pctChange", priorSpend > 0 ? round(delta / priorSpend * 100) : null);
        row.put("drillDown", drillMerchantRange(token, displayName, priorYear, year));
        return row;
    }

    private static Map<String, String> drillMerchantYear(String token, String displayName) {
        int year = LocalDate.now().getYear();
        return drillMerchantRange(token, displayName, year, year);
    }

    private static Map<String, String> drillMerchantRange(String token,
                                                          String displayName,
                                                          int fromYear,
                                                          int toYear) {
        Map<String, String> drill = new LinkedHashMap<>();
        drill.put("transactionDateStartStr", "01/01/" + fromYear);
        drill.put("transactionDateEndStr", "12/31/" + toYear);
        drill.put("txnTypes", "expense");
        drill.put("merchantToken", token);
        drill.put("merchantLabel", displayName);
        return drill;
    }

    private double monthlyEquivalent(Map<String, Object> item) {
        return ((Number) item.getOrDefault("monthlyEquivalent", 0)).doubleValue();
    }

    private double monthlyEquivalentFromRow(Map<String, Object> row, Map<String, Object> payload) {
        double avg = ((Number) row.get("avg_amount")).doubleValue();
        String cadence = String.valueOf(payload.getOrDefault("cadence", "monthly"));
        return switch (cadence) {
            case "quarterly" -> avg / 3;
            case "yearly" -> avg / 12;
            default -> avg;
        };
    }

    private double confidence(Map<String, Object> item) {
        return ((Number) item.getOrDefault("confidence", 0)).doubleValue();
    }

    private double totalSpend(Map<String, Object> row) {
        double avg = ((Number) row.get("avg_amount")).doubleValue();
        int count = ((Number) row.get("txn_count")).intValue();
        return avg * count;
    }

    private Map<String, Object> enrichPayload(SubscriptionDetector.SubscriptionSignal signal,
                                              MerchantAggregate aggregate) {
        Map<String, Object> payload = new LinkedHashMap<>(signal.toPayload());
        payload.put("variants", aggregate.variants());
        payload.put("evidence", buildEvidence(signal, aggregate));
        return payload;
    }

    private String buildEvidence(SubscriptionDetector.SubscriptionSignal signal, MerchantAggregate aggregate) {
        if (!signal.suspectedSubscription()) {
            return "Insufficient recurring pattern";
        }
        return aggregate.txnCount() + " charges every ~" + Math.round(signal.avgIntervalDays())
                + " days (" + signal.cadence() + "), amount CV "
                + Math.round(signal.amountCv() * 100) + "%";
    }

    private Map<String, Object> parsePayload(Object raw) {
        if (raw == null) {
            return Map.of();
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> parsed = objectMapper.readValue(String.valueOf(raw), Map.class);
            return parsed;
        } catch (JsonProcessingException e) {
            return Map.of();
        }
    }

    private String toJson(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    private static boolean asBool(Object value) {
        if (value instanceof Boolean b) {
            return b;
        }
        if (value instanceof Number n) {
            return n.intValue() != 0;
        }
        return false;
    }

    private static String stringVal(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static LocalDate toLocalDate(Object value) {
        if (value instanceof LocalDate ld) {
            return ld;
        }
        if (value instanceof Date d) {
            return d.toLocalDate();
        }
        if (value instanceof java.util.Date ud) {
            return new Date(ud.getTime()).toLocalDate();
        }
        return LocalDate.parse(String.valueOf(value).substring(0, 10));
    }

    private static double round(double v) {
        return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private String userKey() {
        String user = authenticationFacade.getUserName();
        return user == null || user.isBlank() ? "_anonymous" : user;
    }

    private record YearSpend(double spend, String displayName) {
        static YearSpend empty() {
            return new YearSpend(0, "");
        }
    }

    static final class MerchantAggregate {
        private final String token;
        private final List<SubscriptionDetector.TxnPoint> points = new ArrayList<>();
        private final Map<String, Integer> rawCounts = new HashMap<>();
        private String bestRaw = "";

        MerchantAggregate(String token) {
            this.token = token;
        }

        void add(LocalDate date, double amount, String raw) {
            points.add(new SubscriptionDetector.TxnPoint(date, amount));
            if (raw != null && !raw.isBlank()) {
                rawCounts.merge(raw, 1, Integer::sum);
                if (bestRaw.isEmpty() || rawCounts.get(raw) > rawCounts.getOrDefault(bestRaw, 0)) {
                    bestRaw = raw;
                }
            }
        }

        String token() {
            return token;
        }

        int txnCount() {
            return points.size();
        }

        double avgAmount() {
            return points.stream().mapToDouble(SubscriptionDetector.TxnPoint::amount).average().orElse(0);
        }

        LocalDate lastSeen() {
            return points.stream().map(SubscriptionDetector.TxnPoint::date).max(LocalDate::compareTo).orElse(LocalDate.now());
        }

        String displayName() {
            return MerchantNormalizer.displayName(token, bestRaw);
        }

        List<String> variants() {
            return rawCounts.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .map(Map.Entry::getKey)
                    .limit(5)
                    .toList();
        }

        List<SubscriptionDetector.TxnPoint> points() {
            return List.copyOf(points);
        }
    }
}
