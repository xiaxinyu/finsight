-- FinSight v2.0.0: remove preview forecast runs and aged duplicate persisted runs.
-- Preview runs use id prefix 'preview-' and are safe to delete anytime.
-- Rollback: no schema change; deleted rows are not recoverable without backup.

START TRANSACTION;

DELETE FROM fin_forecast_line
WHERE run_id IN (SELECT id FROM fin_forecast_run WHERE id LIKE 'preview-%');

DELETE FROM fin_forecast_run
WHERE id LIKE 'preview-%';

-- Optional: keep latest 20 persisted runs per user/scenario/year
DELETE fl FROM fin_forecast_line fl
INNER JOIN fin_forecast_run fr ON fr.id = fl.run_id
WHERE fr.id NOT LIKE 'preview-%'
  AND fr.id NOT IN (
    SELECT id FROM (
      SELECT fr2.id
      FROM fin_forecast_run fr2
      WHERE fr2.id NOT LIKE 'preview-%'
      ORDER BY fr2.created_at DESC
      LIMIT 20
    ) keep_runs
  );

DELETE FROM fin_forecast_run
WHERE id NOT LIKE 'preview-%'
  AND id NOT IN (
    SELECT id FROM (
      SELECT fr.id
      FROM fin_forecast_run fr
      WHERE fr.id NOT LIKE 'preview-%'
      ORDER BY fr.created_at DESC
      LIMIT 20
    ) keep_runs
  );

COMMIT;
