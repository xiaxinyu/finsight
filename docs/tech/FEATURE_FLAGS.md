# Feature flags

Runtime toggles under `finsight.*` in `application.yml`. The SPA reads effective flags from `GET /api/v1/features` (authenticated).

| Property | Default | Effect when `false` |
|----------|---------|---------------------|
| `finsight.planning.persist` | `false` | Planning uses in-memory store only (no MySQL persistence). |
| `finsight.advisor.enabled` | `true` | Advisor recommendations API returns **404**; Dashboard hides advisor strip. |
| `finsight.advisor.local-ai-enabled` | `true` | `POST /api/v1/advisor/ask` returns **404**. |
| `finsight.profile.enabled` | `true` | Profile analytics API returns **404**; Profile menu hidden. |
| `finsight.forecast.enabled` | `true` | Forecast/trends/scenarios/cash-risk APIs return **404**; related reports hidden. |
| `finsight.merchant-mining.enabled` | `true` | Merchant mining APIs return **404**. |
| `finsight.metrics.reconcile-gate` | `false` | When `true`, advisor layers fall back to report SQL if metrics reconciliation fails. |
| `finsight.security.csrf-enabled` | `false` (dev) | Enable CSRF protection (recommended in prod). |
| `finsight.security.actuator-public` | `true` (dev) | When `false`, `/actuator/health` requires authentication. |

## Production

Use `application-prod.yml` plus environment variables for secrets. Override flags via env, e.g. `FINSIGHT_ADVISOR_ENABLED=false`.

## Frontend

`useFeatureFlags()` loads `/api/v1/features` once per session. Menu items and dashboard widgets respect flags; disabled modules are not rendered (no error spam).
