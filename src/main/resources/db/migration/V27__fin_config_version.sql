-- V27: Taxonomy / rule / metric version tracking (schema only).

CREATE TABLE IF NOT EXISTS fin_config_version (
    id                      INT          NOT NULL PRIMARY KEY DEFAULT 1,
    taxonomy_version        INT          NOT NULL DEFAULT 1,
    rule_set_version        INT          NOT NULL DEFAULT 1,
    metric_refresh_version  INT          NOT NULL DEFAULT 1,
    updatetime              DATETIME     NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO fin_config_version (id, taxonomy_version, rule_set_version, metric_refresh_version)
SELECT 1, 1, 1, 1 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM fin_config_version WHERE id = 1);
