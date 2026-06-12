package com.finsight.web.api.analytics;

import com.finsight.application.analytics.CashRiskCalendarService;
import com.finsight.application.analytics.ForecastService;
import com.finsight.application.analytics.TrendAnalysisService;
import com.finsight.web.api.dto.CommonResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/analytics")
public class AnalyticsForecastController {

    private final ForecastService forecastService;
    private final TrendAnalysisService trendAnalysisService;
    private final CashRiskCalendarService cashRiskCalendarService;

    public AnalyticsForecastController(ForecastService forecastService,
                                       TrendAnalysisService trendAnalysisService,
                                       CashRiskCalendarService cashRiskCalendarService) {
        this.forecastService = forecastService;
        this.trendAnalysisService = trendAnalysisService;
        this.cashRiskCalendarService = cashRiskCalendarService;
    }

    @GetMapping("/forecast")
    public CommonResult forecast(@RequestParam int year,
                                 @RequestParam(defaultValue = "base") String scenario) throws Exception {
        return CommonResult.success(forecastService.forecast(year, scenario));
    }

    @GetMapping("/forecast/categories")
    public CommonResult forecastCategories(@RequestParam int year) throws Exception {
        return CommonResult.success(forecastService.forecast(year, "base"));
    }

    @GetMapping("/trends")
    public CommonResult trends(@RequestParam int fromYear, @RequestParam int toYear) throws Exception {
        return CommonResult.success(trendAnalysisService.trends(fromYear, toYear));
    }

    @PostMapping("/scenarios")
    public CommonResult scenarios(@RequestBody Map<String, Object> body) throws Exception {
        return CommonResult.success(forecastService.simulateScenario(body));
    }

    @GetMapping("/cash-risk-calendar")
    public CommonResult cashRiskCalendar(@RequestParam int year,
                                         @RequestParam(defaultValue = "stress") String scenario) throws Exception {
        return CommonResult.success(cashRiskCalendarService.calendar(year, scenario));
    }
}
