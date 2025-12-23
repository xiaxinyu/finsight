# FinSight Function List & Classification

This document provides a detailed list of features and capabilities of FinSight, categorized by their functional domain. It reflects the development history and the current state of the application.

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
*   **Assets & Income (资产与收入)**
    *   **Salary Management**: Dedicated module for tracking salary income.
    *   **Bank Card Management**: Manage bank cards and query transactions by card ID.
*   **Home Statistics**: Added overview statistics component (added).

## 4. System & Architecture (系统与架构)

The technical foundation of FinSight.

*   **Core Architecture**:
    *   **DDD Upgrade**: Refactored to Domain-Driven Design principles for better maintainability.
    *   **JDK 21**: Built on the latest Java LTS version.
*   **User Interface**:
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

*   **v1.6.0** (2025-12-23): Added Administration module (section and user management with role assignment); switched authentication to Database + BCrypt, improved failure handling and login page style; fixed UserMapper time column alignment.
*   **v1.5.0** (2025-12-19): Added Year/Month comparison, CRBank import, Delete Category.
*   **v1.4.0** (2025-12-18): Optimized Debit import, recovered automation keywords.
*   **v1.3.0** (2025-12-16): Batch category, Upload layout optimization.
*   **v1.2.0** (2025-12-12): Auto-classification, DDD upgrade, Transaction classifier.
*   **v1.1.0** (2025-12-09): Consumer rules maintenance, Bank card functions.
*   **v1.0.0** (2025-12-08): Initial Release, EasyUI integration, JDK 21 upgrade.
