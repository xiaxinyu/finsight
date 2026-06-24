package com.finsight.application.analytics;

import com.finsight.domain.model.Bill;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ForecastBillSupportTest {

    @Test
    void monthlyBillAppliesEveryMonth() {
        Bill bill = bill("monthly", 15);
        assertTrue(ForecastBillSupport.appliesInMonth(bill, 3));
        assertTrue(ForecastBillSupport.appliesInMonth(bill, 11));
    }

    @Test
    void quarterlyBillAppliesOnQuarterStarts() {
        Bill bill = bill("quarterly", 1);
        assertTrue(ForecastBillSupport.appliesInMonth(bill, 1));
        assertTrue(ForecastBillSupport.appliesInMonth(bill, 4));
        assertFalse(ForecastBillSupport.appliesInMonth(bill, 2));
    }

    @Test
    void oneTimeBillNeverApplies() {
        Bill bill = bill("once", 10);
        assertFalse(ForecastBillSupport.appliesInMonth(bill, 6));
    }

    private static Bill bill(String recurrence, int dueDay) {
        Bill bill = new Bill();
        bill.setAmount(BigDecimal.valueOf(100));
        bill.setRecurrence(recurrence);
        bill.setDueDay(dueDay);
        return bill;
    }
}
