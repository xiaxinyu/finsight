# Architecture

## Core Sections (Required)

### 1) Architectural Style

- Primary style: Layered monolith (Web → Application → Domain → Infrastructure)
- Why this classification: Code is organized by packages `com.finsight.web`, `com.finsight.application`, `com.finsight.domain`, `com.finsight.infrastructure`, matching the layered description in the engineering docs.
- Primary constraints:
  - Local-first / self-hosted deployment (no cloud dependency stated) — see README positioning.
  - Relational DB-backed persistence (MySQL) with MyBatis XML mappings.
  - Server-rendered UI via Thymeleaf plus jQuery-based AJAX calls.

### 2) System Flow

```text
Browser (Thymeleaf page + JS) -> Spring MVC controller (@Controller/@RestController)
-> Application service/facade (use-case logic)
-> Domain port (e.g., TransactionRepository) / Infrastructure mapper
-> MyBatis XML -> MySQL -> results
-> JSON response (CommonResult/CollectionResult) or HTML template render
```

Concrete example (transactions):
- UI calls `/transaction/getTransactions` and other endpoints from `src/main/resources/static/scripts/transaction.js`.
- Controller delegates to listing/service interfaces in `src/main/java/com/finsight/web/restful/transaction/TransactionController.java`.
- Application service uses domain port `com.finsight.domain.port.TransactionRepository` (injected) as shown in `TransactionServiceImpl`.
- Infrastructure persistence uses MyBatis mapper interfaces and XML, e.g. `TransactionMapper.java` + `src/main/resources/mapper/TransactionMapper.xml`.

### 3) Layer/Module Responsibilities

| Layer or module | Owns | Must not own | Evidence |
|-----------------|------|--------------|----------|
| Web (`com.finsight.web.*`) | Routing, request binding, response shaping | SQL, persistence mapping | `src/main/java/com/finsight/web/restful/transaction/TransactionController.java` |
| Application (`com.finsight.application.*`) | Use cases (import/classify/report), orchestration, domain-to-DTO mapping where needed | HTTP specifics and template structure | `src/main/java/com/finsight/application/transaction/impl/TransactionServiceImpl.java` |
| Domain (`com.finsight.domain.*`) | Domain models + ports (e.g., `TransactionRepository`) | Framework glue code | `src/main/java/com/finsight/domain/port/TransactionRepository.java` `[TODO]` (file exists per scan; not expanded here) |
| Infrastructure (`com.finsight.infrastructure.*`) | DB mappers/adapters | UI concerns | `src/main/java/com/finsight/infrastructure/mapper/TransactionMapper.java` |
| Resources (MyBatis XML) | SQL and result mappings | Business rules | `src/main/resources/mapper/TransactionMapper.xml` |

### 4) Reused Patterns

| Pattern | Where found | Why it exists |
|---------|-------------|---------------|
| Facade (security principal access) | `src/main/java/com/finsight/application/authentication/AuthenticationFacade.java` | Avoids direct `SecurityContextHolder` access across app logic |
| Repository/Port abstraction | `com.finsight.domain.port.*` (e.g., `TransactionRepository`) | Separates application logic from persistence details |
| Mapper (MyBatis) | `src/main/java/com/finsight/infrastructure/mapper/*Mapper.java` + `src/main/resources/mapper/*.xml` | SQL mapping and query composition |
| Controller helper wrapper | `com.finsight.web.restful.common.ControllerHelper` (used by controllers) | Standardize error handling / response envelopes `[TODO]` confirm behavior by reading file |

### 5) Known Architectural Risks

- Large, vendored static assets (ECharts maps, EasyUI) increase repo size and can complicate frontend modernization. Evidence: scan “Top 10 largest files” and `src/main/resources/static/plugins/`.
- No CI/CD pipeline detected in repo scan, raising risk of regressions during refactors. Evidence: `docs/codebase/.codebase-scan.txt` (“No CI/CD pipelines detected.”).
- Test dependency exists but no tests detected in `src/test/java`, making refactors riskier. Evidence: `pom.xml`, `docs/codebase/.codebase-scan.txt`.

### 6) Evidence

- `src/main/java/com/finsight/FinsightApplication.java`
- `docs/tech/architecture/technical.md`
- `README.md`
- `src/main/java/com/finsight/web/restful/transaction/TransactionController.java`
- `src/main/java/com/finsight/application/transaction/impl/TransactionServiceImpl.java`
- `src/main/java/com/finsight/infrastructure/mapper/TransactionMapper.java`
- `src/main/resources/mapper/TransactionMapper.xml`
- `src/main/resources/static/scripts/transaction.js`
- `docs/codebase/.codebase-scan.txt`

