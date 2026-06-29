package com.finsight.application.classification;

import com.finsight.domain.model.Transaction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
        assertTrue(tags.stream().anyMatch(t -> "semantic_fixed_spending".equals(t.getId())));
        assertTrue(tags.stream().anyMatch(t -> "fixed_cost_rent".equals(t.getId())));
        assertFalse(tags.stream().anyMatch(t -> "fixed_cost".equals(t.getId())));
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
        assertTrue(tags.stream().anyMatch(t -> "semantic_social_spending".equals(t.getId())));
        assertFalse(tags.stream().anyMatch(t -> "social".equals(t.getId())));
    }

    @Test
    void diningCategory_getsDiningTag() {
        Transaction row = baseExpense();
        row.setConsumeCode("DAILY-01");
        row.setConsumeName("Dining");
        row.setCategoryParentId("LIVING");
        row.setCategoryL1Name("日常生活");
        row.setBudgetBehavior("variable");
        row.setEconomicNature("expense");
        row.setQualityState("classified");

        var tags = TransactionDisplayTagsBuilder.build(row);
        assertTrue(tags.stream().anyMatch(t -> "semantic_dining_spending".equals(t.getId())));
        assertTrue(tags.stream().anyMatch(t -> "Dining".equals(t.getLabel())));
        assertTrue(tags.stream().anyMatch(t -> "category_l1".equals(t.getId()) && "日常生活".equals(t.getLabel())));
    }

    @Test
    void supermarket_getsShoppingTag() {
        Transaction row = baseExpense();
        row.setConsumeCode("LIVING-03");
        row.setConsumeName("超市购物 (食材、粮油、日用品)");
        row.setCategoryParentId("LIVING");
        row.setBudgetBehavior("variable");
        row.setEconomicNature("expense");
        row.setQualityState("classified");

        var tags = TransactionDisplayTagsBuilder.build(row);
        assertTrue(tags.stream().anyMatch(t -> "semantic_shopping_spending".equals(t.getId())));
    }

    @Test
    void storedSemanticTag_overridesInference() {
        Transaction row = baseExpense();
        row.setConsumeCode("LIVING-01");
        row.setConsumeName("Dining");
        row.setCategorySemanticTag("social_spending");
        row.setBudgetBehavior("variable");
        row.setEconomicNature("expense");
        row.setQualityState("classified");

        var tags = TransactionDisplayTagsBuilder.build(row);
        assertTrue(tags.stream().anyMatch(t -> "semantic_social_spending".equals(t.getId())));
    }

    @Test
    void unclassified_getsUnclassifiedTag() {
        Transaction row = baseExpense();
        row.setQualityState("unclassified");

        var tags = TransactionDisplayTagsBuilder.build(row);
        assertTrue(tags.stream().anyMatch(t -> "unclassified".equals(t.getId())));
    }

    @Test
    void resolver_prefersStoredSemanticTag() {
        Transaction row = baseExpense();
        row.setCategorySemanticTag("real_income");
        row.setEconomicNature("expense");
        assertEquals("real_income", TransactionSemanticTagResolver.resolve(row));
    }

    private static Transaction baseExpense() {
        Transaction row = new Transaction();
        row.setBalanceMoney(100.0);
        row.setTxnKind("expense");
        return row;
    }
}
