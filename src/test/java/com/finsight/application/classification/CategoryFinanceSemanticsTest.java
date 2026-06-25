package com.finsight.application.classification;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CategoryFinanceSemanticsTest {

    @Test
    void salaryCategory_includedInIncomeTrend() {
        var p = CategoryFinanceSemantics.profile("income", "income");
        assertEquals("income", p.economicNature());
        assertTrue(p.includeInIncomeTrend());
        assertFalse(p.includeInExpenseTrend());
    }

    @Test
    void reimbursement_notIncomeTrend() {
        var p = CategoryFinanceSemantics.profile("refund", "income,refund");
        assertEquals("refund", p.economicNature());
        assertFalse(p.includeInIncomeTrend());
    }

    @Test
    void investment_notConsumptionBudget() {
        var p = CategoryFinanceSemantics.profile("investment", "expense,invest");
        assertEquals("investment", p.economicNature());
        assertFalse(p.includeInExpenseTrend());
        assertFalse(p.includeInBudget());
    }
}
