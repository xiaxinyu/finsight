# FinSight Function List & Classification

> **User guides:** [reports-catalog.md](../../user/concepts/reports-catalog.md) · [version-highlights.md](../../user/concepts/version-highlights.md)  
> **中文版:** [function-list.zh-cn.md](function-list.zh-cn.md)

Feature inventory aligned to the codebase. Product tiers: [product-guide.md](../../user/concepts/product-guide.md).

---

## 6. v2.0.x professional semantics (current)

### 6.1 Finance semantic layer

*   View `v_transaction_finance_semantics`: direction · economic_nature · semantic_tag · inclusion flags
*   Monthly metrics in `fin_metric_monthly`: `REAL_INCOME` · `CONSUMPTION_EXPENSE` · `NET_CASHFLOW`
*   Admin category semantics: editable `report_role` · semantic tag · inclusion preview
*   Transaction filters: full Reporting Classification catalog + quick filters

### 6.2 Dashboard & Profile

*   Dashboard semantic KPIs: Real income · Consumption · Net
*   Donut semantic breakdown with drill
*   Materialized Profile: 10 dimensions · confidence · user type · Refresh
*   Metric hints on KPIs

### 6.3 Decision-oriented reports (v2.0.3 navigation)

*   **Monthly overview**: Cashflow · Budget vs Actual · Bills Calendar
*   **Year-over-year trends**: Income Trends · Consumption Trends · Debt Trends
*   **Spending analysis**: Fixed vs Variable · Period Comparison (formerly Spending Drift)
*   **Capital & taxes**: Fund Flow · Transfers & Investments · Tax Summary
*   **Forecast & risk**: Annual Outlook · Cash Risk
*   **Merchants**: Subscriptions · Top Merchants · Merchant Changes
*   **Shared**: Unified Drill Drawer · Reports data quality bar · semantic drill · metric hints

### 6.4 Year-over-year analytics (v2.0.3)

*   **Income Trends**: `IncomeTrendAnalysisService` · `GET /api/v1/analytics/income-trends`
*   **Consumption Trends**: `TrendAnalysisService` · `GET /api/v1/analytics/trends` · matrix CSV / L1 toggle
*   **Debt Trends**: `DebtTrendAnalysisService` · `GET /api/v1/analytics/debt-trends` · borrowing / repayment / net flow
*   **Nav config**: `frontend/src/config/reportNavigation.ts` (single source for menu groups and breadcrumbs)

### 6.5 Quality & CI (v2.0.0–v2.0.1)

*   Metric gate / reconciliation · read-path stability · index-friendly date-range SQL
*   Forecast hybrid_projection · Profile GET read-only

---

## 1. Transaction Processing (交易处理)

The core engine for importing and normalizing raw financial data.

*   **Bank Statement Import (银行流水导入)**
    *   **Debit Card Support**:
        *   China Construction Bank (CCB) Debit Card (建设银行借记卡).
        *   China Resources Bank (CR Bank) Debit Card (华润银行借记卡).
        *   China Merchants Bank (CMB) Debit Card (招商银行借记卡) — added.
    *   **Credit Card Support**:
        *   Generic Credit Statement Import capabilities.
    *   **Data Handling**:
        *   **Upload Layout**: Optimized interface for uploading statement files.
        *   **Filter Unclassified Data**: Tools to isolate and process transactions that haven't been categorized yet.

## 2. Smart Classification (智能分类)

Automated tools to organize your financial data into meaningful categories.

*   **Auto-Classification Engine (自动分类引擎)**
    *   **Decision Tree Classifier**: AI-based classification for transactions.
    *   **Keyword Rules**: Pattern matching based on transaction descriptions (keywords).
    *   **Category Rules**: Advanced rule management for granular control.
*   **Batch Operations (批量操作)**
    *   **Batch Category**: Assign categories to multiple transactions at once.
    *   **Automation Keywords**: Recovered and optimized keyword-based automation.
*   **Category Management (类别管理)**
    *   **Category Maintenance**: Create, update, and delete consumption categories.
    *   **Cascade Updates**: Updating a category automatically updates related transactions.
    *   **Category Migration**: Tools to migrate data between categories.

## 3. Financial Analysis & Reports (财务分析与报表)

Visualizing your financial health.

