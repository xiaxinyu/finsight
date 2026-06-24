package com.finsight.application.analytics;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Contract tests for merchant token normalization shared by Java services and MySQL
 * {@code finsight_normalize_merchant_token()}. When changing {@link MerchantNormalizer},
 * update {@code docs/tech/database/merchant-token-normalization.sql} in the same PR.
 */
class MerchantTokenContractTest {

    @ParameterizedTest(name = "raw=\"{0}\" -> \"{1}\"")
    @CsvSource(delimiter = '|', textBlock = """
            STARBUCKS COFFEE Order No: 883920184 Alipay | starbucks coffee
            Netflix.com 883920184                         | netflix
            Netflix Monthly                               | netflix
            AMZN MKTP US 883920                           | amzn mktp us
            Uber Trip 442910                              | uber
            财付通 - 北京百度网讯科技有限公司                      | 北京百度网讯科技有限公司
            支付宝 - 支付宝 - 消费 - Stripe Inc               | stripe inc
            (消费) 财付通 - 北京百度网讯科技有限公司                 | 北京百度网讯科技有限公司
            """)
    void normalizeToken_matchesReportAndDrillContract(String raw, String expectedToken) {
        assertEquals(expectedToken, MerchantNormalizer.normalizeToken(raw));
    }

    @Test
    void merchantCoreRaw_extractsTrailingMerchantSegment() {
        assertEquals("Stripe Inc", MerchantNormalizer.merchantCoreRaw("支付宝 - 支付宝 - 消费 - Stripe Inc"));
        assertEquals("北京百度网讯科技有限公司",
                MerchantNormalizer.merchantCoreRaw("财付通 - 北京百度网讯科技有限公司"));
    }

    @Test
    void rawMerchant_prefersOpponentNameOverDescription() {
        assertEquals(
                "Amazon",
                MerchantNormalizer.rawMerchant("Amazon", "AMZN MKTP US"));
    }

    @Test
    void endToEndToken_fromTransactionFields() {
        String raw = MerchantNormalizer.rawMerchant("Netflix.com", "Monthly subscription");
        assertEquals("netflix", MerchantNormalizer.normalizeToken(raw));
    }

    @Test
    void sqlExpressions_referenceNormalizeFunction() {
        assertEquals(
                "finsight_normalize_merchant_token(" + MerchantTokenSql.MERCHANT_RAW_T + ")",
                MerchantTokenSql.normalizedTokenT());
    }
}
