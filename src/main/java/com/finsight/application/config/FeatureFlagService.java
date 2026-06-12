package com.finsight.application.config;

import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class FeatureFlagService {

    private final FinsightFeatureProperties features;

    public FeatureFlagService(FinsightFeatureProperties features) {
        this.features = features;
    }

    public void requireAdvisor() {
        if (!features.getAdvisor().isEnabled()) {
            throw new FeatureDisabledException("advisor");
        }
    }

    public void requireLocalAi() {
        requireAdvisor();
        if (!features.getAdvisor().isLocalAiEnabled()) {
            throw new FeatureDisabledException("local-ai");
        }
    }

    public void requireProfile() {
        if (!features.getProfile().isEnabled()) {
            throw new FeatureDisabledException("profile");
        }
    }

    public void requireForecast() {
        if (!features.getForecast().isEnabled()) {
            throw new FeatureDisabledException("forecast");
        }
    }

    public void requireMerchantMining() {
        if (!features.getMerchantMining().isEnabled()) {
            throw new FeatureDisabledException("merchant-mining");
        }
    }

    public Map<String, Object> snapshot() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("planningPersist", features.getPlanning().isPersist());
        out.put("advisor", features.getAdvisor().isEnabled());
        out.put("localAi", features.getAdvisor().isLocalAiEnabled());
        out.put("profile", features.getProfile().isEnabled());
        out.put("forecast", features.getForecast().isEnabled());
        out.put("merchantMining", features.getMerchantMining().isEnabled());
        out.put("metricsReconcileGate", features.getMetrics().isReconcileGate());
        return out;
    }
}
