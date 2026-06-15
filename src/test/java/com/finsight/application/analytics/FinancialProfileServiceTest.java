package com.finsight.application.analytics;

import com.finsight.application.authentication.AuthenticationFacade;
import com.finsight.application.finance.CashflowService;
import com.finsight.application.finance.DataQualityService;
import com.finsight.application.finance.WealthService;
import com.finsight.domain.port.MetricMonthlyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FinancialProfileServiceTest {

    @Mock
    private MetricMonthlyRepository metricRepository;
    @Mock
    private WealthService wealthService;
    @Mock
    private CashflowService cashflowService;
    @Mock
    private DataQualityService dataQualityService;
    @Mock
    private AuthenticationFacade authenticationFacade;
    @Mock
    private JdbcTemplate jdbcTemplate;
    @Mock
    private MetricGateService metricGateService;
    @Mock
    private MetricMonthlyService metricMonthlyService;

    @InjectMocks
    private FinancialProfileService service;

    @Test
    void currentProfile_includesReadableEvidenceAndActionPaths() throws Exception {
        when(authenticationFacade.getUserName()).thenReturn("alice");
        when(metricGateService.status(3)).thenReturn(Map.of("ok", true, "gateEnabled", false));
        when(metricGateService.useReportFallback()).thenReturn(false);
        when(metricRepository.listForUser(anyString(), anyString(), anyString())).thenReturn(sampleMetrics());
        when(wealthService.snapshot()).thenReturn(sampleWealth());
        when(cashflowService.metrics()).thenReturn(Map.of("runwayMonths", 4.5));
        when(dataQualityService.summary()).thenReturn(Map.of("unclassifiedCount", 12, "totalCount", 400));

        Map<String, Object> profile = service.currentProfile();

        assertEquals(10, ((List<?>) profile.get("dimensions")).size());
        @SuppressWarnings("unchecked")
        Map<String, Object> dataTrust = ((List<Map<String, Object>>) profile.get("dimensions")).stream()
                .filter(d -> "data_trust".equals(d.get("id")))
                .findFirst()
                .orElseThrow();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> evidence = (List<Map<String, Object>>) dataTrust.get("evidence");
        assertEquals("Unclassified transactions", evidence.get(0).get("label"));
        assertTrue(String.valueOf(evidence.get(0).get("value")).contains("12 unclassified"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> actions = (List<Map<String, Object>>) dataTrust.get("actions");
        assertEquals("/transactions?unclassified=1",
                ((Map<?, ?>) actions.get(0).get("payload")).get("path"));
        assertEquals("/admin/rules", ((Map<?, ?>) actions.get(1).get("payload")).get("path"));
    }

    @Test
    void history_filtersByDimensionWhenProvided() {
        when(authenticationFacade.getUserName()).thenReturn("alice");
        when(jdbcTemplate.queryForList(anyString(), eq("alice"), eq("2026-01-01"), eq("2026-06-01"), eq("income_stability")))
                .thenReturn(List.of(Map.of("dimension", "income_stability", "score", 70)));

        List<Map<String, Object>> rows = service.history("2026-01-01", "2026-06-01", "income_stability");

        assertEquals(1, rows.size());
        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).queryForList(sql.capture(), eq("alice"), eq("2026-01-01"), eq("2026-06-01"),
                eq("income_stability"));
        assertTrue(sql.getValue().contains("dimension = ?"));
    }

    @Test
    void currentProfile_spendingControlEvidenceShowsExpenseRatio() throws Exception {
        when(authenticationFacade.getUserName()).thenReturn("alice");
        when(metricGateService.status(3)).thenReturn(Map.of("ok", true, "gateEnabled", false));
        when(metricGateService.useReportFallback()).thenReturn(false);
        when(metricRepository.listForUser(anyString(), anyString(), anyString())).thenReturn(sampleMetrics());
        when(wealthService.snapshot()).thenReturn(sampleWealth());
        when(cashflowService.metrics()).thenReturn(Map.of("runwayMonths", 2));
        when(dataQualityService.summary()).thenReturn(Map.of("unclassifiedCount", 0, "totalCount", 10));

        Map<String, Object> profile = service.currentProfile();
        @SuppressWarnings("unchecked")
        Map<String, Object> spending = ((List<Map<String, Object>>) profile.get("dimensions")).stream()
                .filter(d -> "spending_control".equals(d.get("id")))
                .findFirst()
                .orElseThrow();
        @SuppressWarnings("unchecked")
        Map<String, Object> ev = ((List<Map<String, Object>>) spending.get("evidence")).get(0);
        assertNotNull(ev.get("label"));
        assertNotNull(ev.get("detail"));
        assertFalse(String.valueOf(ev.get("value")).isBlank());
    }

    private static List<Map<String, Object>> sampleMetrics() {
        return List.of(
                metricRow("2025-07", "INCOME_TOTAL", 10000),
                metricRow("2025-08", "INCOME_TOTAL", 11000),
                metricRow("2025-09", "INCOME_TOTAL", 9000),
                metricRow("2025-07", "EXPENSE_TOTAL", 7000),
                metricRow("2025-08", "EXPENSE_TOTAL", 7200),
                metricRow("2025-09", "EXPENSE_TOTAL", 8000),
                metricRow("2025-07", "NET_CASHFLOW", 3000),
                metricRow("2025-08", "NET_CASHFLOW", 3800),
                metricRow("2025-09", "NET_CASHFLOW", 1000)
        );
    }

    private static Map<String, Object> metricRow(String month, String code, double value) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("yearMonth", month);
        row.put("metricCode", code);
        row.put("metricValue", value);
        return row;
    }

    private static Map<String, Object> sampleWealth() {
        Map<String, Object> health = new LinkedHashMap<>();
        health.put("fixedBurden", 28);
        health.put("debtPressure", 15);
        Map<String, Object> wealth = new LinkedHashMap<>();
        wealth.put("savingsRate", 0.18);
        wealth.put("healthScore", health);
        return wealth;
    }
}
