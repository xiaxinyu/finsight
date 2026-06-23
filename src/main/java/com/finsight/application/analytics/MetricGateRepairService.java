package com.finsight.application.analytics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

/**
 * Schedules async metric refresh when reconcile gate blocks read-path report fallback.
 */
@Service
public class MetricGateRepairService {

    private static final Logger LOG = LoggerFactory.getLogger(MetricGateRepairService.class);
    private static final DateTimeFormatter YM = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final int MONTHS_TO_REPAIR = 3;

    private final MetricMonthlyService metricMonthlyService;

    public MetricGateRepairService(MetricMonthlyService metricMonthlyService) {
        this.metricMonthlyService = metricMonthlyService;
    }

    public void scheduleRepairIfGateBlocked(boolean gateBlocked) {
        if (!gateBlocked) {
            return;
        }
        YearMonth cursor = YearMonth.now();
        for (int i = 0; i < MONTHS_TO_REPAIR; i++) {
            String ym = cursor.format(YM);
            metricMonthlyService.refreshAsync(ym);
            LOG.info("metric.gate repair scheduled refresh for {}", ym);
            cursor = cursor.minusMonths(1);
        }
    }
}
