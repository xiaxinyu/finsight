# FinSight

> **Personal finance intelligence you host yourself** — classify transactions, read clear KPIs, and review reports without sending data to a third-party cloud.

[![License: Apache-2.0](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

| | |
| :--- | :--- |
| **Language** | English · [简体中文](README.zh-CN.md) |
| **Docs** | [Documentation hub](docs/_index.md) |

---

## About

FinSight imports bank and card statements, **classifies each transaction**, and builds **Dashboard**, **Profile**, and **Reports** from one shared ruleset.

From **v2.0.2**, all main numbers use the same **finance semantic layer**. That means Real income on the Dashboard matches the Cashflow report when you use the same date range.

**v2.0.3** adds **Income / Consumption / Debt** year-over-year trend reports and a reorganized Reports sidebar — see [version highlights](docs/user/concepts/version-highlights.md).

**Who is it for?** Individuals and small teams who want professional-grade personal finance views on **their own server**.

---

## Key terms (30 seconds)

| Term | Plain meaning |
| :--- | :--- |
| **Real income** | Money you actually earned (salary, etc.). Not refunds or money moved between your own accounts. |
| **Consumption** | Day-to-day living spend. Not transfers, loan repayments, or investment purchases. |
| **Net cashflow** | Real income minus Consumption for the period you select. |
| **Reporting Classification** | The analytic bucket for a transaction (e.g. Dining, Housing, Tax). |
| **Profile** | A 12-month financial health snapshot. Click **Refresh** to update it. |

Full definitions: [Data semantics](docs/user/concepts/data-semantics.md)

---

## Features

| Area | What you get |
| :--- | :--- |
| **Import & classify** | Statement import, rules, categories, semantic tags |
| **Dashboard** | Real income · Consumption · Net · expense breakdown · drill-down |
| **Profile** | 10-dimension score, user type, evidence per dimension |
| **Reports** | Monthly overview, YoY trends (income / consumption / debt), budget, period comparison, forecast, merchants, tax, transfers |
| **Privacy** | Self-hosted; configure secrets with environment variables |

---

## Prerequisites

| Tool | Version |
| :--- | :--- |
| Java | 21+ |
| Maven | 3.9+ |
| MySQL | 8.x |
| Node.js (frontend dev only) | 20+ |

Set `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, and `ACCOUNT_DES_SIGN_KEY` before production use.

---

## Quick start

```bash
# Backend (login UI)
mvn spring-boot:run
# → http://localhost:8080/app/login

# Frontend dev server (optional, hot reload)
cd frontend && npm run dev
# → http://localhost:5173/app/

# Production build (backend + SPA)
mvn clean package
```

First-time setup: [Getting started](docs/user/concepts/getting-started.md) · [Local development](docs/user/setup/local-development.md)

---

## Documentation

| I want to… | Read |
| :--- | :--- |
| Start in 5 minutes | [Getting started](docs/user/concepts/getting-started.md) · [中文](docs/user/concepts/getting-started.zh-cn.md) |
| Understand the numbers | [Data semantics](docs/user/concepts/data-semantics.md) · [中文](docs/user/concepts/data-semantics.zh-cn.md) |
| KPI by scenario | [Semantic scenarios](docs/user/concepts/semantic-scenarios.md) · [中文](docs/user/concepts/semantic-scenarios.zh-cn.md) |
| Use Dashboard & Profile | [Dashboard & Profile guide](docs/user/concepts/dashboard-profile.md) · [中文](docs/user/concepts/dashboard-profile.zh-cn.md) |
| Pick a report | [Reports catalog](docs/user/concepts/reports-catalog.md) · [中文](docs/user/concepts/reports-catalog.zh-cn.md) |
| See v2.0.x changes | [Version highlights](docs/user/concepts/version-highlights.md) · [中文](docs/user/concepts/version-highlights.zh-cn.md) |
| Develop or deploy | [Technical architecture](docs/tech/architecture/technical.md) |
| Step-by-step tasks | [Tasks index](docs/user/tasks/README.md) · [任务](docs/user/tasks/README.zh-cn.md) |

---

## Tech stack

Spring Boot 3 · Java 21 · MyBatis-Plus · MySQL · React 19 · Ant Design · Vite

---

## License

[Apache License 2.0](LICENSE)
