# Codebase Structure

## Core Sections (Required)

### 1) Top-Level Map

| Path | Purpose | Evidence |
|------|---------|----------|
| `pom.xml` | Maven manifest (dependencies/build) | `pom.xml` |
| `src/main/java/` | Application source (Spring Boot) | `docs/codebase/.codebase-scan.txt` |
| `src/main/resources/` | Config + MyBatis XML + static assets + Thymeleaf templates | `src/main/resources/application.yml`, `src/main/resources/mapper/`, `src/main/resources/static/`, `src/main/resources/templates/` |
| `src/test/java/` | Tests folder (no test files detected) | `docs/codebase/.codebase-scan.txt` |
| `docs/` | Product + engineering docs | `docs/_index.md` |
| `target/` | Build output (generated) | `docs/codebase/.codebase-scan.txt` |
| `.vscode/` / `.idea/` / `.settings/` | IDE configs | `docs/codebase/.codebase-scan.txt` |
| `db/` | DB-related folder (contents not detected by scan) | `docs/codebase/.codebase-scan.txt` |

### 2) Entry Points

- Main runtime entry: `src/main/java/com/finsight/FinsightApplication.java`
- Secondary entry points (worker/cli/jobs): NONE detected. `[TODO]` confirm whether any scheduled jobs exist (e.g., `@Scheduled`).
- How entry is selected (script/config): Spring Boot standard main class; packaged as `jar` via Maven. See build/run instructions in `docs/tech/architecture/technical.md`.

### 3) Module Boundaries

| Boundary | What belongs here | What must not be here |
|----------|-------------------|------------------------|
| `com.finsight.web` | Web controllers (Thymeleaf pages + JSON endpoints) | Direct SQL / persistence details (should go through application/domain/infra abstractions) |
| `com.finsight.application` | Use cases/services/facades/importers/reporting | HTTP concerns (request/response objects), template rendering |
| `com.finsight.domain` | Domain models and ports (`...domain.port.*`) | Spring MVC annotations; MyBatis mapper interfaces |
| `com.finsight.infrastructure` | MyBatis mapper interfaces + persistence adapters | UI/controller logic |
| `src/main/resources/mapper/` | MyBatis XML SQL mappings | Business rules (should stay in application/domain) |
| `src/main/resources/templates/` | Thymeleaf HTML UI | Backend business logic |
| `src/main/resources/static/` | JS/CSS/plugins | Server-side logic |

### 4) Naming and Organization Rules

- File naming pattern:
  - Java: `PascalCase.java` (e.g., `FinsightApplication.java`, `SecurityConfig.java`)
  - HTML templates: mixed (`index.html`, `Endowment.html`, `UnEmployment.html`) `[TODO]` normalize or document rationale.
  - JS: lower camel or lower case (e.g., `transaction.js`)
- Directory organization pattern: primarily layered (web / application / domain / infrastructure) as documented in repo technical docs.
- Import aliasing or path conventions: Java package names under `com.finsight.*`; no TS/JS path aliases detected.

### 5) Evidence

- `docs/codebase/.codebase-scan.txt` (directory tree + metrics)
- `src/main/java/com/finsight/FinsightApplication.java`
- `docs/tech/architecture/technical.md`
- `src/main/resources/application.yml`
- `src/main/resources/templates/`

