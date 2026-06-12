-- Repair hook: if a prior V2 attempt failed against Django auth_user name clash, clear failed row.
DELETE FROM flyway_schema_history WHERE version = '2' AND success = 0;
