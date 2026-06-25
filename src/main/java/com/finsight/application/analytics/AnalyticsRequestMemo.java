package com.finsight.application.analytics;

import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import java.util.HashMap;
import java.util.Map;

/**
 * Request-scoped memoization so Profile, Advisor, and Forecast are not recomputed twice per HTTP request.
 */
@Component
@RequestScope
public class AnalyticsRequestMemo {

    private Map<String, Object> profile;
    private final Map<String, Map<String, Object>> forecasts = new HashMap<>();

    public Map<String, Object> getProfile() {
        return profile;
    }

    public void setProfile(Map<String, Object> profile) {
        this.profile = profile;
    }

    public void clearProfile() {
        this.profile = null;
    }

    public Map<String, Object> getForecast(String key) {
        return forecasts.get(key);
    }

    public void setForecast(String key, Map<String, Object> forecast) {
        forecasts.put(key, forecast);
    }
}
