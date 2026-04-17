# Coding Conventions

## Core Sections (Required)

### 1) Naming Rules

| Item | Rule | Example | Evidence |
|------|------|---------|----------|
| Files | Java uses `PascalCase.java` | `TransactionServiceImpl.java` | `src/main/java/com/finsight/application/transaction/impl/TransactionServiceImpl.java` |
| Functions/methods | lowerCamelCase for methods | `getTransactions(...)`, `updateTransaction(...)` | `src/main/java/com/finsight/web/restful/transaction/TransactionController.java` |
| Types/interfaces | `PascalCase`, interfaces prefixed with `I` in application layer | `ITransactionService`, `ITransactionListingService` | `src/main/java/com/finsight/web/restful/transaction/TransactionController.java` |
| Constants/env vars | Env vars are `SCREAMING_SNAKE_CASE` | `SPRING_DATASOURCE_URL`, `ACCOUNT_DES_SIGN_KEY` | `src/main/resources/application.yml`, `docs/tech/architecture/technical.md` |

### 2) Formatting and Linting

- Formatter: `[TODO]` No formatter config detected in scan output.
- Linter: Checkstyle is configured but only enforces `UnusedImports` currently.
- Most relevant enforced rules: `UnusedImports`
- Run commands: `mvn checkstyle:check` (plugin present; lifecycle binding not shown)

Evidence:
- `checkstyle.xml`
- `pom.xml`

### 3) Import and Module Conventions

- Import grouping/order: `[TODO]` not enforced by config (only unused imports are checked).
- Alias vs relative import policy: N/A (Java packages under `com.finsight.*`).
- Public exports/barrel policy: N/A (Java).

### 4) Error and Logging Conventions

- Error strategy by layer:
  - Web layer wraps operations via `ControllerHelper.runCommon/runCollection` pattern. `[TODO]` confirm how exceptions are mapped (read `ControllerHelper`).
  - Application services commonly catch `Exception` and throw `AppServiceException`, or log and continue for per-row import processing.
- Logging style and required context fields:
  - Uses SLF4J `LoggerFactory` and structured placeholders.
  - Example includes action labels and relevant params (e.g., `page={}`; login failures include `user`, `code`, `msg`).
- Sensitive-data redaction rules: `[TODO]` No explicit redaction policy found in reviewed files.

Evidence:
- `src/main/java/com/finsight/application/transaction/impl/TransactionServiceImpl.java`
- `src/main/java/com/finsight/core/SecurityConfig.java`

### 5) Testing Conventions

- Test file naming/location rule: tests should live under `src/test/java/` (standard Maven layout).
- Mocking strategy norm: `[TODO]` no tests detected to infer mocking style.
- Coverage expectation: Roadmap sets a “Quality Gate: 90% Unit Test coverage for Core Domain Logic” (intent). Reality: test files not detected. Marked as divergence in `CONCERNS.md`.

Evidence:
- `pom.xml`
- `docs/tech/architecture/milestones.md`
- `docs/codebase/.codebase-scan.txt`

### 6) Evidence

- `checkstyle.xml`
- `pom.xml`
- `src/main/java/com/finsight/web/restful/transaction/TransactionController.java`
- `src/main/java/com/finsight/application/transaction/impl/TransactionServiceImpl.java`
- `src/main/resources/application.yml`
- `docs/tech/architecture/milestones.md`

