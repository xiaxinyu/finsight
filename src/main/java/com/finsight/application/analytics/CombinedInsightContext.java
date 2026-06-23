package com.finsight.application.analytics;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Shared read-only context for combined insight cards within a single request.
 */
public record CombinedInsightContext(
        Map<String, Object> profile,
        Map<String, Object> trends,
        Map<String, Object> forecast,
        Map<String, Object> subscriptions,
        Map<String, Object> concentration,
        List<Map<String, Object>> cards) {

    public List<Map<String, Object>> topCards(int limit) {
        List<Map<String, Object>> sorted = new ArrayList<>(cards);
        sorted.sort(Comparator.comparingInt(
                c -> -((Number) c.getOrDefault("priority", 0)).intValue()));
        if (sorted.size() <= limit) {
            return sorted;
        }
        return new ArrayList<>(sorted.subList(0, limit));
    }
}
