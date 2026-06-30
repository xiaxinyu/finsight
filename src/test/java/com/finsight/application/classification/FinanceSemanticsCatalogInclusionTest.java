package com.finsight.application.classification;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FinanceSemanticsCatalogInclusionTest {

    @Test
    void txnTypeLabelsAlignWithAdminKinds() {
        assertEquals("Income", FinanceSemanticsCatalog.semanticTagTxnTypeLabel("real_income"));
        assertEquals("Income", FinanceSemanticsCatalog.semanticTagTxnTypeLabel("investment_income"));
        assertEquals("Expense", FinanceSemanticsCatalog.semanticTagTxnTypeLabel("dining_spending"));
        assertEquals("Tax", FinanceSemanticsCatalog.semanticTagTxnTypeLabel("tax_expense"));
        assertEquals("Tax", FinanceSemanticsCatalog.semanticTagTxnTypeLabel("tax_refund"));
        assertEquals("Refund", FinanceSemanticsCatalog.semanticTagTxnTypeLabel("refund_reimbursement"));
        assertEquals("Investment", FinanceSemanticsCatalog.semanticTagTxnTypeLabel("investment"));
        assertEquals("Transfer", FinanceSemanticsCatalog.semanticTagTxnTypeLabel("transfer"));
        assertEquals("Finance", FinanceSemanticsCatalog.semanticTagTxnTypeLabel("finance_loan"));
    }

    @Test
    void inclusionFlagsFollowReportingClassification() {
        assertTrue(FinanceSemanticsCatalog.semanticTagIncludeInIncomeTrend("real_income"));
        assertTrue(FinanceSemanticsCatalog.semanticTagIncludeInExpenseTrend("fixed_housing"));
        assertTrue(FinanceSemanticsCatalog.semanticTagIncludeInExpenseTrend("tax_expense"));
        assertFalse(FinanceSemanticsCatalog.semanticTagIncludeInIncomeTrend("refund_reimbursement"));
        assertFalse(FinanceSemanticsCatalog.semanticTagIncludeInExpenseTrend("investment"));
        assertTrue(FinanceSemanticsCatalog.semanticTagIsNonPnl("finance_loan"));
    }

    @Test
    void classificationPathUsesEnglishL1L2() {
        assertEquals("Expense / Dining", FinanceSemanticsCatalog.semanticTagClassification("dining_spending"));
        assertEquals("Finance / Investment", FinanceSemanticsCatalog.semanticTagClassification("investment"));
    }
}
