package com.finsight.application.analytics;

import com.finsight.application.finance.BillService;
import com.finsight.application.finance.FinancialGoalService;
import com.finsight.application.finance.UserFinancePreferencesService;
import com.finsight.domain.model.Bill;
import com.finsight.domain.model.FinancialGoal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CashRiskCalendarServiceTest {

    @Mock
    private ForecastService forecastService;

    @Mock
    private BillService billService;

    @Mock
    private FinancialGoalService goalService;

    @Mock
    private UserFinancePreferencesService financePreferencesService;

    @InjectMocks
    private CashRiskCalendarService service;

    @Test
    void calendarIncludesBillIncomeAndGoalEvents() throws Exception {
        when(financePreferencesService.incomePayDays()).thenReturn(new int[] {5, 20});
        when(forecastService.forecast(anyInt(), anyString())).thenReturn(Map.of(
                "months", List.of(Map.of(
                        "yearMonth", "2026-03",
                        "income", 10000,
                        "expense", 12000,
                        "net", -2000
                )),
                "deficitMonths", List.of("2026-03"),
                "metricsGate", Map.of("ok", true),
                "metricsSource", "fin_metric_monthly"
        ));

        Bill bill = new Bill();
        bill.setId("b1");
        bill.setName("Rent");
        bill.setDueDay(10);
        bill.setAmount(BigDecimal.valueOf(3000));
        when(billService.listEnabled()).thenReturn(List.of(bill));

        FinancialGoal goal = new FinancialGoal();
        goal.setId("g1");
        goal.setName("Emergency fund");
        goal.setMonthlyContribution(BigDecimal.valueOf(500));
        when(goalService.list()).thenReturn(List.of(goal));

        @SuppressWarnings("unchecked")
        Map<String, Object> out = service.calendar(2026, "stress");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> days = (List<Map<String, Object>>) out.get("days");

        assertEquals("stress", out.get("scenario"));
        assertTrue(days.stream().anyMatch(d -> "2026-03-10".equals(d.get("date"))
                && hasEventType(d, "bill")));
        assertTrue(days.stream().anyMatch(d -> "2026-03-01".equals(d.get("date"))
                && hasEventType(d, "goal")));
        assertTrue(days.stream().anyMatch(d -> "2026-03-05".equals(d.get("date"))
                && hasEventType(d, "income")));
    }

    @Test
    void riskLevelForMonthMarksDeficitAsHigh() {
        assertEquals("high", CashRiskCalendarService.riskLevelForMonth(-1, true));
        assertEquals("medium", CashRiskCalendarService.riskLevelForMonth(200, false));
        assertEquals("low", CashRiskCalendarService.riskLevelForMonth(2000, false));
    }

    @SuppressWarnings("unchecked")
    private static boolean hasEventType(Map<String, Object> day, String type) {
        List<Map<String, Object>> events = (List<Map<String, Object>>) day.get("events");
        return events.stream().anyMatch(e -> type.equals(e.get("type")));
    }
}
