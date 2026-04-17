# Codebase Concerns

## Core Sections (Required)

### 1) Top Risks (Prioritized)

| Severity | Concern | Evidence | Impact | Suggested action |
|----------|---------|----------|--------|------------------|
| high | Tests effectively absent (no test files detected) | `docs/codebase/.codebase-scan.txt`, `pom.xml` | Refactors are risky; regressions likely | Add a thin test harness first (service-level unit tests for core logic, then controller slice tests) |
| high | Default credentials in config and security posture gaps (CSRF disabled; broad `permitAll`) | `src/main/resources/application.yml`, `src/main/java/com/finsight/core/SecurityConfig.java` | Production misconfig risk; increases attack surface | Remove dangerous defaults for prod via profiles; review CSRF and public endpoints; restrict `/encrypt/**` |
| med | Large vendored front-end plugins & assets in repo | `docs/codebase/.codebase-scan.txt`, `src/main/resources/static/plugins/` | Slows tooling, complicates modernization, makes security patching harder | Track third-party assets with versions; consider moving to package-managed frontend build over time |
| med | No CI/CD pipelines detected | `docs/codebase/.codebase-scan.txt` | Broken builds can land unnoticed | Add minimal GitHub Actions for build + tests + checkstyle |
| med | Intent vs reality: roadmap suggests SQLite/H2 for dev, but current config defaults to MySQL | `docs/tech/architecture/milestones.md`, `src/main/resources/application.yml` | Local dev friction; harder to run tests/CI without MySQL | Add dev profile with H2 (or Testcontainers for MySQL) and document it |
| med | Fastjson 1.2.60 is old | `pom.xml` | Known ecosystem risk area; potential security issues depending on usage | Evaluate upgrading or replacing with Jackson; audit usage points |
| low/med | In-memory caching without explicit invalidation (home summary TTL cache) | `src/main/java/com/finsight/application/transaction/impl/TransactionServiceImpl.java` | Multi-instance consistency issues; stale UI data | Scope cache to request/user or add explicit invalidation; document intent |

### 2) Technical Debt

| Debt item | Why it exists | Where | Risk if ignored | Suggested fix |
|-----------|---------------|-------|-----------------|---------------|
| Mixed template naming (`Endowment.html` vs `index.html`) | Historical evolution | `src/main/resources/templates/` | Inconsistent conventions slow navigation | Decide naming rule and gradually normalize |
| Error handling split between wrapper helpers and ad-hoc try/catch | Incremental growth | Controllers + services | Harder to reason about failure modes | Standardize exception mapping at web boundary; reduce broad catch blocks |
| Repo contains generated-ish / huge source maps | Vendored JS libs | `src/main/resources/static/plugins/echarts-6.0.0/*.map` | Repo bloat | Keep only required assets; consider CDN or build pipeline later |

### 3) Security Concerns

| Risk | OWASP category (if applicable) | Evidence | Current mitigation | Gap |
|------|--------------------------------|----------|--------------------|-----|
| Default DB password in config (`123456`) | A02 (Cryptographic Failures) / A05 (Security Misconfiguration) | `src/main/resources/application.yml` | Env overrides supported | Default is unsafe if deployed as-is |
| `ACCOUNT_DES_SIGN_KEY` default placeholder | A02 / A05 | `src/main/resources/application.yml` | Env override supported | Need production enforcement (fail-fast if default) |
| CSRF disabled | A01 (Broken Access Control) / A05 | `src/main/java/com/finsight/core/SecurityConfig.java` | Authentication required for most routes | Risk for form-based endpoints; needs review vs UI pattern |
| Broad public matchers incl. `/encrypt/**` and `/actuator/**` | A01 / A05 | `src/main/java/com/finsight/core/SecurityConfig.java` | Auth required for others | Potentially exposes sensitive functionality/ops endpoints |

### 4) Performance and Scaling Concerns

| Concern | Evidence | Current symptom | Scaling risk | Suggested improvement |
|---------|----------|-----------------|-------------|-----------------------|
| MyBatis queries include `select *` and joins; potential for large payloads | `src/main/resources/mapper/TransactionMapper.xml` | Larger-than-needed result sets | Slower pages as data grows | Select explicit columns; paginate consistently; add indexes and query profiling |
| Very large static assets included | `docs/codebase/.codebase-scan.txt` | Slower initial load / build tooling | Poor client performance on low-end devices | Prune and compress assets; adopt bundling strategy |

### 5) Fragile/High-Churn Areas

| Area | Why fragile | Churn signal | Safe change strategy |
|------|-------------|-------------|----------------------|
| Transaction bill UI | High feature activity | `docs/codebase/.codebase-scan.txt` (high-churn list) | Change behind feature flags; add regression tests around endpoints used by page |
| Consume rules & categories admin UI | Business-critical classification rules | `docs/codebase/.codebase-scan.txt` (high-churn list) | Add unit tests for rule evaluation and reload behavior first |
| `TransactionMapper.xml` | Central query logic for reporting | `docs/codebase/.codebase-scan.txt` (high-churn list) | Add integration tests against a seeded DB; refactor incrementally |

### 6) `[ASK USER]` Questions

1. [ASK USER] 你希望“改造”优先落在哪条线上：**UI 现代化**（Phase 1）、**数据 ETL/导入链路**（Phase 2）、还是**规则/自动化引擎**（Phase 3）？（会影响模块化与测试先行的范围）
2. [ASK USER] 生产部署目标是什么：单机/家庭 NAS/公司内网？是否需要容器化（Docker Compose）？
3. [ASK USER] `/encrypt/**` 这类端点是否必须保留为公网可访问？还是仅用于本地初始化/管理员？
4. [ASK USER] 数据库迁移脚本的“事实来源”在哪里？（仓库里存在 `db/` 目录，但扫描未发现具体脚本文件）

### 7) Evidence

- `docs/codebase/.codebase-scan.txt` (code metrics, high-churn, CI/CD detection)
- `pom.xml`
- `src/main/resources/application.yml`
- `src/main/java/com/finsight/core/SecurityConfig.java`
- `src/main/java/com/finsight/application/transaction/impl/TransactionServiceImpl.java`
- `src/main/resources/mapper/TransactionMapper.xml`
- `src/main/resources/static/plugins/`

