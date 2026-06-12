-- Archive legacy duplicate consume_* tables (RENAME preserves all rows; no DROP).
-- Skipped automatically if tables are absent or row counts diverge (manual review required).

-- consume_category → _archive_consume_category
SET @do_rename := (
    SELECT CASE WHEN
        (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'consume_category') > 0
        AND (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'cls_category') > 0
        AND (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = '_archive_consume_category') = 0
        AND (SELECT COUNT(*) FROM consume_category) = (SELECT COUNT(*) FROM cls_category)
    THEN 1 ELSE 0 END
);
SET @sql := IF(@do_rename = 1, 'RENAME TABLE consume_category TO _archive_consume_category', 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- consume_rule → _archive_consume_rule
SET @do_rename := (
    SELECT CASE WHEN
        (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'consume_rule') > 0
        AND (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'cls_rule') > 0
        AND (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = '_archive_consume_rule') = 0
        AND (SELECT COUNT(*) FROM consume_rule) = (SELECT COUNT(*) FROM cls_rule)
    THEN 1 ELSE 0 END
);
SET @sql := IF(@do_rename = 1, 'RENAME TABLE consume_rule TO _archive_consume_rule', 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- consume_rule_tag → _archive_consume_rule_tag
SET @do_rename := (
    SELECT CASE WHEN
        (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'consume_rule_tag') > 0
        AND (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = '_archive_consume_rule_tag') = 0
    THEN 1 ELSE 0 END
);
SET @sql := IF(@do_rename = 1, 'RENAME TABLE consume_rule_tag TO _archive_consume_rule_tag', 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
