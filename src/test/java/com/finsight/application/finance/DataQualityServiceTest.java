package com.finsight.application.finance;

import com.finsight.application.authentication.LedgerUserScope;
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
    @Mock
    private LedgerUserScope ledgerUserScope;

    private DataQualityService service;

    @BeforeEach
    void setUp() {
        when(ledgerUserScope.resolve()).thenReturn("xiaxinyu");
        service = new DataQualityService(scopedFinancialQueries, dataQualityMapper, configVersionService, ledgerUserScope);
    }

    @Test
    void reportStrip_includesConfidenceFromUnclassifiedPct() {
        when(scopedFinancialQueries.countUnclassified()).thenReturn(40);
        when(scopedFinancialQueries.countTransferGroups()).thenReturn(3);
        when(dataQualityMapper.classificationCoverage("xiaxinyu")).thenReturn(Map.of(
                "totalTxns", 400,
                "unclassifiedPct", 10.0,
                "unclassifiedAmount", 1200.0));
        when(dataQualityMapper.countOrphanCategoryTransactions("xiaxinyu")).thenReturn(2);
        when(dataQualityMapper.countRefundExcluded("xiaxinyu")).thenReturn(5);
        when(dataQualityMapper.merchantTokenCoverage("xiaxinyu")).thenReturn(Map.of("tokenCoveragePct", 88.5));
        when(configVersionService.asMap()).thenReturn(Map.of("taxonomyVersion", 2));

        Map<String, Object> strip = service.reportStrip("fin_metric_monthly");

        assertEquals("medium", strip.get("confidence"));
        assertEquals("fin_metric_monthly", strip.get("metricsSource"));
        assertEquals(10.0, strip.get("unclassifiedPct"));
    }
}