*   **Reporting (报表)**
    *   **Income vs Expense Trend**: Monthly trend chart with interactive drill-down to "Category Pie + Transactions" (added).
    *   **Consumption Comparison**: Compare spending across different Years and Months (YoY, MoM).
    *   **Transaction Reports**: Detailed breakdown of transaction history.
    *   **ECharts Integration**: High-performance interactive charts (upgraded to ECharts 6.0.0).
*   **Management Groups (管理分组)**
    *   **Income Management**: Dedicated module for tracking salary income (Renamed from Payroll).
    *   **Expense Management**: Tracking fixed expenses like Rent (Renamed from Rent).
    *   **Benefit**: Consolidated management for Pension, Provident Fund, Medical, and Unemployment insurance.
    *   **Investment**: Placeholder for future investment tracking.
*   **Bank Card Management**: Manage bank cards and query transactions by card ID.
*   **Home Statistics**: Added overview statistics component (added).

## 4. System & Architecture (系统与架构)

The technical foundation of FinSight.

*   **Core Architecture**:
    *   **DDD Upgrade**: Refactored to Domain-Driven Design principles for better maintainability.
    *   **JDK 21**: Built on the latest Java LTS version.
*   **User Interface**:
    *   **Navigation Structure**: Restructured menu into Transactions, Reports, Income/Expense Management, Benefit, Investment, and Administration (v1.6.0).
    *   **jQuery EasyUI**: Integrated EasyUI 1.11.4 for a responsive and rich desktop-like web experience.
    *   **Layout Optimization**: Continuous improvements to login, upload, and dashboard layouts; multiple page & style refinements (added).
    *   **Login Page Optimization**: Fixed error tip jitter, unified English error messages, tuned password “eye” icon contrast and style (added).
*   **Project Structure**:
    *   Optimized FinSight/FinSight2 directory structure (added).
*   **Database**:
    *   **Migration**: Robust database migration scripts to handle schema changes.
*   **Authentication**:
    *   **Database-backed Authentication**: Spring Security integrated with `app_user` table (added).
    *   **Password Encryption**: Unified `BCrypt` hashing; encryption endpoint `/encrypt/bcrypt?key=...` (added).
    *   **Auth Configuration**: Explicit `DaoAuthenticationProvider` binding to `UserDetailsService` and `BCryptPasswordEncoder` (added).
    *   **Failure Handling**: Custom failure messages surfaced via `/login-error.json` for frontend display (added).
*   **Administration**:
    *   **Administration Group**: New “Administration” section in navigation, placed at the end (added).
    *   **User Management**: List, create/update/delete users, assign roles; passwords auto-hashed with `BCrypt` (added).

## 5. Release History (版本历史)

*   **v2.0.3** (2026-06): Income / Consumption / Debt YoY trend trio; Consumption Trends redesign; six-group Reports navigation; Period Comparison and related renames; trend semantic drill fix. See [version-highlights.md](../../user/concepts/version-highlights.md) · [v2.0.3-release-notes.md](../ops/v2.0.3-release-notes.md).
*   **v2.0.2** (2026-06): Professional finance semantic layer; unified Dashboard/Profile/report scopes; Profile materialization; Transfer & Finance · Tax Summary; semantic drill; metric hints. See [version-highlights.md](../../user/concepts/version-highlights.md).
*   **v2.0.1** (2026-06): Quality optimization; Forecast hybrid; index-friendly date range; Profile read-path.
*   **v2.0.0** (2026-06): Metric gate; read-path stability; L2 category seed.
*   **v1.8** (2026): Classification governance UX; rule impact preview; data quality layer; report navigation.
*   **v1.6.0** (2025-12-23): Restructured navigation menu (Income/Expense Management, Benefit, Investment); Added Administration module (section and user management with role assignment); switched authentication to Database + BCrypt, improved failure handling and login page style; fixed UserMapper time column alignment.
*   **v1.5.0** (2025-12-19): Added Year/Month comparison, CRBank import, Delete Category.
*   **v1.4.0** (2025-12-18): Optimized Debit import, recovered automation keywords.
*   **v1.3.0** (2025-12-16): Batch category, Upload layout optimization.
*   **v1.2.0** (2025-12-12): Auto-classification, DDD upgrade, Transaction classifier.
*   **v1.1.0** (2025-12-09): Consumer rules maintenance, Bank card functions.
*   **v1.0.0** (2025-12-08): Initial Release, EasyUI integration, JDK 21 upgrade.
