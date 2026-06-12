package com.finsight.application.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@Profile("prod")
public class ProdStartupValidator {

    @Value("${account.des-sign-key:}")
    private String desSignKey;

    @Value("${spring.datasource.url:}")
    private String datasourceUrl;

    @EventListener(ApplicationReadyEvent.class)
    public void validateSecrets() {
        if (datasourceUrl == null || datasourceUrl.isBlank()) {
            throw new IllegalStateException("SPRING_DATASOURCE_URL must be set in prod profile");
        }
        if (desSignKey == null || desSignKey.isBlank() || "change-me-in-env".equals(desSignKey)) {
            throw new IllegalStateException("ACCOUNT_DES_SIGN_KEY must be set to a non-default value in prod profile");
        }
    }
}
