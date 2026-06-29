package com.finsight.application.classification;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CategorySemanticDefaultsTest {

    @Test
    void catalogLivingDining_isDiscretionary() {
        assertEquals("daily_spending", CategorySemanticDefaults.inferFromCatalog(
                ClassificationL2TargetCatalog.DAILY_DINE_IN));
    }

    @Test
    void catalogFixedRent_isFixed() {
        assertEquals("fixed_spending", CategorySemanticDefaults.inferFromCatalog(
                ClassificationL2TargetCatalog.FIXED_RENT));
    }

    @Test
    void catalogFixedSubscription_isSubscription() {
        assertEquals("subscription_spending", CategorySemanticDefaults.inferFromCatalog(
                ClassificationL2TargetCatalog.FIXED_SUBSCRIPTION));
    }

    @Test
    void catalogGiftRedpack_isSocial() {
        assertEquals("social_spending", CategorySemanticDefaults.inferFromCatalog(
                ClassificationL2TargetCatalog.GIFT_REDPACK));
    }

    @Test
    void catalogSalary_isEarned() {
        assertEquals("real_income", CategorySemanticDefaults.inferFromCatalog(
                ClassificationL2TargetCatalog.LEGACY_INC_SALARY));
    }

    @Test
    void customLivingChild_infersDiscretionary() {
        assertEquals("daily_spending", CategorySemanticDefaults.inferSemanticTag(
                "LIVING-99",
                "LIVING",
                "餐饮 (含外卖、早餐、咖啡)",
                "expense",
                "budget",
                2));
    }

    @Test
    void fillMissing_preservesUserSemanticTag() {
        CategorySemanticDefaults.ResolvedDefaults d = CategorySemanticDefaults.fillMissing(
                new CategorySemanticDefaults.CategoryInput(
                        "LIVING-01", "餐饮", 2, "LIVING", "expense", "budget", "social_spending"));
        assertEquals("social_spending", d.semanticTag());
        assertEquals("budget", d.reportRole());
    }

    @Test
    void fillMissing_fillsBlankSemanticTag() {
        CategorySemanticDefaults.ResolvedDefaults d = CategorySemanticDefaults.fillMissing(
                new CategorySemanticDefaults.CategoryInput(
                        "TRANS-02", "打车/网约车", 2, "TRANSPORT", "expense", "budget", null));
        assertEquals("daily_spending", d.semanticTag());
    }

    @Test
    void everyCatalogEntry_hasSemanticDefault() {
        for (ClassificationL2TargetCatalog target : ClassificationL2TargetCatalog.values()) {
            String tag = CategorySemanticDefaults.inferFromCatalog(target);
            org.junit.jupiter.api.Assertions.assertFalse(tag.isBlank(),
                    () -> "Missing semantic default for " + target.code());
        }
    }
}
