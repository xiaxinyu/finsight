package com.finsight.application.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProdStartupValidatorTest {

    private ProdStartupValidator validator;

    @BeforeEach
    void setUp() {
        validator = new ProdStartupValidator();
    }

    @Test
    void acceptsValidProdSecrets() {
        apply("jdbc:mysql://db.example.com/finsight", "app_user", "strong-pass", "prod-sign-key");

        assertDoesNotThrow(() -> validator.validateProdSecrets());
    }

    @Test
    void rejectsMissingDatasourceUrl() {
        apply("", "app_user", "strong-pass", "prod-sign-key");

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> validator.validateProdSecrets());
        assertEquals("SPRING_DATASOURCE_URL must be set in prod profile", ex.getMessage());
    }

    @Test
    void rejectsDefaultDatasourceUsername() {
        apply("jdbc:mysql://db.example.com/finsight", "root", "strong-pass", "prod-sign-key");

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> validator.validateProdSecrets());
        assertEquals(
                "SPRING_DATASOURCE_USERNAME must not use the default dev value 'root' in prod profile",
                ex.getMessage());
    }

    @Test
    void rejectsDefaultDatasourcePassword() {
        apply("jdbc:mysql://db.example.com/finsight", "app_user", "123456", "prod-sign-key");

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> validator.validateProdSecrets());
        assertEquals(
                "SPRING_DATASOURCE_PASSWORD must not use the default dev value '123456' in prod profile",
                ex.getMessage());
    }

    @Test
    void rejectsDefaultSignKey() {
        apply("jdbc:mysql://db.example.com/finsight", "app_user", "strong-pass", "change-me-in-env");

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> validator.validateProdSecrets());
        assertEquals("ACCOUNT_DES_SIGN_KEY must be set to a non-default value in prod profile", ex.getMessage());
    }

    private void apply(String url, String username, String password, String signKey) {
        ReflectionTestUtils.setField(validator, "datasourceUrl", url);
        ReflectionTestUtils.setField(validator, "datasourceUsername", username);
        ReflectionTestUtils.setField(validator, "datasourcePassword", password);
        ReflectionTestUtils.setField(validator, "desSignKey", signKey);
    }
}
