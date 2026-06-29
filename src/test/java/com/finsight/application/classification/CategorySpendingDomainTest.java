package com.finsight.application.classification;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CategorySpendingDomainTest {

    @Test
    void supermarketName_isShopping() {
        assertEquals("shopping_spending",
                CategorySpendingDomain.inferDomainTag("LIVING-03", "LIVING", "超市购物 (食材、粮油、日用品)").orElseThrow());
    }

    @Test
    void diningName_isDining() {
        assertTrue(CategorySpendingDomain.matchesDining("LIVING-01", "LIVING", "餐饮 (含外卖、早餐、咖啡)"));
    }

    @Test
    void medicalName_isMedical() {
        assertEquals("medical_spending",
                CategorySpendingDomain.inferDomainTag("LIVING-10", "LIVING", "基础医疗").orElseThrow());
    }

    @Test
    void dailyMedicalCode_isMedical() {
        assertEquals("medical_spending",
                CategorySpendingDomain.inferDomainTag("DAILY-05", "LIVING", "医疗药品").orElseThrow());
    }

    @Test
    void petNameWithMedicalSubstring_isGeneralNotMedical() {
        assertEquals("daily_spending",
                CategorySpendingDomain.inferDomainTag("LIVING-08", "LIVING", "宠物支出（食品、医疗）").orElseThrow());
    }

    @Test
    void dailyPetCode_isGeneral() {
        assertEquals("daily_spending",
                CategorySpendingDomain.inferDomainTag("LIVING-11", "LIVING", "宠物").orElseThrow());
    }
}
