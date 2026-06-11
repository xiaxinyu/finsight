package com.finsight.application.consume;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "finsight.classification")
public class ClassificationProperties {

    /** When true, statement import assigns OTHER-01 if no rule matches. Default false to avoid masking gaps. */
    private boolean importOtherFallback = false;

    public boolean isImportOtherFallback() {
        return importOtherFallback;
    }

    public void setImportOtherFallback(boolean importOtherFallback) {
        this.importOtherFallback = importOtherFallback;
    }
}
