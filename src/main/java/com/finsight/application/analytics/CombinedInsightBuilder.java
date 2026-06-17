package com.finsight.application.analytics;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds advisor cards that link profile archetype, trend drivers, forecast pressure, and merchant evidence.
 */
public final class CombinedInsightBuilder {

    private CombinedInsightBuilder() {
    }

    public static List<Map<String, Object>> build(
            Map<String, Object> profile,
            Map<String, Object> trends,
            Map<String, Object> forecast,
            Map<String, Object> subscriptionReport,
            Map<String, Object> concentration) {
        List<Map<String, Object>> cards = new ArrayList<>();
        String userType = String.valueOf(profile.getOrDefault("userType", "balanced"));
        String archetype = userTypeLabel(userType);
        String archetypeDetail = String.valueOf(profile.getOrDefault("userTypeExplanation", archetype));

        Map<String, Object> archetypeTrend = buildArchetypeTrendCard(archetype, archetypeDetail, trends);
        if (archetypeTrend != null) {
            cards.add(archetypeTrend);
        }

        Map<String, Object> forecastPressure = buildForecastPressureCard(archetype, archetypeDetail, forecast, trends);
        if (forecastPressure != null) {
            cards.add(forecastPressure);
        }

        Map<String, Object> subscriptionCard = buildSubscriptionCard(archetype, subscriptionReport, concentration);
        if (subscriptionCard != null) {
            cards.add(subscriptionCard);
        }

        Map<String, Object> dataQuality = buildDataQualityCard(profile, trends);
        if (dataQuality != null) {
            cards.add(dataQuality);
        }

        return cards;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> buildArchetypeTrendCard(
            String archetype,
            String archetypeDetail,
            Map<String, Object> trends) {
        if (trends == null || trends.isEmpty()) {
            return null;
        }
        Map<String, Object> summary = (Map<String, Object>) trends.getOrDefault("summary", Map.of());
        Map<String, Object> expense = (Map<String, Object>) summary.getOrDefault("expense", Map.of());
        double expenseDelta = num(expense.get("deltaAmount"));
        List<Map<String, Object>> categories = list(trends.get("topCategoryGrowth"));
        List<Map<String, Object>> merchants = list(trends.get("topMerchantMovers"));
        if (categories.isEmpty() && merchants.isEmpty() && Math.abs(expenseDelta) < 100) {
            return null;
        }

        int fromYear = intVal(trends.get("fromYear"));
        int toYear = intVal(trends.get("toYear"));
        String driverLine = buildDriverLine(categories, merchants);
        String reason = String.format(
                "You currently fit the %s profile. %d→%d expense changed %s. %s",
                archetype,
                fromYear,
                toYear,
                formatSignedMoney(expenseDelta),
                driverLine);

        List<Map<String, Object>> evidence = new ArrayList<>();
        evidence.add(evidence("profile", userType(archetype), "User archetype", archetypeDetail, archetype));
        evidence.add(evidence("trend", "expense_delta", "Expense change",
                String.format("%d vs %d total expense shift", fromYear, toYear),
                formatSignedMoney(expenseDelta)));
        appendMoverEvidence(evidence, categories, merchants);

        List<Map<String, Object>> sections = List.of(
                section("profile", "Profile", archetype + " — " + archetypeDetail),
                section("trend", "Trend", reason));

        return card(
                "combined_archetype_trend",
                72,
                0.84,
                "YoY spending drivers",
                reason,
                impactFromDelta(expenseDelta),
                sections,
                evidence,
                List.of(
                        action("Open trend changes", "open_report", "/reports/trend-changes"),
                        action("Review cashflow", "open_report", "/reports/cashflow"),
                        action("Adjust budget", "open_planning", "/planning")));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> buildForecastPressureCard(
            String archetype,
            String archetypeDetail,
            Map<String, Object> forecast,
            Map<String, Object> trends) {
        if (forecast == null || forecast.isEmpty()) {
            return null;
        }
        List<String> deficits = stringList(forecast.get("deficitMonths"));
        if (deficits.isEmpty()) {
            return null;
        }
        List<Map<String, Object>> months = list(forecast.get("months"));
        double deficitImpact = 0;
        List<Map<String, Object>> evidence = new ArrayList<>();
        evidence.add(evidence("profile", userType(archetype), "User archetype", archetypeDetail, archetype));
        for (String ym : deficits) {
            for (Map<String, Object> month : months) {
                if (ym.equals(String.valueOf(month.get("yearMonth")))) {
                    double net = num(month.get("net"));
                    if (net < 0) {
                        deficitImpact += Math.abs(net);
                    }
                    evidence.add(evidence("forecast", ym, "Projected net " + ym,
                            "Base scenario projects negative cash flow",
                            formatMoney(net)));
                }
            }
        }
        if (evidence.size() <= 1) {
            evidence.add(evidence("forecast", deficits.get(0), "Deficit month",
                    "Forecast projects cash pressure", deficits.get(0)));
        }

        Map<String, Object> budgetSuggestion = (Map<String, Object>) forecast.get("budgetSuggestion");
        String budgetLine = "";
        if (budgetSuggestion != null && budgetSuggestion.get("monthlyCap") != null) {
            budgetLine = " Suggested monthly cap: " + formatMoney(num(budgetSuggestion.get("monthlyCap"))) + ".";
        }

        String trendHint = "";
        if (trends != null) {
            Map<String, Object> summary = (Map<String, Object>) trends.getOrDefault("summary", Map.of());
            Map<String, Object> expense = (Map<String, Object>) summary.getOrDefault("expense", Map.of());
            double expenseDelta = num(expense.get("deltaAmount"));
            if (Math.abs(expenseDelta) >= 100) {
                trendHint = " Recent expense trend " + formatSignedMoney(expenseDelta) + " adds pressure.";
            }
        }

        String reason = String.format(
                "%s profile: forecast shows deficit in %s under the base scenario.%s%s",
                archetype,
                String.join(", ", deficits),
                trendHint,
                budgetLine);

        List<Map<String, Object>> sections = List.of(
                section("profile", "Profile", archetype + " — " + archetypeDetail),
                section("forecast", "Forecast", "Deficit months: " + String.join(", ", deficits) + budgetLine),
                section("trend", "Trend", trendHint.isBlank() ? "Review YoY expense movers in Trend Changes." : trendHint.trim()));

        return card(
                "combined_forecast_pressure",
                78,
                0.8,
                "Forecast deficit pressure",
                reason,
                BigDecimal.valueOf(deficitImpact).setScale(2, RoundingMode.HALF_UP),
                sections,
                evidence,
                List.of(
                        action("Annual outlook", "open_forecast", "/reports/annual-outlook"),
                        action("Cash risk calendar", "open_report", "/reports/cash-risk"),
                        action("Open planning", "open_planning", "/planning")));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> buildSubscriptionCard(
            String archetype,
            Map<String, Object> subscriptionReport,
            Map<String, Object> concentration) {
        if (subscriptionReport == null || subscriptionReport.isEmpty()) {
            return null;
        }
        List<Map<String, Object>> subscriptions = list(subscriptionReport.get("subscriptions"));
        Map<String, Object> summary = (Map<String, Object>) subscriptionReport.getOrDefault("summary", Map.of());
        int count = intVal(summary.get("count"));
        double monthlyTotal = num(summary.get("monthlyTotal"));
        if (count == 0 || monthlyTotal < 50) {
            return null;
        }

        List<Map<String, Object>> evidence = new ArrayList<>();
        evidence.add(evidence("merchant", "subscriptions", "Recurring spend",
                count + " suspected subscriptions",
                formatMoney(monthlyTotal) + "/mo"));
        double top3Share = concentration != null ? num(concentration.get("top3SharePct")) : 0;
        if (top3Share > 0) {
            evidence.add(evidence("merchant", "concentration", "Merchant concentration",
                    "Top 3 merchants share of tracked spend",
                    String.format("%.1f%%", top3Share)));
        }
        for (int i = 0; i < Math.min(3, subscriptions.size()); i++) {
            Map<String, Object> sub = subscriptions.get(i);
            evidence.add(evidence(
                    "merchant",
                    String.valueOf(sub.getOrDefault("merchantToken", "sub-" + i)),
                    String.valueOf(sub.getOrDefault("displayName", "Subscription")),
                    String.valueOf(sub.getOrDefault("cadence", "recurring")),
                    formatMoney(monthlyEquivalent(sub))));
        }

        String topNames = subscriptions.stream()
                .limit(3)
                .map(s -> String.valueOf(s.getOrDefault("displayName", "merchant")))
                .reduce((a, b) -> a + ", " + b)
                .orElse("recurring merchants");

        String reason = String.format(
                "%s profile: %d recurring charges total %s/mo. Top items: %s.",
                archetype,
                count,
                formatMoney(monthlyTotal),
                topNames);

        List<Map<String, Object>> sections = List.of(
                section("profile", "Profile", archetype),
                section("merchant", "Subscriptions", reason));

        return card(
                "combined_subscription_review",
                68,
                0.76,
                "Review recurring charges",
                reason,
                BigDecimal.valueOf(monthlyTotal * 12).setScale(2, RoundingMode.HALF_UP),
                sections,
                evidence,
                List.of(
                        action("Subscriptions report", "open_report", "/reports/subscriptions"),
                        action("Merchant concentration", "open_report", "/reports/merchant-concentration"),
                        action("Set budget cap", "open_planning", "/planning")));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> buildDataQualityCard(Map<String, Object> profile, Map<String, Object> trends) {
        String userType = String.valueOf(profile.getOrDefault("userType", "balanced"));
        String archetype = userTypeLabel(userType);
        String archetypeDetail = String.valueOf(profile.getOrDefault("userTypeExplanation", archetype));
        List<Map<String, Object>> dims = list(profile.get("dimensions"));
        Map<String, Object> dataTrust = dims.stream()
                .filter(d -> "data_trust".equals(String.valueOf(d.get("id"))))
                .findFirst()
                .orElse(null);
        if (dataTrust == null) {
            return null;
        }
        double score = num(dataTrust.get("score"));
        if (score >= 60) {
            return null;
        }
        List<Map<String, Object>> dimEvidence = list(dataTrust.get("evidence"));
        List<Map<String, Object>> evidence = new ArrayList<>(dimEvidence);
        if (trends != null && !list(trends.get("topCategoryGrowth")).isEmpty()) {
            evidence.add(evidence("trend", "movers", "Trend analysis available",
                    "Classify more rows to sharpen category movers",
                    "Trend Changes"));
        }

        String reason = String.format(
                "Data trust score is %.0f — classify transactions before trusting trend and forecast insights. %s",
                score,
                String.valueOf(dataTrust.getOrDefault("reason", dataTrust.get("summary"))));

        List<Map<String, Object>> sections = List.of(
                section("profile", "Data trust", reason),
                section("evidence", "Evidence", dimEvidence.isEmpty() ? "Unclassified rows reduce insight confidence." :
                        String.valueOf(dimEvidence.get(0).getOrDefault("value", ""))));

        return card(
                "combined_data_quality",
                74,
                0.88,
                "Improve data before acting",
                reason,
                impactFromScore(score),
                sections,
                evidence,
                List.of(
                        action("Review unclassified", "open_transactions", "/transactions?unclassified=1"),
                        action("Tune rules", "open_rules", "/admin/rules"),
                        action("Open profile", "open_profile", "/profile")));
    }

    private static void appendMoverEvidence(
            List<Map<String, Object>> evidence,
            List<Map<String, Object>> categories,
            List<Map<String, Object>> merchants) {
        for (int i = 0; i < Math.min(2, categories.size()); i++) {
            Map<String, Object> row = categories.get(i);
            evidence.add(evidence(
                    "trend",
                    String.valueOf(row.getOrDefault("categoryCode", "cat-" + i)),
                    String.valueOf(row.getOrDefault("categoryName", "Category")),
                    "YoY category contribution",
                    formatSignedMoney(num(row.get("deltaAmount"))) + " · "
                            + String.format("%.1f%%", num(row.get("contributionPct"))) + " of shift"));
        }
        for (int i = 0; i < Math.min(2, merchants.size()); i++) {
            Map<String, Object> row = merchants.get(i);
            evidence.add(evidence(
                    "merchant",
                    String.valueOf(row.getOrDefault("merchantToken", row.getOrDefault("key", "m-" + i))),
                    String.valueOf(row.getOrDefault("label", "Merchant")),
                    "YoY merchant contribution",
                    formatSignedMoney(num(row.get("deltaAmount"))) + " · "
                            + String.format("%.1f%%", num(row.get("contributionPct"))) + " of shift"));
        }
    }

    private static String buildDriverLine(List<Map<String, Object>> categories, List<Map<String, Object>> merchants) {
        List<String> parts = new ArrayList<>();
        if (!categories.isEmpty()) {
            Map<String, Object> top = categories.get(0);
            parts.add(String.valueOf(top.getOrDefault("categoryName", "top category"))
                    + " (" + String.format("%.0f%%", num(top.get("contributionPct"))) + " of shift)");
        }
        if (!merchants.isEmpty()) {
            Map<String, Object> top = merchants.get(0);
            parts.add(String.valueOf(top.getOrDefault("label", "top merchant"))
                    + " (" + String.format("%.0f%%", num(top.get("contributionPct"))) + " of shift)");
        }
        if (parts.isEmpty()) {
            return "Review trend breakdown for category and merchant movers.";
        }
        return "Top drivers: " + String.join("; ", parts) + ".";
    }

    private static Map<String, Object> card(
            String id,
            int priority,
            double confidence,
            String title,
            String reason,
            BigDecimal impact,
            List<Map<String, Object>> sections,
            List<Map<String, Object>> evidence,
            List<Map<String, Object>> actions) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id);
        m.put("type", "combined");
        m.put("combinedKind", id);
        m.put("priority", priority);
        m.put("urgency", priority >= 70 ? "high" : priority >= 55 ? "medium" : "low");
        m.put("confidence", round(confidence));
        m.put("title", title);
        m.put("reason", reason);
        m.put("detail", reason);
        m.put("impactAmount", impact);
        m.put("sections", sections);
        m.put("evidence", evidence);
        m.put("evidenceRefs", evidenceRefs(evidence));
        m.put("actions", actions);
        return m;
    }

    private static List<Map<String, String>> evidenceRefs(List<Map<String, Object>> evidence) {
        List<Map<String, String>> refs = new ArrayList<>();
        for (Map<String, Object> item : evidence) {
            refs.add(Map.of(
                    "source", String.valueOf(item.get("source")),
                    "ref", String.valueOf(item.get("ref"))));
        }
        return refs;
    }

    private static Map<String, Object> section(String key, String title, String body) {
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("key", key);
        s.put("title", title);
        s.put("body", body);
        return s;
    }

    private static Map<String, Object> evidence(String source, String ref, String label, String detail, Object value) {
        Map<String, Object> e = new LinkedHashMap<>();
        e.put("source", source);
        e.put("ref", ref);
        e.put("label", label);
        e.put("detail", detail);
        e.put("value", value);
        return e;
    }

    private static Map<String, Object> action(String label, String type, String path) {
        Map<String, Object> a = new LinkedHashMap<>();
        a.put("label", label);
        a.put("type", type);
        a.put("payload", Map.of("path", path));
        return a;
    }

    private static String userType(String archetype) {
        return archetype.toLowerCase().replace(' ', '_');
    }

    private static String userTypeLabel(String code) {
        return switch (code) {
            case "disciplined_saver" -> "Disciplined saver";
            case "high_fixed_burden" -> "High fixed burden";
            case "cashflow_stressed" -> "Cashflow stressed";
            case "volatile_income" -> "Volatile income";
            case "lifestyle_inflation" -> "Lifestyle inflation";
            case "debt_pressure" -> "Debt pressure";
            case "data_quality_risk" -> "Data quality risk";
            default -> "Balanced";
        };
    }

    private static BigDecimal impactFromDelta(double delta) {
        return BigDecimal.valueOf(Math.abs(delta)).setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal impactFromScore(double score) {
        return BigDecimal.valueOf(Math.max(0, 60 - score) * 150).setScale(2, RoundingMode.HALF_UP);
    }

    private static double monthlyEquivalent(Map<String, Object> sub) {
        Object v = sub.get("monthlyEquivalent");
        if (v instanceof Number n) {
            return n.doubleValue();
        }
        return num(sub.get("avgAmount"));
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> list(Object raw) {
        if (raw instanceof List<?> list) {
            List<Map<String, Object>> out = new ArrayList<>();
            for (Object item : list) {
                if (item instanceof Map<?, ?> map) {
                    out.add((Map<String, Object>) map);
                }
            }
            return out;
        }
        return List.of();
    }

    private static List<String> stringList(Object raw) {
        if (raw instanceof List<?> list) {
            List<String> out = new ArrayList<>();
            for (Object item : list) {
                out.add(String.valueOf(item));
            }
            return out;
        }
        return List.of();
    }

    private static double num(Object v) {
        if (v instanceof Number n) {
            return n.doubleValue();
        }
        return 0;
    }

    private static int intVal(Object v) {
        if (v instanceof Number n) {
            return n.intValue();
        }
        return 0;
    }

    private static double round(double v) {
        return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private static String formatMoney(double amount) {
        return "¥" + BigDecimal.valueOf(amount).setScale(0, RoundingMode.HALF_UP).toPlainString();
    }

    private static String formatSignedMoney(double amount) {
        return (amount >= 0 ? "+" : "") + formatMoney(amount);
    }
}
