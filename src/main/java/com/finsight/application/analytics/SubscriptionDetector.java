package com.finsight.application.analytics;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Heuristic subscription detector: recurring cadence + stable amounts + repeated merchant.
 */
public final class SubscriptionDetector {

    public static final int MIN_OCCURRENCES = 3;
    private static final double MAX_AMOUNT_CV = 0.15;
    private static final double MIN_INTERVAL_SCORE = 0.67;

    private SubscriptionDetector() {
    }

    public record TxnPoint(LocalDate date, double amount) {
    }

    public record SubscriptionSignal(
            boolean suspectedSubscription,
            String cadence,
            double confidence,
            double avgIntervalDays,
            double amountCv) {

        public static SubscriptionSignal none() {
            return new SubscriptionSignal(false, null, 0, 0, 0);
        }

        public Map<String, Object> toPayload() {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("suspectedSubscription", suspectedSubscription);
            if (cadence != null) {
                payload.put("cadence", cadence);
            }
            payload.put("confidence", round(confidence));
            payload.put("avgIntervalDays", round(avgIntervalDays));
            payload.put("amountCv", round(amountCv));
            return payload;
        }

        private static double round(double v) {
            return Math.round(v * 1000.0) / 1000.0;
        }
    }

    public static SubscriptionSignal detect(List<TxnPoint> points) {
        if (points == null || points.size() < MIN_OCCURRENCES) {
            return SubscriptionSignal.none();
        }
        List<TxnPoint> sorted = points.stream()
                .sorted(Comparator.comparing(TxnPoint::date))
                .toList();
        List<Integer> gaps = new ArrayList<>();
        for (int i = 1; i < sorted.size(); i++) {
            int gap = (int) ChronoUnit.DAYS.between(sorted.get(i - 1).date(), sorted.get(i).date());
            if (gap > 0) {
                gaps.add(gap);
            }
        }
        if (gaps.isEmpty()) {
            return SubscriptionSignal.none();
        }

        double medianGap = median(gaps);
        String cadence = cadenceForMedian(medianGap);
        if (cadence == null) {
            return SubscriptionSignal.none();
        }

        int inRange = 0;
        for (int gap : gaps) {
            if (gapMatchesCadence(gap, cadence)) {
                inRange++;
            }
        }
        double intervalScore = (double) inRange / gaps.size();
        if (intervalScore < MIN_INTERVAL_SCORE) {
            return SubscriptionSignal.none();
        }

        List<Double> amounts = sorted.stream().map(TxnPoint::amount).toList();
        double amountCv = coefficientOfVariation(amounts);
        if (amountCv > MAX_AMOUNT_CV) {
            return SubscriptionSignal.none();
        }

        double confidence = Math.min(0.99,
                0.45 + 0.35 * intervalScore + 0.20 * (1 - amountCv / MAX_AMOUNT_CV));
        return new SubscriptionSignal(true, cadence, confidence, medianGap, amountCv);
    }

    static String cadenceForMedian(double medianGap) {
        if (medianGap >= 25 && medianGap <= 38) {
            return "monthly";
        }
        if (medianGap >= 80 && medianGap <= 100) {
            return "quarterly";
        }
        if (medianGap >= 350 && medianGap <= 380) {
            return "yearly";
        }
        return null;
    }

    static boolean gapMatchesCadence(int gap, String cadence) {
        return switch (cadence) {
            case "monthly" -> gap >= 25 && gap <= 38;
            case "quarterly" -> gap >= 80 && gap <= 100;
            case "yearly" -> gap >= 350 && gap <= 380;
            default -> false;
        };
    }

    private static double median(List<Integer> values) {
        List<Integer> sorted = values.stream().sorted().toList();
        int mid = sorted.size() / 2;
        if (sorted.size() % 2 == 0) {
            return (sorted.get(mid - 1) + sorted.get(mid)) / 2.0;
        }
        return sorted.get(mid);
    }

    private static double coefficientOfVariation(List<Double> amounts) {
        double mean = amounts.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        if (mean <= 0) {
            return 1;
        }
        double variance = amounts.stream()
                .mapToDouble(a -> Math.pow(a - mean, 2))
                .average()
                .orElse(0);
        return Math.sqrt(variance) / mean;
    }
}
