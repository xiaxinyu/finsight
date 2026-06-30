# FinSight 功能清单与分类

> **用户向报表说明：** [reports-catalog.zh-cn.md](../../user/concepts/reports-catalog.zh-cn.md)  
> **版本要点：** [version-highlights.zh-cn.md](../../user/concepts/version-highlights.zh-cn.md)

本文档按功能域列出能力，与代码演进对齐。产品分档见 [product-guide.zh-cn.md](../../user/concepts/product-guide.zh-cn.md)。

---

## 6. v2.0.x 专业语义与分析（当前）

### 6.1 财务语义层

*   **统一视图** `v_transaction_finance_semantics`：cash_direction · economic_nature · semantic_tag · inclusion 标志
*   **月度指标** `fin_metric_monthly`：`REAL_INCOME` · `CONSUMPTION_EXPENSE` · `NET_CASHFLOW` 等
*   **Admin 分类语义**：report_role 可编辑 · semantic tag · Finance semantics 面板
*   **Transactions 筛选**：Reporting Classification 全目录 + Quick filters（consumption / transfer / …）

### 6.2 Dashboard & Profile

*   **Dashboard 语义 KPI**：Real income · Consumption · Net（period-summary）
*   **Expense breakdown 饼图**：semantic scope expense，可 drill 到商户
*   **Profile 物化**：10 维度 · weighted score · confidence · user type · Refresh
*   **Metric hints**：KPI 旁 ? 口径说明

### 6.3 报表（决策导向）

*   **Cashflow & budget**：Cashflow · Budget vs Actual · Fund Flow · Transfer & Finance · Tax Summary
*   **Spending**：Fixed vs Variable · Spending Drift · Trend Changes
*   **Cash & outlook**：Bills Calendar · Annual Outlook · Cash Risk
*   **Merchants**：Subscriptions · Concentration · Drift
*   **共用**：Unified Drill Drawer · Reports Data quality bar · semantic drill

### 6.4 质量与 CI（v2.0.0–v2.0.1）

*   Metric gate / reconciliation · read-path 稳定 · 可索引 date range SQL
*   Forecast hybrid_projection · Profile GET read-only

---

## 1. 交易处理 (Transaction Processing)

用于导入和标准化原始财务数据的核心引擎。

*   **银行流水导入 (Bank Statement Import)**
    *   **借记卡支持**:
        *   建设银行 (CCB) 借记卡导入。
        *   华润银行 (CR Bank) 借记卡导入。
        *   招商银行 (CMB) 借记卡导入（新增）。
    *   **信用卡支持**:
        *   通用信用卡账单导入功能。
    *   **数据处理**:
        *   **上传界面**: 优化的账单文件上传界面。
        *   **过滤未分类数据**: 快速筛选并处理尚未分类的交易。

## 2. 智能分类 (Smart Classification)

将财务数据自动组织成有意义类别的工具。

*   **自动分类引擎 (Auto-Classification Engine)**
    *   **决策树分类器**: 基于 AI 的交易分类。
    *   **关键字规则**: 基于交易描述（关键字）的模式匹配。
    *   **消费类别规则**: 高级规则管理，实现细粒度控制。
*   **批量操作 (Batch Operations)**
    *   **批量分类**: 一次性为多笔交易分配类别。
    *   **自动化关键字**: 恢复并优化了基于关键字的自动化功能。
*   **类别管理 (Category Management)**
    *   **类别维护**: 创建、更新和删除消费类别。
    *   **v1.8 L2 目标字典**: Sprint 2 二级分类增补清单与手动 seed 脚本，见 [`docs/tech/database/classification-l2-target-catalog.zh-cn.md`](../database/classification-l2-target-catalog.zh-cn.md)；在线 plan：`GET /api/v1/maintenance/l2-category-seed-plan`。
    *   **级联更新**: 更新类别时自动更新相关联的交易。
    *   **类别迁移**: 在不同类别间迁移数据的工具。

## 3. 财务分析与报表 (Financial Analysis & Reports)

可视化您的财务健康状况。

