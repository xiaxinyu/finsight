package com.finsight.web.security;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LoginAttemptTracker {

    private final Map<String, AttemptWindow> attempts = new ConcurrentHashMap<>();

    public boolean isBlocked(String clientKey, int maxAttempts, long windowSeconds) {
        if (clientKey == null || clientKey.isBlank()) {
            return false;
        }
        return attempts.computeIfAbsent(clientKey, k -> new AttemptWindow()).isBlocked(maxAttempts, windowSeconds);
    }

    public void recordFailure(String clientKey) {
        if (clientKey == null || clientKey.isBlank()) {
            return;
        }
        attempts.computeIfAbsent(clientKey, k -> new AttemptWindow()).recordFailure();
    }

    public void reset(String clientKey) {
        if (clientKey == null || clientKey.isBlank()) {
            return;
        }
        AttemptWindow window = attempts.get(clientKey);
        if (window != null) {
            window.reset();
        }
    }

    public static String clientKey(jakarta.servlet.http.HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    static final class AttemptWindow {
        private int count;
        private Instant firstAttempt = Instant.now();

        synchronized boolean isBlocked(int maxAttempts, long windowSeconds) {
            refreshWindow(windowSeconds);
            return count >= maxAttempts;
        }

        synchronized void recordFailure() {
            count++;
        }

        synchronized void reset() {
            count = 0;
            firstAttempt = Instant.now();
        }

        private void refreshWindow(long windowSeconds) {
            if (Instant.now().isAfter(firstAttempt.plusSeconds(windowSeconds))) {
                count = 0;
                firstAttempt = Instant.now();
            }
        }
    }
}
