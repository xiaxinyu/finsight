package com.finsight.application.analytics;

import com.finsight.application.classification.ConfigVersionService;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Builds analytics cache keys including user, as-of date, and metric refresh version.
 */
@Component
public class AnalyticsCacheKeySupport {

    private final ConfigVersionService configVersionService;

    public AnalyticsCacheKeySupport(ConfigVersionService configVersionService) {
        this.configVersionService = configVersionService;
    }

    public String profileKey(String userId) {
        return userId + ":" + LocalDate.now() + ":m" + metricRefreshVersion();
    }

    public String forecastKey(String userId, int year, String scenario, ForecastScenarioParams adjustments) {
        return userId + ":" + year + ":" + scenario + ":" + adjustments.cacheKey() + ":m" + metricRefreshVersion();
    }

    public String advisorKey(String userId) {
        return "advisor:" + userId + ":" + LocalDate.now() + ":m" + metricRefreshVersion();
    }

    private long metricRefreshVersion() {
        Object version = configVersionService.asMap().get("metricRefreshVersion");
        if (version instanceof Number n) {
            return n.longValue();
        }
        return 1L;
    }
}
