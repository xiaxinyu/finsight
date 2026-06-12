package com.finsight.application.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Set;

@Component
@Profile("prod")
public class ProdStartupValidator {

    private static final Set<String> FORBIDDEN_SIGN_KEYS = Set.of("change-me-in-env");
    private static final Set<String> FORBIDDEN_DB_PASSWORDS = Set.of("123456");
    private static final Set<String> FORBIDDEN_DB_USERNAMES = Set.of("root");

    @Value("${account.des-sign-key:}")
    private String desSignKey;

    @Value("${spring.datasource.url:}")
    private String datasourceUrl;

    @Value("${spring.datasource.username:}")
    private String datasourceUsername;

    @Value("${spring.datasource.password:}")
    private String datasourcePassword;

    @PostConstruct
    void validateOnStartup() {
        validateProdSecrets();
    }

    void validateProdSecrets() {
        if (!StringUtils.hasText(datasourceUrl)) {
            throw new IllegalStateException("SPRING_DATASOURCE_URL must be set in prod profile");
        }
        if (!StringUtils.hasText(datasourceUsername)) {
            throw new IllegalStateException("SPRING_DATASOURCE_USERNAME must be set in prod profile");
        }
        if (!StringUtils.hasText(datasourcePassword)) {
            throw new IllegalStateException("SPRING_DATASOURCE_PASSWORD must be set in prod profile");
        }
        if (FORBIDDEN_DB_USERNAMES.contains(datasourceUsername.trim())) {
            throw new IllegalStateException(
                    "SPRING_DATASOURCE_USERNAME must not use the default dev value 'root' in prod profile");
        }
        if (FORBIDDEN_DB_PASSWORDS.contains(datasourcePassword)) {
            throw new IllegalStateException(
                    "SPRING_DATASOURCE_PASSWORD must not use the default dev value '123456' in prod profile");
        }
        if (!StringUtils.hasText(desSignKey) || FORBIDDEN_SIGN_KEYS.contains(desSignKey.trim())) {
            throw new IllegalStateException("ACCOUNT_DES_SIGN_KEY must be set to a non-default value in prod profile");
        }
    }
}
