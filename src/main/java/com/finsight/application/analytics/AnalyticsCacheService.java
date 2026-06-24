package com.finsight.application.analytics;

import com.finsight.application.config.FinsightFeatureProperties;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class AnalyticsCacheService {

    private final AnalyticsResultCache<Map<String, Object>> profileCache;
    private final AnalyticsResultCache<List<Map<String, Object>>> advisorCache;
    private final AnalyticsResultCache<Map<String, Object>> forecastCache;

    public AnalyticsCacheService(FinsightFeatureProperties properties) {
        FinsightFeatureProperties.Analytics analytics = properties.getAnalytics();
        profileCache = new AnalyticsResultCache<>(secondsToMillis(analytics.getProfileCacheTtlSeconds()));
        advisorCache = new AnalyticsResultCache<>(secondsToMillis(analytics.getAdvisorCacheTtlSeconds()));
        forecastCache = new AnalyticsResultCache<>(secondsToMillis(analytics.getForecastCacheTtlSeconds()));
    }

    public Map<String, Object> getProfile(String key) {
        return profileCache.get(key);
    }

    public void putProfile(String key, Map<String, Object> value) {
        profileCache.put(key, value);
    }

    public List<Map<String, Object>> getAdvisor(String key) {
        return advisorCache.get(key);
    }

    public void putAdvisor(String key, List<Map<String, Object>> value) {
        advisorCache.put(key, value);
    }

    public Map<String, Object> getForecast(String key) {
        return forecastCache.get(key);
    }

    public void putForecast(String key, Map<String, Object> value) {
        forecastCache.put(key, value);
    }

    public void invalidateProfile(String key) {
        profileCache.invalidate(key);
    }

    public void invalidateAdvisor(String key) {
        advisorCache.invalidate(key);
    }

    public void invalidateForecastsForUser(String userId) {
        forecastCache.invalidatePrefix(userId + ":");
    }

    public void clearAllForecasts() {
        forecastCache.clear();
    }

    private static long secondsToMillis(int seconds) {
        return Math.max(60, seconds) * 1000L;
    }
}
