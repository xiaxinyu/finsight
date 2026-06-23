package com.finsight.application.analytics;

import java.util.concurrent.ConcurrentHashMap;

/**
 * In-process TTL cache for analytics read paths (profile, advisor, forecast previews).
 */
final class AnalyticsResultCache<T> {

    private final ConcurrentHashMap<String, Entry<T>> store = new ConcurrentHashMap<>();
    private final long ttlMillis;

    AnalyticsResultCache(long ttlMillis) {
        this.ttlMillis = Math.max(1_000L, ttlMillis);
    }

    T get(String key) {
        Entry<T> entry = store.get(key);
        if (entry == null) {
            return null;
        }
        if (entry.expiresAtMs < System.currentTimeMillis()) {
            store.remove(key, entry);
            return null;
        }
        return entry.value;
    }

    void put(String key, T value) {
        store.put(key, new Entry<>(value, System.currentTimeMillis() + ttlMillis));
    }

    void invalidate(String key) {
        store.remove(key);
    }

    private record Entry<T>(T value, long expiresAtMs) {
    }
}
