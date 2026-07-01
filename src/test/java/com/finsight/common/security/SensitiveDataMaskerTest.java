package com.finsight.common.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SensitiveDataMaskerTest {

    @Test
    void masksCardNumberToLastFour() {
        assertEquals("****1234", SensitiveDataMasker.maskCardNumber("6222021234561234"));
    }

    @Test
    void masksSecretWithoutValue() {
        assertEquals("(configured, length=18)", SensitiveDataMasker.maskSecret("my-secret-sign-key"));
    }
}
