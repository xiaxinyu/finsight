# FinSight Product Guide

Language: English · [中文](PRODUCT_GUIDE.zh-CN.md)

FinSight is a **self-hosted personal finance application**: import bank activity, apply **rules-based classification**, and review **reports and trends** without sending data to a third-party cloud. This guide describes **product intent and capability areas**; exact features ship with each release—see [FUNCTION_LIST](FUNCTION_LIST.md).

**Principle:** clarity and control first—useful insight from data you already own.

---

## Edition tiers (roadmap reference)

**Free / Plus / Pro** labels describe a **planned capability matrix** for positioning and roadmapping. **They do not strictly map** to this repository’s license, distribution, or pricing. For what is implemented today, see [FUNCTION_LIST](FUNCTION_LIST.md).

The full tier table (Chinese) lives in [PRODUCT_GUIDE.zh-CN.md](PRODUCT_GUIDE.zh-CN.md) under **各版本功能一览（规划分档）**.

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
*   **Rules & Regex Guide**: See **[Rules Guide](RULES_GUIDE.md)** for best practices on writing and maintaining classification rules.

---

## The Local-First Advantage

Why we chose to be a desktop-class local application:

1.  **Zero Latency**: No loading spinners. Charts render instantly.
2.  **Absolute Privacy**: Your bank statements are the most sensitive documents you own. We believe they should never leave your hard drive.
3.  **Forever Access**: Even if FinSight shuts down, you have the code and your database (MySQL). You are not held hostage by a subscription.

---

## Future Roadmap

*These items align with the **Pro**-tier vision in the [Chinese tier matrix](PRODUCT_GUIDE.zh-CN.md). What ships when is release-specific—see [FUNCTION_LIST](FUNCTION_LIST.md).*

*   **AI Advisor**: Local LLM integration to answer questions like "Can I afford a vacation next month?"
*   **Scenario Planning**: "What if I lose my job?" simulation.
*   **Tax Optimization**: Auto-detection of tax-deductible expenses.
