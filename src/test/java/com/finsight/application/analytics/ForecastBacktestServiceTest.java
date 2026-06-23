package com.finsight.application.analytics;

import com.finsight.application.authentication.AuthenticationFacade;
import com.finsight.domain.model.MetricCode;
import com.finsight.domain.port.MetricMonthlyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ForecastBacktestServiceTest {

    @Mock
    private MetricMonthlyRepository metricRepository;
    @Mock
    private AuthenticationFacade authenticationFacade;

    private ForecastBacktestService service;

    @BeforeEach
    void setUp() {
        service = new ForecastBacktestService(metricRepository, authenticationFacade);
        when(authenticationFacade.getUserName()).thenReturn("tester");
    }

    @Test
    void backtest_computesMapeFromStoredForecasts() {
        String month = YearMonth.now().minusMonths(1).format(DateTimeFormatter.ofPattern("yyyy-MM"));
        when(metricRepository.listForUser(eq("tester"), anyString(), anyString())).thenReturn(List.of(
                row(month, MetricCode.INCOME_TOTAL.name(), 10000),
                row(month, ForecastService.METRIC_INCOME_FORECAST, 9000),
                row(month, MetricCode.EXPENSE_TOTAL.name(), 5000),
                row(month, ForecastService.METRIC_EXPENSE_FORECAST, 4500)));

        Map<String, Object> out = service.backtest(1);

        assertNotNull(out.get("incomeMape"));
        assertNotNull(out.get("expenseMape"));
        assertTrue(((Number) out.get("incomeMape")).doubleValue() > 0);
    }

    private static Map<String, Object> row(String ym, String code, double value) {
        return Map.of("yearMonth", ym, "metricCode", code, "metricValue", value);
    }
}
