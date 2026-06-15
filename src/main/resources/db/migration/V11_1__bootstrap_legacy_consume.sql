-- V12 counts rows in legacy consume_* tables; empty shells satisfy the guard on fresh DBs.
CREATE TABLE IF NOT EXISTS consume_category (
    id varchar(64) NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS consume_rule (
    id varchar(64) NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS consume_rule_tag (
    rule_id varchar(64) NOT NULL,
    tag varchar(255) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
