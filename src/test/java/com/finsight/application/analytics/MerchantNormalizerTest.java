package com.finsight.application.analytics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MerchantNormalizerTest {

    @Test
    void normalizeToken_stripsOrderAndPaymentNoise() {
        assertEquals(
                "starbucks coffee",
                MerchantNormalizer.normalizeToken("STARBUCKS COFFEE Order No: 883920184 Alipay"));
        assertEquals(
                "netflix",
                MerchantNormalizer.normalizeToken("Netflix.com 883920184"));
    }

    @Test
    void normalizeToken_mergesVariantsToSameToken() {
        String a = MerchantNormalizer.normalizeToken("Netflix.com 883920");
        String b = MerchantNormalizer.normalizeToken("Netflix Monthly");
        assertEquals("netflix", a);
        assertEquals("netflix", b);
    }

    @Test
    void rawMerchant_prefersOpponentName() {
        assertEquals("Amazon", MerchantNormalizer.rawMerchant("Amazon", "AMZN MKTP US"));
    }

    @Test
    void displayName_usesPreferredRawWhenShort() {
        assertEquals("Netflix", MerchantNormalizer.displayName("netflix", "Netflix"));
    }
}