*   **报表 (Reporting)**
    *   **收入 vs 支出趋势**：月度趋势图，支持点击下钻查看“分类饼图 + 交易明细”（新增）。
    *   **消费对比**: 跨年、跨月的消费对比分析 (同比/环比)。
    *   **交易报表**: 详细的交易历史明细报表。
    *   **ECharts 集成**: 高性能交互式图表 (升级至 ECharts 6.0.0)。
*   **管理分组 (Management Groups)**
    *   **收入管理 (Income Management)**: 专门的薪资收入追踪模块（重命名自薪资管理）。
    *   **支出管理 (Expense Management)**: 追踪房租等固定支出（重命名自房租）。
    *   **五险一金 (Benefit)**: 整合养老、公积金、医疗、失业保险的统一管理。
    *   **投资管理 (Investment)**: 未来扩展的投资追踪模块。
*   **银行卡管理**: 管理银行卡并通过卡号查询交易。
*   **主页统计**：新增总览统计组件（新增）。

## 4. 系统与架构 (System & Architecture)

FinSight 的技术基石。

*   **核心架构**:
    *   **DDD 升级**: 重构为领域驱动设计 (Domain-Driven Design) 原则，提高可维护性。
    *   **JDK 21**: 基于最新的 Java LTS 版本构建。
*   **用户界面**:
    *   **导航结构**: 重构菜单为交易、报表、收/支管理、五险一金、投资管理及后台管理（v1.6.0）。
    *   **jQuery EasyUI**: 集成 EasyUI 1.11.4，提供响应式且功能丰富的桌面级 Web 体验。
    *   **布局优化**: 持续改进登录、上传和仪表盘布局；多轮页面与样式优化（新增）。
    *   **登录页优化**: 修复错误提示抖动、统一英文错误信息、密码“眼睛”图标色差与样式优化（新增）。
*   **项目结构**：
    *   优化 FinSight/FinSight2 项目结构（新增）。
*   **数据库**:
    *   **迁移**: 强大的数据库迁移脚本，用于处理架构变更。
*   **身份认证**:
    *   **数据库认证**: 基于 Spring Security + 数据库的认证，用户源为 `app_user` 表（新增）。
    *   **密码加密**: 统一采用 `BCrypt` 加密；提供加密接口 `/encrypt/bcrypt?key=...`（新增）。
    *   **认证配置**: 显式注册 `DaoAuthenticationProvider` 绑定 `UserDetailsService` 与 `BCryptPasswordEncoder`（新增）。
    *   **失败处理**: 自定义失败信息，通过 `/login-error.json` 拉取错误并在前端展示（新增）。
*   **后台管理**:
    *   **Administration 分组**: 导航菜单新增 “Administration” 分组，位于导航栏末端（新增）。
    *   **用户管理**: 用户列表、创建/更新/删除、角色分配，支持密码自动 `BCrypt` 加密（新增）。

## 5. 版本历史 (Release History)

*   **v2.0.2** (2026-06): 专业财务语义层；Dashboard/Profile/报表口径统一；Profile 物化；Transfer & Finance · Tax Summary 报表；Reporting Classification drill；metric hints。详见 [version-highlights.zh-cn.md](../../user/concepts/version-highlights.zh-cn.md)。
*   **v2.0.1** (2026-06): 质量优化；Forecast hybrid；可索引 date range；Profile read-path。
*   **v2.0.0** (2026-06): Metric gate；read-path 稳定；L2 分类 seed。
*   **v1.8** (2026): 分类治理 UX；规则影响预览；数据质量层；报表导航重组。
*   **v1.6.0** (2025-12-23): 重构导航菜单（收入/支出管理、五险一金、投资）；新增后台管理模块（Admin 分组、用户管理与角色分配）；登录认证切换为数据库 + BCrypt，并优化失败提示与登录页样式；修复 UserMapper 时间列名对齐。
*   **v1.5.0** (2025-12-19): 新增年月消费对比，华润银行导入，删除类别功能。
*   **v1.4.0** (2025-12-18): 优化借记卡导入，恢复自动化关键字。
*   **v1.3.0** (2025-12-16): 批量分类，上传界面优化。
*   **v1.2.0** (2025-12-12): 自动分类，DDD 升级，交易分类器。
*   **v1.1.0** (2025-12-09): 消费规则维护，银行卡功能。
*   **v1.0.0** (2025-12-08): 初始发布，集成 EasyUI，升级 JDK 21。
