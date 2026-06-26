package com.finsight.application.classification;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
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

    @Test
    void dailyBudgetCategory_isVariableNotFixed() {
        var p = CategoryFinanceSemantics.profile("budget", "expense", "LIVING", "DAILY-01");
        assertEquals("variable", p.budgetBehavior());
        assertNull(p.fixedCostKind());
        assertTrue(p.includeInExpenseTrend());
        assertTrue(p.includeInBudget());
    }

    @Test
    void fixedCategory_isFixedCost() {
        var p = CategoryFinanceSemantics.profile("budget", "expense", "FIXED", "FIXED-01");
        assertEquals("fixed", p.budgetBehavior());
        assertEquals("rent", p.fixedCostKind());
    }

    @Test
    void fixedInsuranceUsesCashflowRole() {
        var p = CategoryFinanceSemantics.profile("cashflow", "expense", "FIXED", "FIXED-04");
        assertEquals("fixed", p.budgetBehavior());
        assertEquals("insurance", p.fixedCostKind());
    }
}
