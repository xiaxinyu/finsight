package com.finsight.application.consume;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ClassificationTextNormalizerTest {

    @Test
    void expand_extractsMailOrderInstallmentKeyword() {
        String expanded = ClassificationTextNormalizer.expand("(分期) 邮购分期23104000096/21/24期");
        assertTrue(expanded.contains("邮购分期"));
    }

    @Test
    void expand_extractsAnnualFeeWaiver() {
        String expanded = ClassificationTextNormalizer.expand("刷卡次数免年费 RMB: 100.00");
        assertTrue(expanded.contains("免年费"));
    }
}
