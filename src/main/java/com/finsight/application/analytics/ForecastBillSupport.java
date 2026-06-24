package com.finsight.application.analytics;

import com.finsight.domain.model.Bill;

/**
 * Determines whether a planning bill contributes to a forecast month.
 */
public final class ForecastBillSupport {

    private ForecastBillSupport() {
    }

    public static boolean appliesInMonth(Bill bill, int month) {
        if (bill == null || bill.getAmount() == null) {
            return false;
        }
        String recurrence = bill.getRecurrence();
        if (recurrence == null || recurrence.isBlank() || "monthly".equalsIgnoreCase(recurrence)) {
            return true;
        }
        if ("quarterly".equalsIgnoreCase(recurrence)) {
            return month == 1 || month == 4 || month == 7 || month == 10;
        }
        if ("annual".equalsIgnoreCase(recurrence) || "yearly".equalsIgnoreCase(recurrence)) {
            Integer dueDay = bill.getDueDay();
            return dueDay == null || month == Math.min(12, Math.max(1, dueDay / 28 + 1));
        }
        if ("once".equalsIgnoreCase(recurrence) || "one-time".equalsIgnoreCase(recurrence)) {
            return false;
        }
        return true;
    }
}
