# Getting started in 5 minutes

| | |
| :--- | :--- |
| **Language** | English · [简体中文](getting-started.zh-cn.md) |

> Markers: [_style-guide.md](_style-guide.md)

---

## 1. Run locally

```bash
# Backend
mvn spring-boot:run          # → http://localhost:8080/app/login

# Frontend hot reload (optional)
cd frontend && npm run dev   # → http://localhost:5173/app/
```

Environment: `SPRING_DATASOURCE_*` · `ACCOUNT_DES_SIGN_KEY` — see [local-development.md](../setup/local-development.md).

Production build: `mvn clean package` (bundles the SPA into `src/main/resources/static/app/`).

---

## 2. Recommended reading order

| Step | Document | Why |
| :---: | :--- | :--- |
| 1 | [data-semantics.md](data-semantics.md) | Learn Real income / Consumption before trusting KPIs |
| 2 | [dashboard-profile.md](dashboard-profile.md) | Read Dashboard & Profile with correct expectations |
| 3 | [reports-catalog.md](reports-catalog.md) | Pick the right report for each question |
| 4 | [version-highlights.md](version-highlights.md) | v2.0.x semantic layer and report changes |

---

## 3. First-session checklist

| # | Action | Outcome |
| :---: | :--- | :--- |
| 1 | Import statements | Ledger has data |
| 2 | Clear unclassified rows in **Transactions** | Higher data trust |
| 3 | Review **Admin → Categories** semantic tags | Reporting Classification aligns with intent |
| 4 | Set Dashboard **Period** and check **Net** | KPIs match your window |
| 5 | **Profile → Generate / Refresh** | 12-month persona snapshot exists |

---

## 4. Where to go next

| Goal | Document |
| :--- | :--- |
| Understand numbers | [data-semantics.md](data-semantics.md) |
| Choose a report | [reports-catalog.md](reports-catalog.md) |
| Write rules | [rules-guide.md](../../tech/contributing/rules-guide.md) |
| Extend the app | [technical.md](../../tech/architecture/technical.md) |
