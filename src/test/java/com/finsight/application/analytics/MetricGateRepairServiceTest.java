package com.finsight.application.analytics;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class MetricGateRepairServiceTest {

    @Mock
    private MetricMonthlyService metricMonthlyService;

    @InjectMocks
    private MetricGateRepairService service;

    @Test
    void scheduleRepairWhenGateBlocked() {
        service.scheduleRepairIfGateBlocked(true);
        verify(metricMonthlyService, times(3)).refreshAsync(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void skipRepairWhenGateOk() {
        service.scheduleRepairIfGateBlocked(false);
        verifyNoInteractions(metricMonthlyService);
    }
}
