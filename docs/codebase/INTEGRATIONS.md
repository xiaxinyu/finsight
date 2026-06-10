# External Integrations

## Core Sections (Required)

### 1) Integration Inventory

| System | Type (API/DB/Queue/etc) | Purpose | Auth model | Criticality | Evidence |
|--------|---------------------------|---------|------------|-------------|----------|
| MySQL | DB | Primary persistence for transactions, users, categories, etc. | DB user/password via env vars | High | `pom.xml`, `src/main/resources/application.yml`, `src/main/resources/mapper/TransactionMapper.xml` |
| Spring Security (form login) | Auth framework | Authenticate users and protect routes | DB-backed `UserDetailsService` + BCrypt | High | `src/main/java/com/finsight/web/config/SecurityConfig.java`, `src/main/java/com/finsight/web/security/DbUserDetailsService.java` |
| Actuator endpoints | Observability endpoints | Operational endpoints (health/etc.) | `permitAll` in security config | Medium | `pom.xml`, `src/main/java/com/finsight/web/config/SecurityConfig.java` |
| PDF parsing (PDFBox) | Library | Parse PDF statements for import | N/A | Medium | `pom.xml` |
| Excel parsing (EasyExcel) | Library | Parse/export tabular statement data | N/A | Medium | `pom.xml` |
| Local indexing/search (Lucene) | Library | Index/query text locally | N/A | Low/Med | `pom.xml` |

### 2) Data Stores

| Store | Role | Access layer | Key risk | Evidence |
|-------|------|--------------|----------|----------|
| MySQL | System of record | MyBatis-Plus mappers + MyBatis XML | Credentials default values in config; schema migration ownership unclear | `src/main/resources/application.yml`, `src/main/resources/mapper/*.xml` |

### 3) Secrets and Credentials Handling

- Credential sources: environment variables with defaults in `application.yml`.
- Hardcoding checks: DB password default is present (`SPRING_DATASOURCE_PASSWORD:123456`) and crypto signing key default is `change-me-in-env`. This indicates local-dev defaults are embedded and must be overridden in production.
- Rotation or lifecycle notes: `[TODO]` No explicit rotation process documented in repo.

Evidence:
- `src/main/resources/application.yml`
- `docs/tech/architecture/technical.md`

### 4) Reliability and Failure Behavior

- Retry/backoff behavior: none detected in reviewed files. `[TODO]` check for HTTP clients / retries if any external APIs exist.
- Timeout policy:
  - DB connection pool has Hikari timeouts (`connection-timeout`, etc.) in `application.yml`.
  - HTTP client timeouts: `[TODO]` not found (no external HTTP calls reviewed).
- Circuit-breaker or fallback behavior: none detected.

Evidence:
- `src/main/resources/application.yml`

### 5) Observability for Integrations

- Logging around external calls: DB access uses MyBatis; SQL logging is present but commented out (`log-impl` commented). No explicit integration logging wrappers found.
- Metrics/tracing coverage: Actuator dependency exists; scan did not find monitoring config. `[TODO]` determine which actuator endpoints are enabled/exposed.
- Missing visibility gaps:
  - No CI/CD pipeline detected by scan.
  - No centralized structured logging config found in reviewed files. `[TODO]`

Evidence:
- `pom.xml`
- `src/main/resources/application.yml`
- `docs/codebase/.codebase-scan.txt`

### 6) Evidence

- `pom.xml`
- `src/main/resources/application.yml`
- `src/main/java/com/finsight/web/config/SecurityConfig.java`
- `src/main/java/com/finsight/web/security/DbUserDetailsService.java`
- `src/main/resources/mapper/TransactionMapper.xml`
- `docs/codebase/.codebase-scan.txt`

