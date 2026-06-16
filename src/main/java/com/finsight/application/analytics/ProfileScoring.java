package com.finsight.application.analytics;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Formulas, thresholds, and user-type rules for the 10-dimension financial profile.
 */
public final class ProfileScoring {

    public record UserTypeResult(String type, String explanation) {
    }

    public record ConcentrationStats(double topSharePct, String topCategoryCode, String topCategoryName, double totalExpense) {
    }

    private ProfileScoring() {
    }

    public static double scoreIncomeStability(List<Double> incomes) {
        if (incomes.size() < 2) {
            return 50;
        }
        double avg = average(incomes);
        if (avg == 0) {
            return 40;
        }
        double cv = stdDev(incomes) / avg;
        return clamp(100 - cv * 100);
    }

    public static String incomeStabilityReason(double score, List<Double> incomes) {
        if (incomes.size() < 2) {
            return "Not enough income history to judge stability yet.";
        }
        if (score >= 75) {
            return "Income has been steady month to month.";
        }
        if (score >= 50) {
            return "Income varies somewhat between months — keep a buffer for lean months.";
        }
        return "Income swings sharply; prioritize emergency cash and flexible spending.";
    }

    public static double scoreSpendingControl(double income, double expense) {
        if (income <= 0) {
            return expense > 0 ? 30 : 60;
        }
        double rate = expense / income;
        return clamp(100 - rate * 80);
    }

    public static String spendingControlReason(double score, double income, double expense) {
        if (income <= 0) {
            return expense > 0 ? "Spending exists but income is not recorded in this window." : "No spending recorded in this window.";
        }
        double rate = expense / income;
        if (score >= 75) {
            return String.format("You kept spending near %.0f%% of income — strong control.", rate * 100);
        }
        if (score >= 50) {
            return String.format("Spending ran at %.0f%% of income — room to tighten discretionary costs.", rate * 100);
        }
        return String.format("Spending exceeded %.0f%% of income — review recurring and discretionary outflows.", rate * 100);
    }

    public static double scoreSavingsDiscipline(double savingsRate, double target) {
        return clamp(savingsRate / target * 80);
    }

    public static String savingsDisciplineReason(double score, double savingsRate, double target) {
        if (score >= 75) {
            return String.format("Savings rate %.0f%% meets or beats the %.0f%% reference.", savingsRate * 100, target * 100);
        }
        if (score >= 50) {
            return String.format("Savings rate %.0f%% is below the %.0f%% target but still positive.", savingsRate * 100, target * 100);
        }
        return String.format("Savings rate %.0f%% is weak versus the %.0f%% reference.", savingsRate * 100, target * 100);
    }

    public static double scoreFixedBurden(double fixedBurdenPct, double threshold) {
        if (fixedBurdenPct <= threshold) {
            return 85;
        }
        return clamp(100 - (fixedBurdenPct - threshold) * 2);
    }

    public static String fixedBurdenReason(double score, double fixedBurdenPct, double threshold) {
        if (score >= 75) {
            return String.format("Fixed costs are %.0f%% of income — within the %.0f%% comfort zone.", fixedBurdenPct, threshold);
        }
        if (score >= 50) {
            return String.format("Fixed costs at %.0f%% leave limited room for savings.", fixedBurdenPct);
        }
        return String.format("Fixed costs above %.0f%% (now %.0f%%) squeeze monthly flexibility.", threshold, fixedBurdenPct);
    }

    public static double scoreLiquidity(double runwayMonths, double targetMonths) {
        return clamp(runwayMonths / targetMonths * 100);
    }

    public static String liquidityReason(double score, double runwayMonths, double targetMonths) {
        if (score >= 75) {
            return String.format("%.1f months of runway — above the %.0f-month safety target.", runwayMonths, targetMonths);
        }
        if (score >= 50) {
            return String.format("%.1f months of runway — build toward %.0f months.", runwayMonths, targetMonths);
        }
        return String.format("Only %.1f months of runway — prioritize cash reserves.", runwayMonths);
    }

    public static double scoreDebtPressure(double debtPressurePct) {
        return clamp(100 - debtPressurePct * 2);
    }

    public static String debtPressureReason(double score, double debtPressurePct) {
        if (score >= 75) {
            return String.format("Debt service is %.0f%% of income — manageable.", debtPressurePct);
        }
        if (score >= 50) {
            return String.format("Debt service at %.0f%% is noticeable — watch new borrowing.", debtPressurePct);
        }
        return String.format("Debt service at %.0f%% is heavy relative to income.", debtPressurePct);
    }

    public static double scoreLifestyleInflation(List<Double> expenses) {
        if (expenses.size() < 3) {
            return 60;
        }
        double first = average(expenses.subList(0, expenses.size() / 2));
        double second = average(expenses.subList(expenses.size() / 2, expenses.size()));
        if (first <= 0) {
            return 60;
        }
        double growth = (second - first) / first;
        return clamp(100 - growth * 120);
    }

    public static String lifestyleInflationReason(double score, List<Double> expenses) {
        if (expenses.size() < 3) {
            return "Need more months to detect lifestyle drift.";
        }
        double first = average(expenses.subList(0, expenses.size() / 2));
        double second = average(expenses.subList(expenses.size() / 2, expenses.size()));
        double growth = first > 0 ? (second - first) / first : 0;
        if (score >= 75) {
            return String.format("Recent spending is flat or down (%+.0f%% vs earlier months).", growth * 100);
        }
        if (score >= 50) {
            return String.format("Spending crept up %+.0f%% versus earlier months.", growth * 100);
        }
        return String.format("Spending rose %+.0f%% — lifestyle inflation may be eroding savings.", growth * 100);
    }

