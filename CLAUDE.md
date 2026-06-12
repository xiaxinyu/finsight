# CLAUDE.md — FinSight

## Cursor rules (authoritative)

Project rules for Cursor live under **`.cursor/rules/`** (`.mdc` files with YAML frontmatter). They replace any legacy `.claude/rules/*.md` paths.

| Topic | Cursor rule file |
|--------|-------------------|
| Architecture (layering, ports, SQL location) | [`.cursor/rules/architecture.mdc`](.cursor/rules/architecture.mdc) |
| Frontend (React SPA, Ant Design, Vite) | [`.cursor/rules/frontend.mdc`](.cursor/rules/frontend.mdc) |
| **Data persistence (CRITICAL)** — DB, imports, integrity | [`.cursor/rules/message-persistence.mdc`](.cursor/rules/message-persistence.mdc) |
| Main process (Boot entry, security, lifecycle) | [`.cursor/rules/main-process.mdc`](.cursor/rules/main-process.mdc) |
| Git conventions | [`.cursor/rules/git-conventions.mdc`](.cursor/rules/git-conventions.mdc) |

## Quick Reference

- Product: FinSight (personal finance intelligence, local-first).
- Stack: Java 21 + Spring Boot 3.5.x + Maven + MyBatis-Plus + MySQL 8.x.
- UI: React 19 + Ant Design SPA (`frontend/` → `/app/`). See [docs/tech/REPORT_UI.md](docs/tech/REPORT_UI.md).
- Main entry: `src/main/java/com/finsight/FinsightApplication.java`.
- Key config: `src/main/resources/application.yml`.

## Core Directories

- `src/main/java/com/finsight/application/`: use cases, services, importers.
- `src/main/java/com/finsight/domain/`: domain models and ports.
- `src/main/java/com/finsight/infrastructure/`: MyBatis mappers and persistence adapters.
- `src/main/java/com/finsight/web/`: MVC/REST controllers.
- `src/main/resources/mapper/`: MyBatis XML SQL mappings.
- `frontend/`: React SPA 源码（Vite + TypeScript + Ant Design）。
- `src/main/resources/static/app/`: SPA 构建产物（`npm run build` 输出）。
- `src/main/resources/templates/`: 仅保留错误页等兜底模板。

## Commands

```bash
# 本地启动（后端）
mvn spring-boot:run
# → http://localhost:8080/app/login

# 开发热更新（另开终端）
cd frontend && npm run dev
# → http://localhost:5173/app/

# 生产打包（含前端构建）
mvn clean package

# 快速打包（跳过测试）
mvn clean package -DskipTests

# 前端单独构建 / 测试
cd frontend && npm run build
cd frontend && npm test

# 代码检查
mvn checkstyle:check
mvn test
```

完整 UI 启动说明见 [docs/tech/REPORT_UI.md](docs/tech/REPORT_UI.md)。

## Feature flags

See [docs/tech/FEATURE_FLAGS.md](docs/tech/FEATURE_FLAGS.md). Runtime toggles live under `finsight.*` in `application.yml`; the SPA reads `GET /api/v1/features`.

## Environment Variables

Set these for local/prod instead of hardcoding secrets:

- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `ACCOUNT_DES_SIGN_KEY`

## Working Rules

- Keep layering clean: `web -> application -> domain`; infrastructure handles DB details.
- SQL changes should stay in `src/main/resources/mapper/*.xml` with matching mapper interfaces.
- Avoid committing real credentials or real financial data exports.
- Prefer focused changes; update docs when behavior/architecture changes.

## Verification

Before claiming done:

- **Rules**: Changes align with `.cursor/rules/architecture.mdc`, `.cursor/rules/frontend.mdc` (if UI), `.cursor/rules/message-persistence.mdc` for any DB/import/ledger behavior, `.cursor/rules/main-process.mdc` for entry/security/runtime behavior, and `.cursor/rules/git-conventions.mdc` for commits/PRs.
- Build passes: `mvn clean package` (or explain why not run).
- Checkstyle passes for touched Java files: `mvn checkstyle:check`.
- For feature changes, run app via `mvn spring-boot:run` and verify affected pages/APIs.
