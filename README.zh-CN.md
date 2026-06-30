# FinSight

> **可自建的 personal finance intelligence** — 在本地完成交易归类、KPI 解读与报表分析，核心数据不上第三方云。

[![License: Apache-2.0](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

| | |
| :--- | :--- |
| **Language** | 简体中文 · [English](README.md) |
| **文档** | [文档中心](docs/user/concepts/overview.zh-cn.md) |

---

## 产品简介

FinSight 导入银行/卡流水，对每笔交易做**分类与语义标注**，并基于同一套规则生成 **Dashboard**、**Profile** 与 **Reports**。

自 **v2.0.2** 起，核心指标统一走 **finance semantic layer（财务语义层）**。在同一 date range 下，Dashboard 的 Real income 与 Cashflow 报表应一致。

**适用对象：** 需要专业级个人财务视图、且要求**数据自主托管**的个人或小团队。

---

## 核心术语（30 秒）

| 术语 | 含义 |
| :--- | :--- |
| **Real income（真实收入）** | 实际劳动/经营所得，不含退款、账户间划转、借款流入 |
| **Consumption（生活消费）** | 日常与预算类支出，不含转账、还贷、投资买入 |
| **Net cashflow（净现金流）** | 所选 period 内 Real income − Consumption |
| **Reporting Classification（报表分类）** | 分析用分类桶，如 Dining、Housing、Tax |
| **Profile（财务画像）** | 近 12 个月健康度快照；点击 **Refresh** 重算 |

完整定义：[数据语义](docs/user/concepts/data-semantics.zh-cn.md)

---

## 功能概览

| 模块 | 能力 |
| :--- | :--- |
| **导入与归类** | 流水导入、规则引擎、category、semantic tag |
| **Dashboard** | Real income · Consumption · Net · 支出结构 · drill-down |
| **Profile** | 10 维度评分、user type、evidence |
| **Reports** | 现金流、预算、drift、forecast、merchant、tax、transfer |
| **隐私与部署** | Self-hosted；secrets 经 environment variables 注入 |

---

## 环境要求

| 组件 | 版本 |
| :--- | :--- |
| Java | 21+ |
| Maven | 3.9+ |
| MySQL | 8.x |
| Node.js（仅前端开发） | 20+ |

生产环境请配置 `SPRING_DATASOURCE_*` 与 `ACCOUNT_DES_SIGN_KEY`。

---

## 快速启动

```bash
# 后端
mvn spring-boot:run
# → http://localhost:8080/app/login

# 前端热更新（可选）
cd frontend && npm run dev
# → http://localhost:5173/app/

# 生产打包
mvn clean package
```

首次使用：[5 分钟上手](docs/user/concepts/getting-started.zh-cn.md) · [本地开发](docs/user/setup/local-development.md)

---

## 文档导航

| 目标 | 文档 |
| :--- | :--- |
| 5 分钟上手 | [getting-started.zh-cn.md](docs/user/concepts/getting-started.zh-cn.md) · [EN](docs/user/concepts/getting-started.md) |
| 理解指标含义 | [data-semantics.zh-cn.md](docs/user/concepts/data-semantics.zh-cn.md) · [EN](docs/user/concepts/data-semantics.md) |
| 场景速查 KPI | [semantic-scenarios.zh-cn.md](docs/user/concepts/semantic-scenarios.zh-cn.md) · [EN](docs/user/concepts/semantic-scenarios.md) |
| Dashboard / Profile | [dashboard-profile.zh-cn.md](docs/user/concepts/dashboard-profile.zh-cn.md) · [EN](docs/user/concepts/dashboard-profile.md) |
| 报表说明 | [reports-catalog.zh-cn.md](docs/user/concepts/reports-catalog.zh-cn.md) · [EN](docs/user/concepts/reports-catalog.md) |
| 版本功能 | [version-highlights.zh-cn.md](docs/user/concepts/version-highlights.zh-cn.md) · [EN](docs/user/concepts/version-highlights.md) |
| 研发部署 | [technical.zh-cn.md](docs/tech/architecture/technical.zh-cn.md) |
| 分步任务 | [tasks/README.zh-cn.md](docs/user/tasks/README.zh-cn.md) · [EN](docs/user/tasks/README.md) |

---

## 技术栈

Spring Boot 3 · Java 21 · MyBatis-Plus · MySQL · React 19 · Ant Design · Vite

---

## 许可

[Apache License 2.0](LICENSE)
