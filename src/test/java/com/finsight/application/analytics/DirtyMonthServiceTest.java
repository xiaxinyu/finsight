package com.finsight.application.analytics;

import com.finsight.application.classification.ConfigVersionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DirtyMonthServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;
    @Mock
    private MetricMonthlyService metricMonthlyService;
    @Mock
    private MerchantMiningService merchantMiningService;
    @Mock
    private ConfigVersionBump configVersionBump;

    private DirtyMonthService service;

    @BeforeEach
    void setUp() {
        service = new DirtyMonthService(jdbcTemplate, metricMonthlyService, merchantMiningService, configVersionBump);
    }

    @Test
    void markDirty_insertsMonthKey() {
        service.markDirty(List.of("2026-03"));
        verify(jdbcTemplate).update(
                org.mockito.ArgumentMatchers.contains("insert into fin_dirty_month"),
                eq("2026-03"));
    }
}
