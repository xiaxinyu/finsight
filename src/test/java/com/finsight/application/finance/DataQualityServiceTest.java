package com.finsight.application.finance;

import com.finsight.application.classification.ConfigVersionService;
import com.finsight.infrastructure.mapper.DataQualityMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataQualityServiceTest {

    @Mock
    private UserScopedFinancialQueries scopedFinancialQueries;
    @Mock
    private DataQualityMapper dataQualityMapper;
    @Mock
    private ConfigVersionService configVersionService;

    private DataQualityService service;

    @BeforeEach
    void setUp() {
        service = new DataQualityService(scopedFinancialQueries, dataQualityMapper, configVersionService);
    }

    @Test
    void reportStrip_includesConfidenceFromUnclassifiedPct() {
        when(scopedFinancialQueries.countUnclassified()).thenReturn(40);
        when(scopedFinancialQueries.countTransferGroups()).thenReturn(3);
        when(dataQualityMapper.classificationCoverage()).thenReturn(Map.of(
                "totalTxns", 400,
                "unclassifiedPct", 10.0,
                "unclassifiedAmount", 1200.0));
        when(dataQualityMapper.countOrphanCategoryTransactions()).thenReturn(2);
        when(dataQualityMapper.countRefundExcluded()).thenReturn(5);
        when(dataQualityMapper.merchantTokenCoverage()).thenReturn(Map.of("tokenCoveragePct", 88.5));
        when(configVersionService.asMap()).thenReturn(Map.of("taxonomyVersion", 2));

        Map<String, Object> strip = service.reportStrip("fin_metric_monthly");

        assertEquals("medium", strip.get("confidence"));
        assertEquals("fin_metric_monthly", strip.get("metricsSource"));
        assertEquals(10.0, strip.get("unclassifiedPct"));
    }
}
