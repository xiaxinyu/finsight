# Getting started

| | |
| :--- | :--- |
| **Language** | English · [简体中文](getting-started.zh-cn.md) |

> Style: [_style-guide.md](_style-guide.md)

This guide takes about **5 minutes**. You will run FinSight locally and know which document to read next.

---

## 1. Run the application

```bash
# Terminal 1 — backend (serves login + API + built UI)
mvn spring-boot:run
# Open http://localhost:8080/app/login

# Terminal 2 — frontend dev only (optional, hot reload)
cd frontend && npm run dev
# Open http://localhost:5173/app/
```

**Environment variables** (required for real databases):

| Variable | Purpose |
| :--- | :--- |
| `SPRING_DATASOURCE_URL` | MySQL JDBC URL |
| `SPRING_DATASOURCE_USERNAME` | Database user |
| `SPRING_DATASOURCE_PASSWORD` | Database password |
| `ACCOUNT_DES_SIGN_KEY` | Encryption key for sensitive fields |

Details: [local-development.md](../setup/local-development.md)

**Production build:**

```bash
mvn clean package
```

The SPA is copied to `src/main/resources/static/app/`.

---

## 2. Recommended reading order

| Step | Document | Goal |
| :---: | :--- | :--- |
| 1 | [data-semantics.md](data-semantics.md) | Understand Real income and Consumption |
| 1b | [semantic-scenarios.md](semantic-scenarios.md) | Quick KPI lookup by scenario |
| 2 | [dashboard-profile.md](dashboard-profile.md) | Read Dashboard and Profile correctly |
| 3 | [reports-catalog.md](reports-catalog.md) | Choose the right report |
| 4 | [Tasks index](../tasks/README.md) | Step-by-step operations |
| 5 | [version-highlights.md](version-highlights.md) | See what changed in v2.0.x |

---

## 3. First-session checklist

| # | Task | Guide |
| :---: | :--- | :--- |
| 1 | Import at least one statement | [import-bank-statement.md](../tasks/import-bank-statement.md) |
| 2 | Classify unclassified rows | [classify-unclassified-transactions.md](../tasks/classify-unclassified-transactions.md) |
| 3 | Check category semantics | [set-category-semantics.md](../tasks/set-category-semantics.md) |
| 4 | Set Dashboard Period; note Net | [dashboard-profile.md](dashboard-profile.md) |
| 5 | Profile → Generate or Refresh | [refresh-profile.md](../tasks/refresh-profile.md) |

---

## 4. Next steps

| Goal | Document |
| :--- | :--- |
| Understand KPIs | [data-semantics.md](data-semantics.md) |
| Pick a report | [reports-catalog.md](reports-catalog.md) |
| Write classification rules | [write-classification-rule.md](../tasks/write-classification-rule.md) |
| Drill down from reports | [drill-down-from-reports.md](../tasks/drill-down-from-reports.md) |
| Develop features | [technical.md](../../tech/architecture/technical.md) |
| Step-by-step tasks | [Tasks index](../tasks/README.md) |
