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
    void transportUnderLiving_isTransport() {
        assertEquals("transport_spending",
                CategorySpendingDomain.inferDomainTag("LIVING-05", "LIVING", "公共交通 (公交、地铁)").orElseThrow());
    }
}
