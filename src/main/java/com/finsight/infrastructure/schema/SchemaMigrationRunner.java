package com.finsight.infrastructure.schema;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

@Component
public class SchemaMigrationRunner implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger log = LoggerFactory.getLogger(SchemaMigrationRunner.class);
    private static final Pattern STATEMENT_SPLIT = Pattern.compile(";\\s*\n");

    private final JdbcTemplate jdbcTemplate;

    public SchemaMigrationRunner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        try {
            runClasspathSql("db/migration/V1_personal_finance.sql");
            ensureTransactionColumns();
            log.info("Personal finance schema migration completed");
        } catch (Exception e) {
            log.error("Schema migration failed", e);
        }
    }

    private void runClasspathSql(String path) throws Exception {
        String sql = StreamUtils.copyToString(new ClassPathResource(path).getInputStream(), StandardCharsets.UTF_8);
        for (String stmt : STATEMENT_SPLIT.split(sql)) {
            String trimmed = stmt.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("--")) {
                continue;
            }
            jdbcTemplate.execute(trimmed);
        }
    }

    private void ensureTransactionColumns() {
        addColumnIfMissing("transaction", "txn_kind", "VARCHAR(16) NULL");
        addColumnIfMissing("transaction", "transfer_group_id", "VARCHAR(64) NULL");
    }

    private void addColumnIfMissing(String table, String column, String definition) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?",
                Integer.class, table, column);
        if (count != null && count == 0) {
            jdbcTemplate.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
            log.info("Added column {}.{}", table, column);
        }
    }
}
