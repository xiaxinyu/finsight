# FinSight Product Guide

| | |
| :--- | :--- |
| **Language** | English · [简体中文](product-guide.zh-cn.md) |

FinSight is a **self-hosted personal finance application**. You import bank data, classify transactions with rules, and review Dashboard, Profile, and Reports on your own server.

**Read these first (v2.0.2+):**

| Step | Document |
| :---: | :--- |
| 1 | [Data semantics](data-semantics.md) |
| 1b | [Semantic scenarios](semantic-scenarios.md) |
| 2 | [Dashboard & Profile](dashboard-profile.md) |
| 3 | [Reports catalog](reports-catalog.md) |
| 4 | [Tasks index](../tasks/README.md) |

Feature inventory: [function-list.md](../../tech/reference/function-list.md) · Releases: [version-highlights.md](version-highlights.md)

**Principle:** clarity and control first — then insight from data you already own.

---

## Edition tiers (roadmap reference)

**Free / Plus / Pro** labels describe a **planned capability matrix** for positioning and roadmapping. **They do not strictly map** to this repository’s license, distribution, or pricing. For what is implemented today, see [`docs/tech/reference/function-list.md`](../../tech/reference/function-list.md).

The full tier table (Chinese) lives in [`product-guide.zh-cn.md`](product-guide.zh-cn.md) under **各版本功能一览（规划分档）**.

---

## The Intelligence Engine

At the heart of FinSight is a local processing engine that classifies, links, and analyzes your financial footprint without ever sending a byte to the cloud.

### **1. Smart Categorization & Context**
*Turn "Transaction ID 99283" into "Friday Night Dinner".*

*   **Rule-Based Engine**: Custom rules engine that learns from your corrections.
*   **Multi-Dimensional Tagging**: Not just "Food", but "Food > Dining Out > Weekend".
*   **Merchant Normalization**: Cleans up messy bank descriptions into readable merchant names.

### **2. The Financial Persona**
*Understanding the "Who" behind the "How much".*

FinSight builds a profile of your financial behavior:
*   **The Saver vs. Spender**: Visualizes your savings rate trends over time.
*   **Lifestyle Inflation Tracker**: Alerts you when your fixed costs are creeping up faster than your income.
*   **Seasonal Patterns**: Detects recurring spikes (e.g., "You always overspend in December").

---

## Core Capabilities

### **Wealth Management (The "Balance Sheet")**
*A holistic view of what you own and what you owe.*

*   **Liquid Assets**: Real-time tracking of Cash, Bank Accounts, and Digital Wallets.
*   **Investment Tracking**: Manual or semi-automated tracking of Stocks, Funds, and Crypto.
*   **Liability Management**: Credit Card balances, Loans, and Mortgages.
*   **Net Worth Evolution**: A historical chart showing your true financial progress.

### **Income & Benefits (The "Safety Net")**
*Comprehensive tracking of your earnings and social safety net.*

*   **Salary Ledger**: Detailed breakdown of Base Pay, Bonus, and Tax.
*   **Social Security**: Tracks your invisible assets—Pension, Medical Insurance, Unemployment Insurance, and Housing Provident Fund.
    *   *Why this matters*: These are often your biggest hidden assets, yet most tools ignore them.

### **Spending Analysis (The "Cash Flow")**
*Deep dive into where the money goes.*

*   **Transaction Bill**: The master view of every penny. Supports complex filtering (Date, Category, Merchant, Amount).
*   **Trend Reports**:
    *   **Monthly Comparison**: Year-over-Year (YoY) and Month-over-Month (MoM) growth.
    *   **Category Breakdown**: Interactive Pie and Sunburst charts.
    *   **Drill-Down Analysis**: Click any bar in a chart to see the specific transactions behind it.
*   **Rules & Regex Guide**: See **[Rules Guide](../../tech/contributing/rules-guide.md)** for best practices on writing and maintaining classification rules.

---

## The Local-First Advantage

Why we chose to be a desktop-class local application:

1.  **Zero Latency**: No loading spinners. Charts render instantly.
2.  **Absolute Privacy**: Your bank statements are the most sensitive documents you own. We believe they should never leave your hard drive.
3.  **Forever Access**: Even if FinSight shuts down, you have the code and your database (MySQL). You are not held hostage by a subscription.

---

## Future Roadmap

*These items align with the **Pro**-tier vision in the [Chinese tier matrix](product-guide.zh-cn.md). What ships when is release-specific—see [`docs/tech/reference/function-list.md`](../../tech/reference/function-list.md).*

*   **AI Advisor**: Local LLM integration to answer questions like "Can I afford a vacation next month?"
*   **Scenario Planning**: "What if I lose my job?" simulation.
*   **Tax Optimization**: Auto-detection of tax-deductible expenses.
