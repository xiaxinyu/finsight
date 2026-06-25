package com.finsight.web.api.analytics;

import com.finsight.application.analytics.PeriodMetricsService;
import com.finsight.web.api.dto.CommonResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/analytics")
public class AnalyticsMetricsController {

    private final PeriodMetricsService periodMetricsService;

    public AnalyticsMetricsController(PeriodMetricsService periodMetricsService) {
        this.periodMetricsService = periodMetricsService;
    }

    @GetMapping("/metrics/period-summary")
    public CommonResult periodSummary(@RequestParam(required = false) String from,
                                      @RequestParam(required = false) String to) {
        return CommonResult.success(periodMetricsService.periodSummary(from, to));
    }
}
