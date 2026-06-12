package com.finsight.application.analytics;

import com.finsight.application.authentication.AuthenticationFacade;
import com.finsight.application.config.FinsightFeatureProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MetricGateServiceTest {

    @Mock
    private FinsightFeatureProperties features;

    @Mock
    private MetricReconciliationService reconciliationService;

    @Mock
    private AuthenticationFacade authenticationFacade;

    @InjectMocks
    private MetricGateService service;

    @Test
    void useReportFallback_falseWhenGateDisabled() throws Exception {
        FinsightFeatureProperties.Metrics metrics = new FinsightFeatureProperties.Metrics();
        metrics.setReconcileGate(false);
        when(features.getMetrics()).thenReturn(metrics);
        assertFalse(service.useReportFallback());
    }

    @Test
    void useReportFallback_trueWhenMismatch() throws Exception {
        FinsightFeatureProperties.Metrics metrics = new FinsightFeatureProperties.Metrics();
        metrics.setReconcileGate(true);
        when(features.getMetrics()).thenReturn(metrics);
        when(authenticationFacade.getUserName()).thenReturn("u1");
        when(reconciliationService.reconcile(anyString(), anyString()))
                .thenReturn(Map.of("ok", false, "mismatches", List.of("INCOME_TOTAL report=1 metric=2")));

        assertTrue(service.useReportFallback());
    }
}
