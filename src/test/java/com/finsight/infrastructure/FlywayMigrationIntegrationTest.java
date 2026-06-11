package com.finsight.infrastructure;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertTrue;

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
        registry.add("spring.flyway.baseline-on-migrate", () -> "true");
        registry.add("spring.flyway.baseline-version", () -> "10");
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void flywayCreatesCoreTables() {
        assertTrue(tableExists("transaction"));
        assertTrue(tableExists("cls_category"));
        assertTrue(tableExists("cls_rule"));
        assertTrue(tableExists("imp_staging_entry"));
        assertTrue(tableExists("fin_bank_account"));
        assertTrue(migrationAtLeast(11));
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
}
