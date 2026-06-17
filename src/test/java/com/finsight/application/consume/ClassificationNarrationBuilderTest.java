package com.finsight.application.consume;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClassificationNarrationBuilderTest {

    @Test
    void stripsTenpayPrefixFromCcbCreditDescription() {
        assertEquals(
                "深圳市地铁相关运营主体",
                ClassificationNarrationBuilder.merchantCore("财付通-深圳市地铁相关运营主体"));
    }

    @Test
    void stripsMultiplePaymentChannels() {
        assertEquals(
                "深圳市地铁相关运营主体",
                ClassificationNarrationBuilder.merchantCore("财付通-微信支付-深圳市地铁相关运营主体"));
    }

    @Test
    void keepsMerchantWhenNoChannelPrefix() {
        assertEquals("东乐饮食有限公司", ClassificationNarrationBuilder.merchantCore("东乐饮食有限公司"));
    }
}
