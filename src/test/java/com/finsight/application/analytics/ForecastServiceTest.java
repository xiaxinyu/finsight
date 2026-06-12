package com.finsight.application.analytics;

import com.finsight.application.authentication.AuthenticationFacade;
import com.finsight.application.finance.BillService;
import com.finsight.common.exception.AppServiceException;
import com.finsight.domain.model.MetricCode;
import com.finsight.domain.port.MetricMonthlyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ForecastServiceTest {

    @Mock
    private MetricMonthlyRepository metricRepository;

    @Mock
    private BillService billService;

    @Mock
    private AuthenticationFacade authenticationFacade;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private MetricGateService metricGateService;

    @Mock
    private MetricMonthlyService metricMonthlyService;

    @InjectMocks
    private ForecastService service;

    @Test
    void forecast_persistsThirtySixLinesPerRun() throws Exception {
        when(authenticationFacade.getUserName()).thenReturn("user1");
        when(metricGateService.status(3)).thenReturn(Map.of("ok", true));
        when(metricGateService.useReportFallback()).thenReturn(false);
        when(metricRepository.listForUser(anyString(), anyString(), anyString())).thenReturn(sampleHistory());
        when(billService.listEnabled()).thenReturn(List.of());
        when(jdbcTemplate.queryForObject(contains("information_schema"), eq(Integer.class), eq("fin_forecast_line")))
                .thenReturn(1);

        Map<String, Object> out = service.forecast(2026, "base");

        assertNotNull(out.get("runId"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> months = (List<Map<String, Object>>) out.get("months");
        assertEquals(12, months.size());

        verify(jdbcTemplate).update(
                contains("insert into fin_forecast_run"),
                any(), eq("user1"), eq("base"), eq(2026), anyString());
        verify(jdbcTemplate, times(36)).update(
                argThat((String sql) -> sql.contains("insert into fin_forecast_line")),
                any(), any(), any(), any(), any(), any(), any());

        ArgumentCaptor<String> metricCodes = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate, times(36)).update(
                anyString(),
                any(),
                any(),
                any(),
                metricCodes.capture(),
                any(),
                any(),
                any());
        List<String> codes = metricCodes.getAllValues();
        assertEquals(12, codes.stream().filter(ForecastService.METRIC_INCOME_FORECAST::equals).count());
        assertEquals(12, codes.stream().filter(ForecastService.METRIC_EXPENSE_FORECAST::equals).count());
        assertEquals(12, codes.stream().filter(ForecastService.METRIC_NET_FORECAST::equals).count());
    }

    @Test
    void forecastLines_returnsRowsForOwnedRun() throws Exception {
        when(authenticationFacade.getUserName()).thenReturn("user1");
        when(jdbcTemplate.queryForObject(contains("information_schema"), eq(Integer.class), eq("fin_forecast_line")))
                .thenReturn(1);
        when(jdbcTemplate.queryForObject(contains("fin_forecast_run"), eq(Integer.class), eq("run-1"), eq("user1")))
                .thenReturn(1);
        List<Map<String, Object>> lines = List.of(lineRow("2026-01", ForecastService.METRIC_NET_FORECAST, "100.0000"));
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq("run-1"))).thenReturn(lines);

        List<Map<String, Object>> out = service.forecastLines("run-1");

        assertEquals(1, out.size());
        assertEquals("2026-01", out.get(0).get("monthKey"));
        assertEquals(ForecastService.METRIC_NET_FORECAST, out.get(0).get("metricCode"));
    }

    @Test
    void forecastLines_rejectsUnknownRun() {
        when(authenticationFacade.getUserName()).thenReturn("user1");
        when(jdbcTemplate.queryForObject(contains("information_schema"), eq(Integer.class), eq("fin_forecast_line")))
                .thenReturn(1);
        when(jdbcTemplate.queryForObject(contains("fin_forecast_run"), eq(Integer.class), eq("missing"), eq("user1")))
                .thenReturn(0);

        assertThrows(AppServiceException.class, () -> service.forecastLines("missing"));
    }

    private static List<Map<String, Object>> sampleHistory() {
        return List.of(
                metricRow("2025-06", MetricCode.INCOME_TOTAL.name(), 8000),
                metricRow("2025-07", MetricCode.INCOME_TOTAL.name(), 8200),
                metricRow("2025-06", MetricCode.EXPENSE_TOTAL.name(), 5000),
                metricRow("2025-07", MetricCode.EXPENSE_TOTAL.name(), 5100)
        );
    }

    private static Map<String, Object> metricRow(String month, String code, double value) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("monthKey", month);
        row.put("metricCode", code);
        row.put("metricValue", value);
        return row;
    }

    private static Map<String, Object> lineRow(String monthKey, String metricCode, String value) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("monthKey", monthKey);
        row.put("metricCode", metricCode);
        row.put("metricValue", new BigDecimal(value));
        row.put("lowerBound", new BigDecimal("90.0000"));
        row.put("upperBound", new BigDecimal("110.0000"));
        return row;
    }
}
