# FinSight

**个人财务智能应用 — 本地优先，以洞察驱动决策。**

| | |
| :--- | :--- |
| **Language** | 简体中文 · [English](README.md) |

## 定位

FinSight 将银行与卡片流水转化为**可分类、可对账、可报表**的财务信息。**v2.0.2** 起采用统一**财务语义层**，Dashboard、Profile 与报表口径一致。

---

## 文档导航（中英对照）

| 主题 | English | 简体中文 |
| :--- | :--- | :--- |
| <span style="color:#2563eb">**5 分钟上手**</span> | [getting-started.md](docs/user/concepts/getting-started.md) | [getting-started.zh-cn.md](docs/user/concepts/getting-started.zh-cn.md) |
| <span style="color:#2563eb">**理解数字**</span> | [data-semantics.md](docs/user/concepts/data-semantics.md) | [data-semantics.zh-cn.md](docs/user/concepts/data-semantics.zh-cn.md) |
| <span style="color:#2563eb">**Dashboard / Profile**</span> | [dashboard-profile.md](docs/user/concepts/dashboard-profile.md) | [dashboard-profile.zh-cn.md](docs/user/concepts/dashboard-profile.zh-cn.md) |
| <span style="color:#2563eb">**报表说明**</span> | [reports-catalog.md](docs/user/concepts/reports-catalog.md) | [reports-catalog.zh-cn.md](docs/user/concepts/reports-catalog.zh-cn.md) |
| **文档总览** | [overview.md](docs/user/concepts/overview.md) | [overview.zh-cn.md](docs/user/concepts/overview.zh-cn.md) |
| **研发** | [technical.md](docs/tech/architecture/technical.md) | [technical.zh-cn.md](docs/tech/architecture/technical.zh-cn.md) |

---

## 核心价值

- **语义化分析** — Real income · Consumption · Reporting Classification  
- **决策型报表** — 现金流、预算、漂移、预测、商户  
- **财务画像** — 12 个月维度评分，可 Refresh 重算  
- **隐私与可控** — 自建部署；凭据通过环境变量注入  

---

## 快速启动

```bash
mvn spring-boot:run              # http://localhost:8080/app/login
cd frontend && npm run dev       # http://localhost:5173/app/（可选）
```

---

## 许可

Apache License 2.0 — 详见 [LICENSE](LICENSE)。
