package com.finsight.application.analytics;

import org.springframework.stereotype.Component;

import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Schedules async monthly metric refresh after ledger mutations (import, classify, budget).
 */
@Component
public class MetricRefreshTrigger {

    private static final DateTimeFormatter YM = DateTimeFormatter.ofPattern("yyyy-MM");

    private final MetricMonthlyService metricMonthlyService;

    public MetricRefreshTrigger(MetricMonthlyService metricMonthlyService) {
        this.metricMonthlyService = metricMonthlyService;
    }

    public void afterTransactionsChanged(Collection<Date> transactionDates) {
        Set<String> months = new LinkedHashSet<>();
        months.add(YearMonth.now().format(YM));
        if (transactionDates != null) {
            for (Date d : transactionDates) {
                if (d == null) {
                    continue;
                }
                YearMonth ym = YearMonth.from(d.toInstant().atZone(ZoneId.systemDefault()));
                months.add(ym.format(YM));
            }
        }
        for (String month : months) {
            metricMonthlyService.refreshAsync(month);
        }
    }

    public void refreshCurrentMonth() {
        metricMonthlyService.refreshAsync(YearMonth.now().format(YM));
    }
}
