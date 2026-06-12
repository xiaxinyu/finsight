-- FinSight schema inventory marker + idempotent bootstrap for fresh databases (baseline >= 10).
-- Does not drop or truncate any data.

-- Ledger (table name fixed by product constraint)
CREATE TABLE IF NOT EXISTS `transaction` (
  `version` int NOT NULL DEFAULT 0,
  `created_by` varchar(255) DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `updated_by` varchar(255) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `id` varchar(255) NOT NULL,
  `card_id` varchar(255) DEFAULT NULL,
  `transaction_date` datetime(6) DEFAULT NULL,
  `bookkeeping_date` datetime(6) DEFAULT NULL,
  `transaction_desc` text,
  `balance_currency` varchar(20) DEFAULT NULL,
  `balance_money` decimal(19,4) DEFAULT NULL,
  `card_type_id` int DEFAULT NULL,
  `card_type_name` varchar(255) DEFAULT NULL,
  `bank_card_id` varchar(64) DEFAULT NULL,
  `bank_card_name` varchar(128) DEFAULT NULL,
  `deleted` int DEFAULT NULL,
  `consumption_type` int DEFAULT NULL,
  `consume_id` varchar(255) DEFAULT NULL,
  `consume_code` varchar(64) DEFAULT NULL,
  `consume_name` varchar(255) DEFAULT NULL,
  `memo` text,
  `statement_id` varchar(255) DEFAULT NULL,
  `payment_type_id` varchar(20) DEFAULT NULL,
  `income_money` decimal(10,2) DEFAULT 0.00,
  `opponent_account` varchar(64) DEFAULT '',
  `opponent_name` varchar(128) DEFAULT '',
  `transaction_time` varchar(32) DEFAULT '',
  `account_balance` decimal(10,2) DEFAULT 0.00,
  `txn_kind` varchar(16) DEFAULT NULL,
  `transfer_group_id` varchar(64) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `cls_category` (
  `id` varchar(64) NOT NULL,
  `parent_id` varchar(64) DEFAULT NULL,
  `code` varchar(64) DEFAULT NULL,
  `name` varchar(128) NOT NULL,
  `level` int DEFAULT 0,
  `txn_types` varchar(256) DEFAULT 'expense',
  `sort_no` int DEFAULT NULL,
  `deleted` int DEFAULT 0,
  `version` int DEFAULT 0,
  `created_by` varchar(64) DEFAULT NULL,
  `created_at` datetime DEFAULT NULL,
  `updated_by` varchar(64) DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_parent` (`parent_id`),
  KEY `idx_level_sort` (`level`,`sort_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `cls_rule` (
  `id` varchar(64) NOT NULL,
  `category_id` varchar(64) DEFAULT NULL,
  `pattern` varchar(256) NOT NULL,
  `pattern_type` varchar(32) DEFAULT NULL,
  `priority` int DEFAULT 100,
  `active` int DEFAULT 1,
  `bank_code` varchar(32) DEFAULT NULL,
  `card_type_code` varchar(32) DEFAULT NULL,
  `remark` varchar(256) DEFAULT NULL,
  `version` int DEFAULT 0,
  `created_by` varchar(64) DEFAULT NULL,
  `created_at` datetime DEFAULT NULL,
  `updated_by` varchar(64) DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `min_amount` decimal(12,2) DEFAULT NULL,
  `max_amount` decimal(12,2) DEFAULT NULL,
  `start_date` date DEFAULT NULL,
  `end_date` date DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_category` (`category_id`),
  KEY `idx_active_priority` (`active`,`priority`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `cls_rule_tag` (
  `rule_id` varchar(64) NOT NULL,
  `tag` varchar(255) NOT NULL,
  KEY `idx_rule_id` (`rule_id`),
  KEY `idx_tag` (`tag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `imp_staging_entry` (
  `version` int NOT NULL DEFAULT 0,
  `created_by` varchar(255) DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `updated_by` varchar(255) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `id` varchar(255) NOT NULL,
  `card_id` varchar(255) DEFAULT NULL,
  `transaction_date` datetime(6) DEFAULT NULL,
  `bookkeeping_date` datetime(6) DEFAULT NULL,
  `transaction_desc` text,
  `balance_currency` varchar(20) DEFAULT NULL,
  `balance_money` decimal(19,4) DEFAULT NULL,
  `card_type_id` int DEFAULT NULL,
  `card_type_name` varchar(255) DEFAULT NULL,
  `bank_card_id` varchar(64) DEFAULT NULL,
  `bank_card_name` varchar(128) DEFAULT NULL,
  `deleted` int DEFAULT NULL,
  `consumption_type` int DEFAULT NULL,
  `consume_id` varchar(255) DEFAULT NULL,
  `consume_code` varchar(64) DEFAULT NULL,
  `consume_name` varchar(255) DEFAULT NULL,
  `memo` text,
  `statement_id` varchar(255) DEFAULT NULL,
  `payment_type_id` varchar(20) DEFAULT NULL,
  `income_money` decimal(10,2) DEFAULT 0.00,
  `opponent_account` varchar(64) DEFAULT '',
  `opponent_name` varchar(128) DEFAULT '',
  `transaction_time` varchar(32) DEFAULT '',
  `account_balance` decimal(10,2) DEFAULT 0.00,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `statement` (
  `version` int NOT NULL DEFAULT 0,
  `created_by` varchar(255) DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `updated_by` varchar(255) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `id` varchar(255) NOT NULL,
  `file_name` varchar(512) DEFAULT NULL,
  `row_count` int DEFAULT NULL,
  `content` longtext,
  `status` varchar(32) DEFAULT NULL,
  `source_bank_code` varchar(32) DEFAULT NULL,
  `deleted` tinyint(1) DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `fin_bank_account` (
  `id` varchar(36) NOT NULL,
  `bank_code` varchar(32) DEFAULT NULL,
  `card_type_code` varchar(32) DEFAULT NULL,
  `card_no` varchar(64) DEFAULT NULL,
  `card_name` varchar(128) DEFAULT NULL,
  `deleted` tinyint(1) DEFAULT 0,
  `created_by` varchar(64) DEFAULT NULL,
  `created_at` datetime(3) DEFAULT NULL,
  `updated_by` varchar(64) DEFAULT NULL,
  `updated_at` datetime(3) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `fs_user` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `username` varchar(64) NOT NULL,
  `password` varchar(128) NOT NULL,
  `display_name` varchar(128) DEFAULT NULL,
  `enabled` tinyint DEFAULT 1,
  `version` int DEFAULT 0,
  `created_by` varchar(64) DEFAULT NULL,
  `created_at` datetime DEFAULT NULL,
  `updated_by` varchar(64) DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `fs_role` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `code` varchar(64) NOT NULL,
  `name` varchar(128) NOT NULL,
  `description` varchar(256) DEFAULT NULL,
  `version` int DEFAULT 0,
  `created_by` varchar(64) DEFAULT NULL,
  `created_at` datetime DEFAULT NULL,
  `updated_by` varchar(64) DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `fs_user_role` (
  `user_id` bigint NOT NULL,
  `role_id` bigint NOT NULL,
  `version` int DEFAULT 0,
  `created_by` varchar(64) DEFAULT NULL,
  `created_at` datetime DEFAULT NULL,
  `updated_by` varchar(64) DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  PRIMARY KEY (`user_id`,`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `ben_contribution` (
  `id` varchar(255) NOT NULL,
  `benefit_type` varchar(32) NOT NULL,
  `unit_no` varchar(64) DEFAULT NULL,
  `unit_name` varchar(256) DEFAULT NULL,
  `period_label` varchar(16) DEFAULT NULL,
  `pay_base` decimal(19,4) DEFAULT NULL,
  `personal_pay` decimal(19,4) DEFAULT NULL,
  `unit_pay` decimal(19,4) DEFAULT NULL,
  `total_pay` decimal(19,4) DEFAULT NULL,
  `personal_reserved` decimal(19,4) DEFAULT NULL,
  `memo` text,
  `fiscal_year` smallint DEFAULT NULL,
  `created_by` varchar(255) DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `updated_by` varchar(255) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `version` int DEFAULT 0,
  `deleted` tinyint(1) DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Compatibility view for import staging legacy name
CREATE OR REPLACE VIEW `transaction_temp` AS SELECT * FROM `imp_staging_entry`;

-- Compatibility view for bank cards legacy name
CREATE OR REPLACE VIEW `bank_card` AS SELECT * FROM `fin_bank_account`;
