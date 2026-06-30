# 快速上手

| | |
| :--- | :--- |
| **Language** | 简体中文 · [English](getting-started.md) |

> 规范：[_style-guide.zh-cn.md](_style-guide.zh-cn.md)

约 **5 分钟**完成本地启动，并明确后续阅读路径。

---

## 1. 启动应用

```bash
# 终端 1 — 后端（login + API + 内置 UI）
mvn spring-boot:run
# 打开 http://localhost:8080/app/login

# 终端 2 — 前端开发（可选，热更新）
cd frontend && npm run dev
# 打开 http://localhost:5173/app/
```

**Environment variables（生产/本地 DB 必填）：**

| 变量 | 用途 |
| :--- | :--- |
| `SPRING_DATASOURCE_URL` | MySQL JDBC 连接串 |
| `SPRING_DATASOURCE_USERNAME` | 数据库用户 |
| `SPRING_DATASOURCE_PASSWORD` | 数据库密码 |
| `ACCOUNT_DES_SIGN_KEY` | 敏感字段加密 key |

详见 [local-development.md](../setup/local-development.md)

**生产打包：**

```bash
mvn clean package
```

SPA 输出至 `src/main/resources/static/app/`。

---

## 2. 推荐阅读顺序

| 步骤 | 文档 | 目标 |
| :---: | :--- | :--- |
| 1 | [data-semantics.zh-cn.md](data-semantics.zh-cn.md) | 理解 Real income / Consumption |
| 1b | [semantic-scenarios.zh-cn.md](semantic-scenarios.zh-cn.md) | 按场景查 KPI |
| 2 | [dashboard-profile.zh-cn.md](dashboard-profile.zh-cn.md) | 正确阅读 Dashboard / Profile |
| 3 | [reports-catalog.zh-cn.md](reports-catalog.zh-cn.md) | 选择报表 |
| 4 | [任务索引](../tasks/README.zh-cn.md) | 分步操作 |
| 5 | [version-highlights.zh-cn.md](version-highlights.zh-cn.md) | 了解 v2.0.x 变更 |

---

## 3. 首次使用检查清单

| # | 操作 | 指南 |
| :---: | :--- | :--- |
| 1 | 导入至少一份流水 | [import-bank-statement.zh-cn.md](../tasks/import-bank-statement.zh-cn.md) |
| 2 | 处理 unclassified 交易 | [classify-unclassified-transactions.zh-cn.md](../tasks/classify-unclassified-transactions.zh-cn.md) |
| 3 | 核对 category 语义 | [set-category-semantics.zh-cn.md](../tasks/set-category-semantics.zh-cn.md) |
| 4 | 设置 Dashboard Period，查看 Net | [dashboard-profile.zh-cn.md](dashboard-profile.zh-cn.md) |
| 5 | Profile → Generate / Refresh | [refresh-profile.zh-cn.md](../tasks/refresh-profile.zh-cn.md) |

---

## 4. 下一步

| 目标 | 文档 |
| :--- | :--- |
| 理解 KPI | [data-semantics.zh-cn.md](data-semantics.zh-cn.md) |
| 选择报表 | [reports-catalog.zh-cn.md](reports-catalog.zh-cn.md) |
| 编写归类规则 | [write-classification-rule.zh-cn.md](../tasks/write-classification-rule.zh-cn.md) |
| 报表下钻 | [drill-down-from-reports.zh-cn.md](../tasks/drill-down-from-reports.zh-cn.md) |
| 功能开发 | [technical.zh-cn.md](../../tech/architecture/technical.zh-cn.md) |
| 分步任务 | [任务索引](../tasks/README.zh-cn.md) |
