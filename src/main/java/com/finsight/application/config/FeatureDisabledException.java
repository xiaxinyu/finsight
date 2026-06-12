package com.finsight.application.config;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class FeatureDisabledException extends RuntimeException {

    private final String feature;

    public FeatureDisabledException(String feature) {
        super("Feature disabled: " + feature);
        this.feature = feature;
    }

    public String getFeature() {
        return feature;
    }
}
