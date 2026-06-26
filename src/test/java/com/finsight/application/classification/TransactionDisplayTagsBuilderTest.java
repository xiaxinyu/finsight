package com.finsight.application.classification;

import com.finsight.domain.model.Transaction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransactionDisplayTagsBuilderTest {

    @Test
    void fixedCategory_getsFixedCostTags() {
        Transaction row = baseExpense();
        row.setConsumeCode("FIXED-01");
        row.setConsumeName("Rent");
        row.setCategoryParentId("FIXED");
        row.setBudgetBehavior("fixed");
        row.setEconomicNature("expense");
        row.setQualityState("classified");

        var tags = TransactionDisplayTagsBuilder.build(row);
        assertTrue(tags.stream().anyMatch(t -> "fixed_cost".equals(t.getId())));
        assertTrue(tags.stream().anyMatch(t -> "fixed_cost_rent".equals(t.getId())));
    }

    @Test
    void giftCategory_getsSocialTag() {
        Transaction row = baseExpense();
        row.setConsumeCode("GIFT-01");
        row.setConsumeName("Red envelope");
        row.setCategoryParentId("GIFT");
        row.setBudgetBehavior("variable");
        row.setEconomicNature("expense");
        row.setQualityState("classified");

        var tags = TransactionDisplayTagsBuilder.build(row);
        assertTrue(tags.stream().anyMatch(t -> "social".equals(t.getId())));
    }

    @Test
    void dailySpending_noFixedCostTag() {
        Transaction row = baseExpense();
        row.setConsumeCode("DAILY-01");
        row.setConsumeName("Dining");
        row.setCategoryParentId("LIVING");
        row.setBudgetBehavior("variable");
        row.setEconomicNature("expense");
        row.setQualityState("classified");

        var tags = TransactionDisplayTagsBuilder.build(row);
        assertFalse(tags.stream().anyMatch(t -> "fixed_cost".equals(t.getId())));
    }

    @Test
    void unclassified_getsUnclassifiedTag() {
        Transaction row = baseExpense();
        row.setQualityState("unclassified");

        var tags = TransactionDisplayTagsBuilder.build(row);
        assertTrue(tags.stream().anyMatch(t -> "unclassified".equals(t.getId())));
    }

    private static Transaction baseExpense() {
        Transaction row = new Transaction();
        row.setBalanceMoney(100.0);
        row.setTxnKind("expense");
        return row;
    }
}
