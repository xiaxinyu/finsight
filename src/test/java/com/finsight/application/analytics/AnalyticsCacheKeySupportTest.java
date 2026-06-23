package com.finsight.application.analytics;

import com.finsight.application.classification.ConfigVersionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsCacheKeySupportTest {

    @Mock
    private ConfigVersionService configVersionService;

    @InjectMocks
    private AnalyticsCacheKeySupport support;

    @Test
    void profileKeyIncludesMetricRefreshVersion() {
        when(configVersionService.asMap()).thenReturn(Map.of("metricRefreshVersion", 42));
        String key = support.profileKey("alice");
        assertTrue(key.contains("alice"));
        assertTrue(key.contains(":m42"));
    }
}
