# 5 分钟上手

| | |
| :--- | :--- |
| **Language** | 简体中文 · [English](getting-started.md) |

> 标记说明：[_style-guide.zh-cn.md](_style-guide.zh-cn.md)

---

## 1. 启动

```bash
# 后端
mvn spring-boot:run          # → http://localhost:8080/app/login

# 前端热更新（可选）
cd frontend && npm run dev   # → http://localhost:5173/app/
```

环境变量：`SPRING_DATASOURCE_*` · `ACCOUNT_DES_SIGN_KEY` — [local-development.md](../setup/local-development.md)

生产打包：`mvn clean package`（SPA 输出至 `src/main/resources/static/app/`）。

---

## 2. 推荐阅读顺序

| 步骤 | 文档 | 目的 |
| :---: | :--- | :--- |
| 1 | [data-semantics.zh-cn.md](data-semantics.zh-cn.md) | 先懂 Real income / Consumption |
| 2 | [dashboard-profile.zh-cn.md](dashboard-profile.zh-cn.md) | Dashboard 与 Profile |
| 3 | [reports-catalog.zh-cn.md](reports-catalog.zh-cn.md) | 按需查报表 |
| 4 | [version-highlights.zh-cn.md](version-highlights.zh-cn.md) | v2.0.x 新能力 |

---

## 3. 首次使用检查清单

| # | 动作 | 目的 |
| :---: | :--- | :--- |
| 1 | 导入流水 | 有数据 |
| 2 | Transactions 处理未分类 | 提高 Data trust |
| 3 | Admin Categories 核对 semantic tag | 口径正确 |
| 4 | Dashboard 选 period 看 Net | 确认 KPI |
| 5 | Profile → Generate / Refresh | 长期画像 |

---

## 4. 下一步

| 目标 | 文档 |
| :--- | :--- |
| 理解数字 | [data-semantics.zh-cn.md](data-semantics.zh-cn.md) |
| 看报表 | [reports-catalog.zh-cn.md](reports-catalog.zh-cn.md) |
| 写规则 | [rules-guide.zh-cn.md](../../tech/contributing/rules-guide.zh-cn.md) |
| 开发扩展 | [technical.zh-cn.md](../../tech/architecture/technical.zh-cn.md) |
