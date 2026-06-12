package com.finsight.application.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FeatureFlagServiceTest {

    private FinsightFeatureProperties properties;
    private FeatureFlagService service;

    @BeforeEach
    void setUp() {
        properties = new FinsightFeatureProperties();
        service = new FeatureFlagService(properties);
    }

    @Test
    void snapshotReflectsConfiguredFlags() {
        properties.getAdvisor().setEnabled(false);
        properties.getProfile().setEnabled(true);

        assertEquals(false, service.snapshot().get("advisor"));
        assertEquals(true, service.snapshot().get("profile"));
    }

    @Test
    void requireProfileThrowsWhenDisabled() {
        properties.getProfile().setEnabled(false);
        FeatureDisabledException ex = assertThrows(FeatureDisabledException.class, service::requireProfile);
        assertEquals("profile", ex.getFeature());
    }

    @Test
    void requireLocalAiThrowsWhenAdvisorDisabled() {
        properties.getAdvisor().setEnabled(false);
        assertThrows(FeatureDisabledException.class, service::requireLocalAi);
    }

    @Test
    void requireLocalAiThrowsWhenLocalAiDisabled() {
        properties.getAdvisor().setLocalAiEnabled(false);
        FeatureDisabledException ex = assertThrows(FeatureDisabledException.class, service::requireLocalAi);
        assertEquals("local-ai", ex.getFeature());
    }

    @Test
    void requireForecastPassesWhenEnabled() {
        properties.getForecast().setEnabled(true);
        service.requireForecast();
        assertTrue(properties.getForecast().isEnabled());
    }
}
