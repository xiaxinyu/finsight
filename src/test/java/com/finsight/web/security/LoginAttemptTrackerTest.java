package com.finsight.web.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoginAttemptTrackerTest {

    @Test
    void blocksAfterMaxAttemptsWithinWindow() {
        LoginAttemptTracker tracker = new LoginAttemptTracker();
        String key = "127.0.0.1";
        for (int i = 0; i < 3; i++) {
            tracker.recordFailure(key);
        }
        assertTrue(tracker.isBlocked(key, 3, 900));
    }

    @Test
    void resetsAfterSuccessfulLogin() {
        LoginAttemptTracker tracker = new LoginAttemptTracker();
        String key = "127.0.0.1";
        tracker.recordFailure(key);
        tracker.recordFailure(key);
        tracker.reset(key);
        assertFalse(tracker.isBlocked(key, 3, 900));
    }
}
