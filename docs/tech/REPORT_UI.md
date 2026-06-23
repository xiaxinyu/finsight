# FinSight UI — React SPA

所有已登录界面由 **`/app/`** 提供（React + Ant Design）。旧版 Thymeleaf/EasyUI 页面已移除；历史 URL 会通过 Controller 重定向到 SPA 路由。

## 本地启动（仅后端）

先确保 MySQL 可连接，并设置环境变量（见 [`CLAUDE.md`](../../CLAUDE.md) 中的 `SPRING_DATASOURCE_*`）。

```bash
# 启动 Spring Boot
mvn spring-boot:run
```

浏览器访问：

- 登录页：http://localhost:8080/app/login
- 登录成功后：http://localhost:8080/app/dashboard

> 使用已构建的静态资源（`src/main/resources/static/app/`）。若尚未构建前端，请先执行下方「生产打包」或 `cd frontend && npm run build`。

## 开发模式（Vite 热更新）

前后端分离开发：Vite 将 API 请求代理到 `:8080`。

```bash
# 终端 1 — 后端
mvn spring-boot:run

# 终端 2 — 前端
cd frontend && npm run dev
```

浏览器访问：http://localhost:5173/app/

代理目标见 [`frontend/vite.config.ts`](../../frontend/vite.config.ts)（`/transaction`、`/api`、`/statement` 等 → `http://localhost:8080`）。

前端单测：

```bash
cd frontend && npm test
```

## 响应式与表格规范（v2.0.0）

### 视口验收

核心页面应在 **360、390、768、1024、1440**（及 1920）宽度下无**页面级**横向溢出：

- Profile（`.fs-data-page--profile`）
- Dashboard
- Reports 壳层
- Transactions Detail

自动化（可选，本地）：`cd frontend && npm run test:e2e`（Playwright）。**CI 暂不跑 E2E**；发布前请手工在 DevTools 设备模式下抽查上述视口。

### 表格

- 固定表头 + 明确 `scroll.x`；窄屏优先横向滚动，不压缩到不可读
- 金额列右对齐、等宽数字（`MoneyText` / tabular nums）
- Transactions：Date | Transaction | Category | Merchant | Amount | Card | Actions
- 行内操作按钮最小点击区域 **40×40px**（`.fs-row-action`）

### 图表

- 固定高度（如 Profile radar 320px），避免加载后布局跳变
- 移动端（≤768px）Profile radar 切换为维度条形列表

### 空态 / 错误态

- 使用 `EmptyState` / `Alert` + `PageSkeleton`；禁止空白图表占位

### 前端缓存

- 重型 analytics query 默认 `staleTime: 600_000`（10 分钟）
- 键名见 `frontend/src/constants/queryKeys.ts`

## 生产打包

```bash
mvn clean package
```

`prepare-package` 阶段由 `frontend-maven-plugin` 自动执行：

1. `npm ci`
2. `npm run build`

构建产物输出到 `src/main/resources/static/app/`，随 jar 一并发布。

跳过测试的快速打包：

```bash
mvn clean package -DskipTests
```

## 路由一览

| SPA 路径 | 功能 |
|----------|------|
| `/app/login` | 登录 |
| `/app/dashboard` | 首页 KPI + 图表 |
| `/app/transactions` | 交易明细 ProTable |
| `/app/statements/upload` | 账单导入（上传 → 预览 → 入账） |
| `/app/statements` | 导入历史 |
| `/app/reports/*` | 8 个分析报表 + 下钻 Drawer |
| `/app/ledgers/*` | 收入/支出/房租/社保台账 |
| `/app/admin/*` | 用户、卡片、规则、分类 |

## 财务 UI 规范

- 表格 `size="small"`，金额右对齐、`tabular-nums`
- 筛选：日期范围、卡片、分类树；Enter / Apply 触发刷新
- 导入：上传 → 预览 → 确认入账（不可跳步）
- 报表：空数据/稀疏年份显示 Insight 提示；点击图表可下钻交易明细

## 验证

```bash
mvn test
mvn checkstyle:check
cd frontend && npm run build
cd frontend && npm test
```

手工验收：登录/登出、各侧栏模块、报表年份切换、1280px 布局。
