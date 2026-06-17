package com.finsight.infrastructure;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Runs when Docker is available; CI job {@code backend-flyway} uses {@code -Dtest=FlywayMigrationIntegrationTest}. */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
class FlywayMigrationIntegrationTest {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("finsight_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void datasourceProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.flyway.baseline-on-migrate", () -> "true");
        registry.add("spring.flyway.baseline-version", () -> "10");
        registry.add("spring.flyway.out-of-order", () -> "true");
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void flywayCreatesCoreTables() {
        assertTrue(tableExists("transaction"));
        assertTrue(tableExists("statement"));
        assertTrue(tableExists("cls_category"));
        assertTrue(tableExists("cls_rule"));
        assertTrue(tableExists("imp_staging_entry"));
        assertTrue(tableExists("fin_bank_account"));
        assertTrue(migrationAtLeast(21));
    }

    private boolean migrationAtLeast(int version) {
        Integer max = jdbcTemplate.queryForObject(
                "select coalesce(max(cast(version as unsigned)),0) from flyway_schema_history where success=1",
                Integer.class);
        return max != null && max >= version;
    }

    private boolean tableExists(String name) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.tables where table_schema = database() and table_name = ?",
                Integer.class,
                name);
        return count != null && count > 0;
    }

    /**
     * Mirrors production legacy databases: schema already at V10, forward migrations are V11+.
     * Empty Testcontainers DBs are baselined at 10 instead of replaying V0–V10 legacy renames.
     */
    @TestConfiguration
    static class BaselineLegacySchemaAtV10 {
        @Bean
        FlywayMigrationStrategy flywayMigrationStrategy() {
            return flyway -> {
                if (flyway.info().all().length == 0) {
                    flyway.baseline();
                }
                flyway.migrate();
            };
        }
    }
}
