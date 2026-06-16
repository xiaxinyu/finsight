package com.finsight.application.analytics;

import com.finsight.application.authentication.AuthenticationFacade;
import com.finsight.application.finance.BillService;
import com.finsight.application.finance.BudgetService;
import com.finsight.common.exception.AppServiceException;
import com.finsight.domain.model.Budget;
import com.finsight.domain.model.BudgetLine;
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
import java.time.YearMonth;
import java.util.ArrayList;
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
    private BudgetService budgetService;

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
    void forecast_includesConfidenceBoundsAndBudgetSuggestion() throws Exception {
        stubCommon();
        stubCategoryHistoryEmpty();
        when(jdbcTemplate.queryForObject(contains("information_schema"), eq(Integer.class), eq("fin_forecast_line")))
                .thenReturn(1);

        Map<String, Object> out = service.forecast(2026, "stress");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> months = (List<Map<String, Object>>) out.get("months");
        Map<String, Object> jan = months.get(0);
        assertNotNull(jan.get("netLower"));
        assertNotNull(jan.get("netUpper"));
        assertNotNull(jan.get("incomeLower"));
        assertNotNull(jan.get("expenseUpper"));
        assertTrue(((Number) jan.get("incomeUpper")).doubleValue()
                >= ((Number) jan.get("incomeLower")).doubleValue());

        assertNotNull(out.get("yearNetLower"));
        assertNotNull(out.get("yearNetUpper"));

        @SuppressWarnings("unchecked")
        Map<String, Object> confidence = (Map<String, Object>) out.get("confidence");
        assertEquals(15.0, ((Number) confidence.get("halfWidthPct")).doubleValue());

        @SuppressWarnings("unchecked")
        Map<String, Object> suggestion = (Map<String, Object>) out.get("budgetSuggestion");
        assertNotNull(suggestion.get("monthlyCap"));
        assertNotNull(suggestion.get("annualCap"));
        assertNotNull(suggestion.get("note"));
        assertNotNull(out.get("budgetTarget"));
        assertNotNull(out.get("explanation"));
    }

    @Test
    void simulateScenario_incomeChangePctReducesYearIncome() throws Exception {
        stubCommon();
        stubCategoryHistoryEmpty();
        Map<String, Object> base = service.simulateScenario(Map.of("year", 2026, "scenario", "base"));
        Map<String, Object> reduced = service.simulateScenario(Map.of(
                "year", 2026,
                "scenario", "base",
                "incomeChangePct", -10
        ));
        assertTrue(((Number) reduced.get("yearIncome")).doubleValue()
                < ((Number) base.get("yearIncome")).doubleValue());
        @SuppressWarnings("unchecked")
        List<String> explanation = (List<String>) reduced.get("explanation");
        assertTrue(explanation.stream().anyMatch(s -> s.contains("-10.0%")));
    }

    @Test
    void simulateScenario_newMonthlyBillIncreasesYearExpense() throws Exception {
        stubCommon();
        stubCategoryHistoryEmpty();
        Map<String, Object> base = service.simulateScenario(Map.of("year", 2026, "scenario", "base"));
        Map<String, Object> withBill = service.simulateScenario(Map.of(
                "year", 2026,
                "scenario", "base",
                "newMonthlyBill", 500
        ));
        assertEquals(
                ((Number) base.get("yearExpense")).doubleValue() + 6000,
                ((Number) withBill.get("yearExpense")).doubleValue(),
                0.01);
    }

    @Test
    void simulateScenario_lumpSumExpenseAddsToJanuary() throws Exception {
        stubCommon();
        stubCategoryHistoryEmpty();
        Map<String, Object> base = service.simulateScenario(Map.of("year", 2026, "scenario", "base"));
        Map<String, Object> withLump = service.simulateScenario(Map.of(
                "year", 2026,
                "scenario", "base",
                "lumpSumExpense", 10000
        ));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> baseMonths = (List<Map<String, Object>>) base.get("months");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> lumpMonths = (List<Map<String, Object>>) withLump.get("months");
        double janBase = ((Number) baseMonths.get(0).get("expense")).doubleValue();
        double janLump = ((Number) lumpMonths.get(0).get("expense")).doubleValue();
        assertEquals(janBase + 10000, janLump, 0.01);
        assertEquals(
                ((Number) base.get("yearExpense")).doubleValue() + 10000,
                ((Number) withLump.get("yearExpense")).doubleValue(),
                0.01);
    }

    @Test
    void forecast_marksCompletedMonthsAsActual() throws Exception {
        stubCommon();
        stubCategoryHistoryEmpty();
        List<Map<String, Object>> history = new ArrayList<>(sampleHistory());
        history.add(metricRow("2026-01", MetricCode.INCOME_TOTAL.name(), 9000));
        history.add(metricRow("2026-01", MetricCode.EXPENSE_TOTAL.name(), 4800));
        when(metricRepository.listForUser(anyString(), anyString(), anyString())).thenReturn(history);

        Map<String, Object> out = service.forecast(2026, "base");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> months = (List<Map<String, Object>>) out.get("months");
        Map<String, Object> jan = months.get(0);
        if (YearMonth.of(2026, 1).isBefore(YearMonth.now())) {
            assertEquals(true, jan.get("actual"));
            assertEquals(9000.0, ((Number) jan.get("income")).doubleValue());
        }
    }

    @Test
    void forecast_scenarioChangesYearNetAndConfidenceWidth() throws Exception {
        stubCommon();
        stubCategoryHistoryEmpty();
        when(jdbcTemplate.queryForObject(contains("information_schema"), eq(Integer.class), eq("fin_forecast_line")))
                .thenReturn(0);

        Map<String, Object> base = service.forecast(2026, "base");
        Map<String, Object> stress = service.forecast(2026, "stress");

        assertTrue(((Number) stress.get("yearNet")).doubleValue()
                < ((Number) base.get("yearNet")).doubleValue());

        @SuppressWarnings("unchecked")
        Map<String, Object> baseConf = (Map<String, Object>) base.get("confidence");
        @SuppressWarnings("unchecked")
        Map<String, Object> stressConf = (Map<String, Object>) stress.get("confidence");
        assertTrue(((Number) stressConf.get("halfWidthPct")).doubleValue()
                > ((Number) baseConf.get("halfWidthPct")).doubleValue());
    }

    @Test
    void forecast_includesTopCategoryForecasts() throws Exception {
        stubCommon();
        when(jdbcTemplate.queryForList(contains("v_transaction_analytics"), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(sampleCategoryHistory());
        when(jdbcTemplate.queryForObject(contains("information_schema"), eq(Integer.class), eq("fin_forecast_line")))
                .thenReturn(0);

        Map<String, Object> out = service.forecast(2026, "base");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> categories = (List<Map<String, Object>>) out.get("categoryForecasts");
        assertEquals(1, categories.size());
        assertEquals("food", categories.get(0).get("categoryCode"));
        assertEquals("Food", categories.get(0).get("categoryName"));
        assertNotNull(categories.get(0).get("yearTotalLower"));
        assertNotNull(categories.get(0).get("yearTotalUpper"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> catMonths = (List<Map<String, Object>>) categories.get(0).get("months");
        assertEquals(12, catMonths.size());
        assertNotNull(catMonths.get(0).get("amountLower"));
    }

    @Test
    void categoryForecasts_returnsDedicatedPayload() throws Exception {
        stubCommon();
        when(jdbcTemplate.queryForList(contains("v_transaction_analytics"), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(sampleCategoryHistory());
        when(jdbcTemplate.queryForObject(contains("information_schema"), eq(Integer.class), eq("fin_forecast_line")))
                .thenReturn(0);

        Map<String, Object> out = service.categoryForecasts(2026, "optimistic");

        assertEquals(2026, out.get("year"));
        assertEquals("optimistic", out.get("scenario"));
        assertNotNull(out.get("categories"));
        assertNotNull(out.get("confidence"));
    }

    @Test
    void forecast_persistsAggregateAndCategoryLines() throws Exception {
        stubCommon();
        when(jdbcTemplate.queryForList(contains("v_transaction_analytics"), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(sampleCategoryHistory());
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
        verify(jdbcTemplate, times(48)).update(
                argThat((String sql) -> sql.contains("insert into fin_forecast_line")),
                any(), any(), any(), any(), any(), any(), any());

        ArgumentCaptor<String> metricCodes = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate, times(48)).update(
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
        assertEquals(12, codes.stream()
                .filter(c -> c.startsWith(ForecastService.METRIC_CATEGORY_EXPENSE_PREFIX))
                .count());
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

    private void stubCommon() throws Exception {
        when(authenticationFacade.getUserName()).thenReturn("user1");
        when(metricGateService.status(3)).thenReturn(Map.of("ok", true));
        when(metricGateService.useReportFallback()).thenReturn(false);
        when(metricRepository.listForUser(anyString(), anyString(), anyString())).thenReturn(sampleHistory());
        when(billService.listEnabled()).thenReturn(List.of());
        stubBudget(8000);
    }

    private void stubCategoryHistoryEmpty() {
        when(jdbcTemplate.queryForList(contains("v_transaction_analytics"), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(List.of());
    }

    private void stubBudget(double monthlyCap) {
        Budget budget = new Budget();
        budget.setId("monthly-budget");
        when(budgetService.currentMonthlyBudget()).thenReturn(budget);
        if (monthlyCap > 0) {
            BudgetLine line = new BudgetLine();
            line.setBucketKey("all");
            line.setLimitAmount(BigDecimal.valueOf(monthlyCap));
            when(budgetService.linesForBudget(anyString())).thenReturn(List.of(line));
        } else {
            when(budgetService.linesForBudget(anyString())).thenReturn(List.of());
        }
    }

    private static List<Map<String, Object>> sampleHistory() {
        return List.of(
                metricRow("2025-06", MetricCode.INCOME_TOTAL.name(), 8000),
                metricRow("2025-07", MetricCode.INCOME_TOTAL.name(), 8200),
                metricRow("2025-06", MetricCode.EXPENSE_TOTAL.name(), 5000),
                metricRow("2025-07", MetricCode.EXPENSE_TOTAL.name(), 5100)
        );
    }

    private static List<Map<String, Object>> sampleCategoryHistory() {
        return List.of(
                categoryRow("food", "Food", "2025-06", 1200),
                categoryRow("food", "Food", "2025-07", 1300)
        );
    }

    private static Map<String, Object> metricRow(String month, String code, double value) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("monthKey", month);
        row.put("metricCode", code);
        row.put("metricValue", value);
        return row;
    }

    private static Map<String, Object> categoryRow(String code, String name, String month, double amount) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("category_code", code);
        row.put("category_name", name);
        row.put("month_key", month);
        row.put("amount", amount);
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
