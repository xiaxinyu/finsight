package com.finsight.web.api.analytics;

import com.finsight.application.analytics.PeriodMetricsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AnalyticsMetricsController.class)
@AutoConfigureMockMvc(addFilters = false)
class AnalyticsMetricsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PeriodMetricsService periodMetricsService;

    @Test
    void periodSummary_returnsSemanticTotals() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("realIncome", 8000);
        body.put("consumptionExpense", 6000);
        body.put("netCashflow", 2000);
        body.put("metricsSource", "v_transaction_finance_semantics");
        when(periodMetricsService.periodSummary("01/01/2026", "06/30/2026")).thenReturn(body);

        mockMvc.perform(get("/api/v1/analytics/metrics/period-summary")
                        .param("from", "01/01/2026")
                        .param("to", "06/30/2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.realIncome").value(8000))
                .andExpect(jsonPath("$.data.metricsSource").value("v_transaction_finance_semantics"));
    }
}
