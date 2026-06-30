package com.finsight.web.api.analytics;

import com.finsight.application.analytics.PeriodMetricsService;
import com.finsight.application.analytics.SemanticBreakdownService;
import com.finsight.web.api.dto.CommonResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/analytics")
public class AnalyticsMetricsController {

    private final PeriodMetricsService periodMetricsService;
    private final SemanticBreakdownService semanticBreakdownService;

    public AnalyticsMetricsController(PeriodMetricsService periodMetricsService,
                                      SemanticBreakdownService semanticBreakdownService) {
        this.periodMetricsService = periodMetricsService;
        this.semanticBreakdownService = semanticBreakdownService;
    }

    @GetMapping("/metrics/period-summary")
    public CommonResult periodSummary(@RequestParam(required = false) String from,
                                      @RequestParam(required = false) String to) {
        return CommonResult.success(periodMetricsService.periodSummary(from, to));
    }

    @GetMapping("/metrics/semantic-breakdown")
    public CommonResult semanticBreakdown(@RequestParam(required = false) String from,
                                          @RequestParam(required = false) String to,
                                          @RequestParam(required = false) String cardId,
                                          @RequestParam(required = false) String consumeID,
                                          @RequestParam(required = false, defaultValue = "expense") String scope) {
        return CommonResult.success(semanticBreakdownService.breakdown(from, to, cardId, consumeID, scope));
    }
}
