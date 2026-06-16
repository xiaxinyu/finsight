package com.finsight.application.analytics;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SubscriptionDetectorTest {

    @Test
    void detect_monthlyStableSubscription() {
        List<SubscriptionDetector.TxnPoint> points = List.of(
                new SubscriptionDetector.TxnPoint(LocalDate.of(2026, 1, 5), 15.99),
                new SubscriptionDetector.TxnPoint(LocalDate.of(2026, 2, 4), 15.99),
                new SubscriptionDetector.TxnPoint(LocalDate.of(2026, 3, 5), 16.00),
                new SubscriptionDetector.TxnPoint(LocalDate.of(2026, 4, 4), 15.99));

        SubscriptionDetector.SubscriptionSignal signal = SubscriptionDetector.detect(points);

        assertTrue(signal.suspectedSubscription());
        assertEquals("monthly", signal.cadence());
        assertTrue(signal.confidence() >= 0.7);
    }

    @Test
    void detect_quarterlySubscription() {
        List<SubscriptionDetector.TxnPoint> points = List.of(
                new SubscriptionDetector.TxnPoint(LocalDate.of(2025, 1, 10), 120),
                new SubscriptionDetector.TxnPoint(LocalDate.of(2025, 4, 12), 120),
                new SubscriptionDetector.TxnPoint(LocalDate.of(2025, 7, 11), 118),
                new SubscriptionDetector.TxnPoint(LocalDate.of(2025, 10, 10), 120));

        SubscriptionDetector.SubscriptionSignal signal = SubscriptionDetector.detect(points);

        assertTrue(signal.suspectedSubscription());
        assertEquals("quarterly", signal.cadence());
    }

    @Test
    void detect_rejectsIrregularAmounts() {
        List<SubscriptionDetector.TxnPoint> points = List.of(
                new SubscriptionDetector.TxnPoint(LocalDate.of(2026, 1, 5), 10),
                new SubscriptionDetector.TxnPoint(LocalDate.of(2026, 2, 5), 80),
                new SubscriptionDetector.TxnPoint(LocalDate.of(2026, 3, 5), 15));

        assertFalse(SubscriptionDetector.detect(points).suspectedSubscription());
    }

    @Test
    void detect_rejectsTooFewTransactions() {
        List<SubscriptionDetector.TxnPoint> points = List.of(
                new SubscriptionDetector.TxnPoint(LocalDate.of(2026, 1, 5), 15),
                new SubscriptionDetector.TxnPoint(LocalDate.of(2026, 2, 5), 15));

        assertFalse(SubscriptionDetector.detect(points).suspectedSubscription());
    }
}
