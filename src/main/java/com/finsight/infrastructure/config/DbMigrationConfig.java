package com.finsight.infrastructure.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

@Component
@Configuration
public class DbMigrationConfig implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(DbMigrationConfig.class);

    @Autowired
    private DataSource dataSource;

    @Override
    public void run(String... args) throws Exception {
        migrate();
    }

    public void migrate() {
        log.info("Starting database migration check...");
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            // 1. Create consume_rule_tag table if not exists
            String createTableSql = "CREATE TABLE IF NOT EXISTS consume_rule_tag (" +
                    "rule_id VARCHAR(64) NOT NULL, " +
                    "tag VARCHAR(255) NOT NULL, " +
                    "INDEX idx_rule_id (rule_id), " +
                    "INDEX idx_tag (tag)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
            stmt.execute(createTableSql);
            log.info("Checked/Created consume_rule_tag table.");

            // 2. Check if data migration is needed
            // Check if consume_rule_tag is empty
            boolean tagTableEmpty = true;
            try (ResultSet rs = stmt.executeQuery("SELECT count(*) FROM consume_rule_tag")) {
                if (rs.next() && rs.getInt(1) > 0) {
                    tagTableEmpty = false;
                }
            }

            if (tagTableEmpty) {
                log.info("consume_rule_tag is empty, checking for legacy data in consume_rule...");
                // Check if 'tag' column exists in consume_rule
                boolean hasTagColumn = false;
                try (ResultSet rs = conn.getMetaData().getColumns(null, null, "consume_rule", "tag")) {
                    if (rs.next()) {
                        hasTagColumn = true;
                    }
                }

                if (hasTagColumn) {
                    log.info("Found legacy 'tag' column in consume_rule. Migrating data...");
                    String migrateSql = "INSERT INTO consume_rule_tag (rule_id, tag) " +
                            "SELECT id, tag FROM consume_rule " +
                            "WHERE tag IS NOT NULL AND tag != ''";
                    int count = stmt.executeUpdate(migrateSql);
                    log.info("Migrated {} tags from consume_rule to consume_rule_tag.", count);
                } else {
                    log.info("No legacy 'tag' column found in consume_rule. Skipping migration.");
                }
            } else {
                log.info("consume_rule_tag already has data. Skipping migration.");
            }

        } catch (Exception e) {
            log.error("Database migration failed", e);
        }
    }
}
