# Next Development Prompt (Structure + Terminology + Jasypt Removal)

This document is a reusable prompt template for the next iteration, so FinSight can keep improving in architecture, naming, domain language, and frontend-backend consistency.

## How to Use

- Before each new iteration, copy the "Reusable Prompt (Full Version)" and send it to your AI/dev assistant.
- For smaller changes, use the "Short Prompt" and add the exact scope.
- Require executable output: code changes + verification + updated docs.

## Reusable Prompt (Full Version)

```text
You are my refactoring and financial-domain standardization assistant. Based on the current project (Spring Boot + jQuery EasyUI + ECharts), run one practical modernization cycle and directly update code.

Priority goals:
1) Optimize code structure, directory layout, file naming, and file placement using industry standards.
2) Standardize financial terminology across backend + frontend + API + UI copy.
3) Improve frontend-backend collaboration (API contracts, error handling, compatibility, maintainability).
4) Evaluate and remove Jasypt and related config with a secure replacement plan.

Execution requirements:
- Backend (Spring Boot):
  - Organize by domain layers: domain / application / infrastructure / interfaces (or equivalent).
  - Keep controllers thin; move business logic to service/domain layers.
  - Standardize response shape around code/message/data with backward-compatible transition.
  - Standardize exception handling, logging fields, validation, and error code semantics.
  - Remove duplicated logic and reduce controller-level helper methods.
  - Remove Jasypt dependencies/config references; use environment variables + secret management (KMS/Vault/container secrets).
  - Provide migration checklist: impacted files, keys, rollback points, verification steps.

- Frontend (EasyUI + JavaScript):
  - Organize templates/scripts/styles by module; avoid large inline scripts.
  - Normalize naming for pages/functions/variables/CSS classes.
  - Standardize API response parsing and toast messaging style (success/fail/error).
  - Standardize financial wording: income/expense/balance/bookkeeping date/counterparty/category, etc.
  - Improve loading/empty/error/retry/batch feedback states.

- Financial terminology baseline:
  - Income
  - Expense
  - Balance
  - Bookkeeping Date
  - Transaction DateTime
  - Counterparty
  - Transaction Category
  - Statement Import
  - Reconciliation (if applicable)
  - Keep naming consistent across DB fields, DTOs, APIs, and UI labels.

Delivery format:
1) Provide a phased plan first, then implement directly.
2) For each phase: changed files, rationale, compatibility impact.
3) After each phase: validation output (lint/build/key UI checks).
4) Final migration summary: directory changes, naming conventions, terminology mapping, Jasypt removal notes.

Constraints:
- Avoid unrelated refactors.
- Avoid breaking changes.
- Keep backward compatibility first, then deprecate legacy fields gradually.
- Optimize for readability, maintainability, and extensibility.
```

## Short Prompt (Fast Iteration)

```text
Continue with structural modernization: unify directory and naming standards, financial terminology, and frontend-backend response semantics; also progress Jasypt removal and config migration. Implement directly and provide change list + compatibility notes + verification results.
```

## Jasypt Removal Add-on Prompt

```text
Run a dedicated Jasypt removal pass:
1) Remove Jasypt dependency from pom/gradle;
2) Remove jasypt.encryptor and related config from application*.yml;
3) Replace ENC(...) usage paths;
4) Switch to env vars or secret manager injection;
5) Provide migration + rollback steps;
6) Verify startup, datasource connectivity, and signing/encryption flows.
```

## Suggested Baseline Standards

- Backend directory baseline:
  - `com.finsight.domain`: entities, value objects, domain services, repository interfaces
  - `com.finsight.application`: use cases, DTOs, assemblers
  - `com.finsight.infrastructure`: persistence, external adapters, config implementations
  - `com.finsight.interfaces`: REST controllers, request/response models
- Frontend directory baseline:
  - `templates/<module>/...`
  - `static/scripts/<module>/...`
  - `static/css/<module>/...`
- Naming baseline:
  - Avoid ambiguous abbreviations.
  - Keep page names aligned with menu/business language.
  - Keep API path names, DTO fields, and UI labels semantically consistent.

## Purpose

Make the next iteration start from execution, not from alignment.

## Chinese Version

- 中文版本: `docs/NEXT_DEV_PROMPT.zh-CN.md`