    public static double scoreSpendingConcentration(double topSharePct) {
        if (topSharePct <= 0) {
            return 55;
        }
        return clamp(100 - Math.max(0, topSharePct - 20) * 2);
    }

    public static String spendingConcentrationReason(double score, ConcentrationStats stats) {
        if (stats.totalExpense() <= 0) {
            return "No categorized expense history to measure concentration.";
        }
        if (score >= 75) {
            return String.format("%s is %.0f%% of spend — well diversified.", stats.topCategoryName(), stats.topSharePct());
        }
        if (score >= 50) {
            return String.format("%s accounts for %.0f%% of spend — moderate concentration.", stats.topCategoryName(), stats.topSharePct());
        }
        return String.format("%.0f%% of spend sits in %s — high concentration risk.", stats.topSharePct(), stats.topCategoryName());
    }

    public static double scoreSeasonality(List<Double> nets) {
        if (nets.size() < 2) {
            return 55;
        }
        double avg = average(nets);
        if (avg == 0) {
            return 50;
        }
        return clamp(100 - (stdDev(nets) / Math.abs(avg)) * 50);
    }

    public static String seasonalityReason(double score, List<Double> nets) {
        if (nets.size() < 2) {
            return "Not enough months to measure cashflow seasonality.";
        }
        if (score >= 75) {
            return "Net cashflow is relatively smooth across months.";
        }
        if (score >= 50) {
            return "Some month-to-month swings in net cashflow — plan for uneven months.";
        }
        return "Large month-to-month net swings — seasonality risk is elevated.";
    }

    public static double scoreDataTrust(int unclassifiedCount) {
        return clamp(100 - Math.min(90, unclassifiedCount / 5.0));
    }

    public static String dataTrustReason(double score, int unclassifiedCount, int totalCount) {
        if (score >= 75) {
            return "Transaction data is largely classified — profile signals are reliable.";
        }
        if (score >= 50) {
            return unclassifiedCount + " unclassified rows remain — classify them to sharpen this profile.";
        }
        return unclassifiedCount + " unclassified of " + totalCount + " rows — data quality is limiting accuracy.";
    }

    public static UserTypeResult classifyUserType(Map<String, Double> scores) {
        double dataTrust = scores.getOrDefault("data_trust", 50.0);
        double liquidity = scores.getOrDefault("liquidity_safety", 50.0);
        double spendingControl = scores.getOrDefault("spending_control", 50.0);
        double debt = scores.getOrDefault("debt_pressure", 50.0);
        double incomeStability = scores.getOrDefault("income_stability", 50.0);
        double lifestyle = scores.getOrDefault("lifestyle_inflation", 50.0);
        double fixedBurden = scores.getOrDefault("fixed_burden", 50.0);
        double savings = scores.getOrDefault("savings_discipline", 50.0);

        if (dataTrust < 45) {
            return new UserTypeResult("data_quality_risk",
                    "Many transactions are still unclassified, so this profile may not reflect your real habits yet.");
        }
        if (liquidity < 40 || spendingControl < 40) {
            return new UserTypeResult("cashflow_stressed",
                    "Low liquidity or high spending relative to income is putting month-to-month cashflow under pressure.");
        }
        if (debt < 40) {
            return new UserTypeResult("debt_pressure",
                    "Debt payments take a large share of income compared with a healthy baseline.");
        }
        if (incomeStability < 40) {
            return new UserTypeResult("volatile_income",
                    "Income varies sharply between months, so cash planning needs extra buffer.");
        }
        if (lifestyle < 40) {
            return new UserTypeResult("lifestyle_inflation",
                    "Recent spending has grown faster than earlier months in the lookback window.");
        }
        if (fixedBurden < 45) {
            return new UserTypeResult("high_fixed_burden",
                    "Fixed obligations consume a large share of income, leaving less room to maneuver.");
        }
        if (savings >= 75 && spendingControl >= 60) {
            return new UserTypeResult("disciplined_saver",
                    "You save consistently while keeping spending in check versus income.");
        }
        return new UserTypeResult("balanced",
                "No single risk dominates — strengths and watch areas are relatively balanced.");
    }

    public static String levelLabel(double score) {
        if (score >= 75) {
            return "strong";
        }
        if (score >= 50) {
            return "moderate";
        }
        return "needs_attention";
    }

    public static ConcentrationStats concentrationFromRows(List<Map<String, Object>> rows) {
        double total = 0;
        String topCode = "";
        String topName = "Top category";
        double topAmount = 0;
        for (Map<String, Object> row : rows) {
            double amount = ((Number) row.get("amount")).doubleValue();
            total += amount;
            if (amount > topAmount) {
                topAmount = amount;
                topCode = String.valueOf(row.get("category_code"));
                topName = String.valueOf(row.get("category_name"));
            }
        }
        double share = total > 0 ? topAmount / total * 100 : 0;
        return new ConcentrationStats(round(share), topCode, topName, total);
    }

    public static Map<String, Double> scoresFromDimensions(List<Map<String, Object>> dimensions) {
        Map<String, Double> scores = new LinkedHashMap<>();
        for (Map<String, Object> dim : dimensions) {
            scores.put(String.valueOf(dim.get("id")), ((Number) dim.get("score")).doubleValue());
        }
        return scores;
    }

    public static double round(double v) {
        return BigDecimal.valueOf(v).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }

    private static double clamp(double v) {
        return Math.max(0, Math.min(100, v));
    }

    private static double average(List<Double> values) {
        return values.stream().mapToDouble(Double::doubleValue).average().orElse(0);
    }

    private static double stdDev(List<Double> values) {
        double avg = average(values);
        double var = values.stream().mapToDouble(v -> (v - avg) * (v - avg)).average().orElse(0);
        return Math.sqrt(var);
    }
}
