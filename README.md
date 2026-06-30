# FinSight

**Personal finance intelligence — local-first, insight-driven.**

| | |
| :--- | :--- |
| **Language** | English · [简体中文](README.zh-CN.md) |

## Positioning

FinSight turns bank and card activity into **classified transactions, trends, and reports** you control end-to-end. From **v2.0.2**, a unified **finance semantic layer** aligns Dashboard, Profile, and all reports.

---

## Documentation (bilingual)

| Topic | English | 简体中文 |
| :--- | :--- | :--- |
| <span style="color:#2563eb">**Quick start**</span> | [getting-started.md](docs/user/concepts/getting-started.md) | [getting-started.zh-cn.md](docs/user/concepts/getting-started.zh-cn.md) |
| <span style="color:#2563eb">**Understand numbers**</span> | [data-semantics.md](docs/user/concepts/data-semantics.md) | [data-semantics.zh-cn.md](docs/user/concepts/data-semantics.zh-cn.md) |
| <span style="color:#2563eb">**Dashboard & Profile**</span> | [dashboard-profile.md](docs/user/concepts/dashboard-profile.md) | [dashboard-profile.zh-cn.md](docs/user/concepts/dashboard-profile.zh-cn.md) |
| <span style="color:#2563eb">**Reports**</span> | [reports-catalog.md](docs/user/concepts/reports-catalog.md) | [reports-catalog.zh-cn.md](docs/user/concepts/reports-catalog.zh-cn.md) |
| **All docs** | [Docs hub](docs/_index.md) | [文档中心](docs/user/concepts/overview.zh-cn.md) |
| **Engineering** | [Technical](docs/tech/architecture/technical.md) | [technical.zh-cn.md](docs/tech/architecture/technical.zh-cn.md) |

---

## What it delivers

- **Semantic analytics** — Real income · Consumption · Reporting Classification  
- **Decision-oriented reports** — Cashflow, budget, drift, forecast, merchants  
- **Profile** — 12-month persona with refreshable snapshot  
- **Privacy by design** — Self-hosted; secrets via environment variables  

---

## Quick start

```bash
mvn spring-boot:run              # http://localhost:8080/app/login
cd frontend && npm run dev       # http://localhost:5173/app/ (optional)
```

---

## License

Apache License 2.0 — see [LICENSE](LICENSE).
