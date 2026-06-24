package com.finsight.application.analytics;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finsight.application.authentication.AuthenticationFacade;
import com.finsight.application.support.ListingDateSupport;
import com.finsight.common.exception.AppServiceException;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class MerchantMiningService {

    private static final int MIN_MERCHANT_TXNS = 3;
    private static final int MAX_PROFILES = 200;
    private static final double OPTIMIZABLE_CONFIDENCE = 0.75;

    private static final int MIN_CATEGORY_MERCHANT_TXNS = 1;
    private static final Set<String> SUBSCRIPTION_CATEGORY_CODES = Set.of("FIXED-05", "EXP_SUBSCRIPTION");

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
        return subscriptionReport(null, null);
    }

    public Map<String, Object> subscriptionReport(String startStr, String endStr) {
        AnalyticsDateRange.HalfOpen range = resolvePeriod(startStr, endStr);
        List<Map<String, Object>> subscriptions = new ArrayList<>();
        Set<String> seenTokens = new LinkedHashSet<>();
        int patternCount = 0;
        int categoryOnlyCount = 0;
        double monthlyTotal = 0;
        double optimizable = 0;

        for (MerchantAggregate aggregate : aggregateMerchantsInPeriod(range).values()) {
            if (aggregate.txnCount() < MIN_MERCHANT_TXNS) {
                continue;
            }
            SubscriptionDetector.SubscriptionSignal signal = SubscriptionDetector.detect(aggregate.points());
            if (!signal.suspectedSubscription()) {
                continue;
            }
            patternCount++;
            Map<String, Object> item = mapLivePatternSubscription(aggregate, signal, range);
            subscriptions.add(item);
            seenTokens.add(aggregate.token());
            double monthly = monthlyEquivalent(item);
            monthlyTotal += monthly;
            if (confidence(item) < OPTIMIZABLE_CONFIDENCE) {
                optimizable += monthly;
            }
        }

        Map<String, Object> categorySummary = loadCategorySubscriptionSummary(range);
        for (Map<String, Object> categoryRow : loadCategorySubscriptionMerchants(range)) {
            String token = String.valueOf(categoryRow.get("merchantToken"));
            if (token.isBlank() || seenTokens.contains(token)) {
                continue;
            }
            seenTokens.add(token);
            categoryOnlyCount++;
            Map<String, Object> item = mapCategorySubscription(categoryRow, range);
            subscriptions.add(item);
            double monthly = monthlyEquivalent(item);
            monthlyTotal += monthly;
            if (confidence(item) < OPTIMIZABLE_CONFIDENCE) {
                optimizable += monthly;
            }
        }

        subscriptions.sort(Comparator.comparingDouble((Map<String, Object> item) ->
                -monthlyEquivalent(item)));

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("count", subscriptions.size());
        summary.put("patternCount", patternCount);
        summary.put("categoryOnlyCount", categoryOnlyCount);
        summary.put("monthlyTotal", round(monthlyTotal));
        summary.put("annualizedTotal", round(monthlyTotal * 12));
        summary.put("optimizableAmount", round(optimizable * 12));
        summary.put("categoryTxnCount", categorySummary.getOrDefault("txnCount", 0));
        summary.put("categoryTotalSpend", categorySummary.getOrDefault("totalSpend", 0));
        summary.put("categoryMerchantCount", categorySummary.getOrDefault("merchantCount", 0));
        summary.put("periodStart", range.startInclusive().toString());
        summary.put("periodEnd", range.endExclusive().minusDays(1).toString());

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
        item.put("detectionSource", "pattern");
        item.put("drillDown", drillMerchantYear(token, displayName));
        return item;
    }

    private Map<String, Object> mapLivePatternSubscription(MerchantAggregate aggregate,
                                                           SubscriptionDetector.SubscriptionSignal signal,
                                                           AnalyticsDateRange.HalfOpen range) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("merchantToken", aggregate.token());
        item.put("displayName", aggregate.displayName());
        item.put("avgAmount", round(aggregate.avgAmount()));
        item.put("txnCount", aggregate.txnCount());
        item.put("lastSeen", Date.valueOf(aggregate.lastSeen()));
        item.put("suspectedSubscription", true);
        item.put("cadence", signal.cadence());
        item.put("confidence", signal.confidence());
        item.put("avgIntervalDays", signal.avgIntervalDays());
        item.put("amountCv", signal.amountCv());
        item.put("evidence", buildEvidence(signal, aggregate));
        item.put("monthlyEquivalent", round(monthlyEquivalentFromCadence(aggregate.avgAmount(), signal.cadence())));
        item.put("detectionSource", "pattern");
        item.put("periodSpend", round(aggregate.totalSpend()));
        item.put("drillDown", drillMerchantPeriod(aggregate.token(), aggregate.displayName(), range));
        return item;
    }

    private Map<String, Object> mapCategorySubscription(Map<String, Object> aggregate,
                                                      AnalyticsDateRange.HalfOpen range) {
        String token = String.valueOf(aggregate.get("merchantToken"));
        String displayName = String.valueOf(aggregate.get("displayName"));
        int txnCount = ((Number) aggregate.get("txnCount")).intValue();
        double totalSpend = ((Number) aggregate.get("totalSpend")).doubleValue();
        int monthSpan = Math.max(1, ((Number) aggregate.get("monthSpan")).intValue());
        double monthlyEq = totalSpend / monthSpan;
        double conf = txnCount >= 3 ? 0.78 : txnCount >= 2 ? 0.72 : 0.65;
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("merchantToken", token);
        item.put("displayName", displayName);
        item.put("avgAmount", round(totalSpend / txnCount));
        item.put("txnCount", txnCount);
        item.put("lastSeen", aggregate.get("lastSeen"));
        item.put("suspectedSubscription", true);
        item.put("cadence", "category");
        item.put("confidence", conf);
        item.put("avgIntervalDays", 0);
        item.put("amountCv", 0);
        item.put("evidence", txnCount + " txns in subscription category · "
                + formatMoney(totalSpend) + " in period");
        item.put("monthlyEquivalent", round(monthlyEq));
        item.put("detectionSource", "category");
        item.put("periodSpend", round(totalSpend));
        item.put("drillDown", drillCategoryMerchant(token, displayName, range));
        return item;
    }

    private Map<String, Object> loadCategorySubscriptionSummary(AnalyticsDateRange.HalfOpen range) {
        String userId = userKey();
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "select count(distinct v.id) as txn_count, "
                        + "coalesce(sum(v.amount), 0) as total_spend, "
                        + "count(distinct v.merchant_token) as merchant_count "
                        + "from v_transaction_analytics v "
                        + "inner join transaction t on t.id = v.id "
                        + "where v.direction = 'expense' and v.is_transfer = 0 and v.is_refund = 0 "
                        + "and v.amount > 0 and " + subscriptionCategoryPredicate("v")
                        + "and v.txn_date >= ? and v.txn_date < ? "
                        + "and (t.created_by = ? or (? = '_anonymous' and t.created_by is null))",
                Date.valueOf(range.startInclusive()),
                Date.valueOf(range.endExclusive()),
                userId, userId);
        Map<String, Object> row = rows.isEmpty() ? Map.of() : rows.get(0);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("txnCount", intVal(row, "txn_count"));
        out.put("totalSpend", dblVal(row, "total_spend"));
        out.put("merchantCount", intVal(row, "merchant_count"));
        return out;
    }

    private List<Map<String, Object>> loadCategorySubscriptionMerchants(AnalyticsDateRange.HalfOpen range) {
        String userId = userKey();
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "select v.merchant_token, v.opponent_name, v.transaction_desc, v.amount, v.txn_date "
                        + "from v_transaction_analytics v "
                        + "inner join transaction t on t.id = v.id "
                        + "where v.direction = 'expense' and v.is_transfer = 0 and v.is_refund = 0 "
                        + "and v.amount > 0 and " + subscriptionCategoryPredicate("v")
                        + "and v.txn_date >= ? and v.txn_date < ? "
                        + "and (t.created_by = ? or (? = '_anonymous' and t.created_by is null))",
                Date.valueOf(range.startInclusive()),
                Date.valueOf(range.endExclusive()),
                userId, userId);

        Map<String, CategoryMerchantAggregate> aggregates = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            String raw = MerchantNormalizer.rawMerchant(
                    stringVal(row.get("opponent_name")),
                    stringVal(row.get("transaction_desc")));
            String token = MerchantNormalizer.normalizeToken(raw);
            if (token.isEmpty()) {
                token = stringVal(row.get("merchant_token"));
            }
            if (token.isEmpty()) {
                continue;
            }
            LocalDate txnDate = toLocalDate(row.get("txn_date"));
            double amount = ((Number) row.get("amount")).doubleValue();
            aggregates.computeIfAbsent(token, k -> new CategoryMerchantAggregate(k, raw))
                    .add(txnDate, amount, raw);
        }

        List<Map<String, Object>> out = new ArrayList<>();
        for (CategoryMerchantAggregate aggregate : aggregates.values()) {
            if (aggregate.txnCount() < MIN_CATEGORY_MERCHANT_TXNS) {
                continue;
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("merchantToken", aggregate.token());
            item.put("displayName", aggregate.displayName());
            item.put("txnCount", aggregate.txnCount());
            item.put("totalSpend", round(aggregate.totalSpend()));
            item.put("monthSpan", aggregate.monthSpan());
            item.put("lastSeen", Date.valueOf(aggregate.lastSeen()));
            out.add(item);
        }
        out.sort(Comparator.comparingDouble(r -> -((Number) r.get("totalSpend")).doubleValue()));
        return out;
    }

    private Map<String, MerchantAggregate> aggregateMerchantsInPeriod(AnalyticsDateRange.HalfOpen range) {
        Map<String, MerchantAggregate> aggregates = new LinkedHashMap<>();
        for (Map<String, Object> row : loadExpenseRowsInPeriod(range)) {
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
        return aggregates;
    }

    private List<Map<String, Object>> loadExpenseRowsInPeriod(AnalyticsDateRange.HalfOpen range) {
        String userId = userKey();
        return jdbcTemplate.queryForList(
                "select v.opponent_name, v.transaction_desc, v.amount, v.txn_date "
                        + "from v_transaction_analytics v "
                        + "inner join transaction t on t.id = v.id "
                        + "where v.direction = 'expense' and v.is_transfer = 0 and v.is_refund = 0 "
                        + "and v.amount > 0 and v.txn_date >= ? and v.txn_date < ? "
                        + "and (t.created_by = ? or (? = '_anonymous' and t.created_by is null)) "
                        + "order by v.txn_date desc",
                Date.valueOf(range.startInclusive()),
                Date.valueOf(range.endExclusive()),
                userId, userId);
    }

    private AnalyticsDateRange.HalfOpen resolvePeriod(String startStr, String endStr) {
        try {
            java.util.Date[] dates = ListingDateSupport.parseMmDdYyyyOrDefaultOneYear(startStr, endStr);
            LocalDate start = fromUtilDate(dates[0]);
            LocalDate endInclusive = fromUtilDate(dates[1]);
            return new AnalyticsDateRange.HalfOpen(start, endInclusive.plusDays(1));
        } catch (AppServiceException e) {
            return AnalyticsDateRange.calendarYear(LocalDate.now().getYear());
        }
    }

    private static LocalDate fromUtilDate(java.util.Date date) {
        return new Date(date.getTime()).toLocalDate();
    }

    private static double monthlyEquivalentFromCadence(double avgAmount, String cadence) {
        if (cadence == null) {
            return avgAmount;
        }
        return switch (cadence) {
            case "quarterly" -> avgAmount / 3;
            case "yearly" -> avgAmount / 12;
            default -> avgAmount;
        };
    }

    private static String subscriptionCategoryPredicate(String alias) {
        String codes = SUBSCRIPTION_CATEGORY_CODES.stream()
                .map(code -> "'" + code + "'")
                .collect(java.util.stream.Collectors.joining(", "));
        return "(" + alias + ".category_name like '%订阅%' "
                + "or " + alias + ".category_name like '%会员%' "
                + "or " + alias + ".category_code in (" + codes + ")) ";
    }

    private static Map<String, String> drillCategoryMerchant(String token,
                                                             String displayName,
                                                             AnalyticsDateRange.HalfOpen range) {
        Map<String, String> drill = drillMerchantPeriod(token, displayName, range);
        drill.put("consumeName", "订阅");
        return drill;
    }

    private static Map<String, String> drillMerchantPeriod(String token,
                                                           String displayName,
                                                           AnalyticsDateRange.HalfOpen range) {
        Map<String, String> drill = new LinkedHashMap<>();
        drill.put("transactionDateStartStr", formatUsDate(range.startInclusive()));
        drill.put("transactionDateEndStr", formatUsDate(range.endExclusive().minusDays(1)));
        drill.put("txnTypes", "expense");
        drill.put("merchantToken", token);
        drill.put("merchantLabel", displayName);
        return drill;
    }

    private static String formatUsDate(LocalDate date) {
        return String.format("%02d/%02d/%04d", date.getMonthValue(), date.getDayOfMonth(), date.getYear());
    }

    private static String formatMoney(double amount) {
        return String.format("¥%,.2f", amount);
    }

    private static int intVal(Map<String, Object> m, String key) {
        if (m == null || m.get(key) == null) {
            return 0;
        }
        return ((Number) m.get(key)).intValue();
    }

    private static double dblVal(Map<String, Object> m, String key) {
        if (m == null || m.get(key) == null) {
            return 0;
        }
        return ((Number) m.get(key)).doubleValue();
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

        double totalSpend() {
            return points.stream().mapToDouble(SubscriptionDetector.TxnPoint::amount).sum();
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

    static final class CategoryMerchantAggregate {
        private final String token;
        private final List<SubscriptionDetector.TxnPoint> points = new ArrayList<>();
        private final Map<String, Integer> rawCounts = new HashMap<>();
        private String bestRaw = "";
        private final Set<String> months = new LinkedHashSet<>();

        CategoryMerchantAggregate(String token, String raw) {
            this.token = token;
            if (raw != null && !raw.isBlank()) {
                bestRaw = raw;
                rawCounts.put(raw, 1);
            }
        }

        void add(LocalDate date, double amount, String raw) {
            points.add(new SubscriptionDetector.TxnPoint(date, amount));
            months.add(date.getYear() + "-" + String.format("%02d", date.getMonthValue()));
            if (raw != null && !raw.isBlank()) {
                rawCounts.merge(raw, 1, Integer::sum);
                if (rawCounts.get(raw) > rawCounts.getOrDefault(bestRaw, 0)) {
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

        double totalSpend() {
            return points.stream().mapToDouble(SubscriptionDetector.TxnPoint::amount).sum();
        }

        int monthSpan() {
            return Math.max(1, months.size());
        }

        LocalDate lastSeen() {
            return points.stream().map(SubscriptionDetector.TxnPoint::date).max(LocalDate::compareTo)
                    .orElse(LocalDate.now());
        }

        String displayName() {
            return MerchantNormalizer.displayName(token, bestRaw);
        }
    }
}
