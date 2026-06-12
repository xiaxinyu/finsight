-- Clear failed V17 so Flyway can retry after script fix.
-- Run in MySQL client, then restart the app.

SELECT installed_rank, version, description, success, installed_on
FROM flyway_schema_history
WHERE success = 0;

DELETE FROM flyway_schema_history
WHERE version = '17' AND success = 0;

SELECT installed_rank, version, description, success
FROM flyway_schema_history
ORDER BY installed_rank DESC
LIMIT 5;
