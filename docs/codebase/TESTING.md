# Testing Patterns

## Core Sections (Required)

### 1) Test Stack and Commands

- Primary test framework: Spring Boot Test (via `spring-boot-starter-test`) `[TODO]` confirm JUnit version via dependency tree if needed.
- Assertion/mocking tools: `[TODO]` not inferable (no test files detected).
- Commands:

```bash
# run all tests
mvn test

# run unit tests (same in standard Maven unless profiles exist)
mvn -Dtest='*Test' test

# run integration/e2e tests
[# TODO: no integration test setup detected]

# run coverage
[# TODO: no coverage tool/config detected]
```

### 2) Test Layout

- Test file placement pattern: expected under `src/test/java/` (standard Maven).
- Naming convention: `[TODO]` no tests detected to infer.
- Setup files and where they run: `[TODO]` no test setup detected.

### 3) Test Scope Matrix

| Scope | Covered? | Typical target | Notes |
|-------|----------|----------------|-------|
| Unit | No (not detected) | Domain/Application services | `spring-boot-starter-test` present but repo scan found no test files |
| Integration | No (not detected) | Web + DB boundaries | No CI pipeline detected; no test resources/config found |
| E2E | No (not detected) | UI flows | UI appears server-rendered + jQuery; no E2E tooling detected |

### 4) Mocking and Isolation Strategy

- Main mocking approach: `[TODO]` (no tests).
- Isolation guarantees: `[TODO]`.
- Common failure mode in tests: `[TODO]`.

### 5) Coverage and Quality Signals

- Coverage tool + threshold: `[TODO]` not configured.
- Current reported coverage: `[TODO]`.
- Known gaps/flaky areas:
  - No automated tests detected, increasing refactor risk.
  - Roadmap expresses an intent for “90% Unit Test coverage for Core Domain Logic”, but this is not reflected in the current repo state.

### 6) Evidence

- `pom.xml`
- `docs/codebase/.codebase-scan.txt`
- `docs/tech/architecture/milestones.md`

