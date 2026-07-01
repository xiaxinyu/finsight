# FinSight security guide

| | |
| :--- | :--- |
| **Language** | English · [简体中文](SECURITY.zh-cn.md) |

Operational and architectural security controls for self-hosted FinSight.

---

## 1. Authentication & login

| Control | Implementation |
| :--- | :--- |
| Password hashing | BCrypt via Spring Security `PasswordEncoder` |
| Session auth | HTTP session cookie (`JSESSIONID`), 30-minute idle timeout |
| Brute-force throttle | `LoginRateLimitFilter` — 8 failures / 15 min per client IP (configurable) |
| User enumeration | Generic login error message; `hideUserNotFoundExceptions` enabled |
| Logout | Invalidates session; clears `JSESSIONID` and `XSRF-TOKEN` |

**API:** `GET /api/v1/auth/me` returns `{ authenticated, username, roles, admin }`.

---

## 2. Authorization

| Path | Requirement |
| :--- | :--- |
| `/api/v1/users/**` | `ROLE_ADMIN` |
| `/api/v1/maintenance/**` | `ROLE_ADMIN` |
| Card / category / rule admin mutations | `ROLE_ADMIN` |
| Other `/api/v1/**` | Authenticated user |

Roles are stored in `fs_role` / `fs_user_role`. Migration **V50** seeds `ADMIN` and `USER` and grants `ADMIN` to existing users on upgrade.

Frontend hides **Admin** navigation and routes unless `admin: true` from `/auth/me`.

---

## 3. CSRF (production)

When `finsight.security.csrf-enabled=true` (default in `application-prod.yml`):

- Cookie `XSRF-TOKEN` (readable by SPA)
- Mutating requests send header `X-XSRF-TOKEN`
- Bootstrap: `GET /api/v1/auth/csrf`

Development profile keeps CSRF disabled for simpler local workflows.

---

## 4. Data protection

| Data | Policy |
| :--- | :--- |
| Card numbers (PAN) | Masked in logs (`****1234`); non-admin API list returns masked `cardNo` |
| Password hashes | Never returned from `GET /api/v1/users` |
| Sign key | Never logged in full — startup logs `(configured, length=N)` |
| CVV | Not collected or stored |

Set a strong `ACCOUNT_DES_SIGN_KEY` in production (`ProdStartupValidator` rejects defaults).

---

## 5. Transport & headers (production)

- Session cookie: `Secure`, `HttpOnly`, `SameSite=Strict`
- HSTS enabled on prod profile
- `X-Frame-Options: SAMEORIGIN`
- `/actuator/health` requires auth unless explicitly public
- `/encrypt/**` denied in prod

---

## 6. Configuration

| Property | Dev default | Prod |
| :--- | :--- | :--- |
| `finsight.security.csrf-enabled` | `false` | `true` |
| `finsight.security.actuator-public` | `true` | `false` |
| `finsight.security.login-max-attempts` | `8` | `8` |
| `finsight.security.login-lockout-seconds` | `900` | `900` |

Env vars: `SPRING_DATASOURCE_*`, `ACCOUNT_DES_SIGN_KEY`.

---

## 7. Verification checklist

- [ ] Prod secrets not using dev defaults
- [ ] Non-admin user cannot open `/admin/*` or call `/api/v1/users`
- [ ] Login lockout after repeated failures
- [ ] API returns JSON 401/403 (not HTML) for `/api/**`
- [ ] Card list for non-admin shows masked numbers only
- [ ] CSRF-protected POST/PUT/DELETE succeed from SPA in prod

See also [FEATURE_FLAGS.md](FEATURE_FLAGS.md) · [personal-finance-reporting-guide.md](finance/personal-finance-reporting-guide.md)
