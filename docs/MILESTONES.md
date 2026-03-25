# Product Roadmap & Milestones
Language: English | [中文](MILESTONES.zh-CN.md)

> **Vision**: From a "Digital Ledger" to a "Local, Intelligent Financial Advisor".
> **Execution Strategy**: Agile development powered by AI-augmented coding (1 Engineer + AI Pair Programmer).

This roadmap outlines the strategic path to building FinSight. It is designed to be **pragmatic**, delivering incremental value at each stage, while keeping technical debt low.

---

## Overview Timeline

| Phase | Theme | Core Value | Est. Duration | Status |
| :--- | :--- | :--- | :--- | :--- |
| **0** | **Foundation & Identity** | Rebranding, Codebase Cleanup, Docs | 1 Week | Completed |
| **1** | **The "Smart" Ledger** | Visual Insights, Usability, Mobile-Ready | 2 Weeks | In progress |
| **2** | **Data Foundations** | Collection, Cleaning, Organization | 2 Weeks | Planned |
| **3** | **Automation Engine** | "Set it and Forget it" Operations | 2 Weeks | Planned |
| **4** | **Local Intelligence** | "Chat with Data" (Local LLM), Insights | 4 Weeks | Planned |
| **5** | **Financial Wisdom** | Forecasting, Scenarios, Health Score | 3 Weeks | Planned |

---

## Phase 1: The "Smart" Ledger (Modernization)
**Goal**: Transform the legacy UI into a modern, responsive experience that makes users *want* to check their finances daily.

*   **Value Proposition**: "Clarity & Joy". Eliminate the friction of clunky interfaces.
*   **Key Deliverables**:
    *   [ ] **UI Overhaul**: Replace legacy tables with responsive Cards/Grids (Mobile friendly).
    *   [ ] **Dashboard 2.0**: Customizable widgets (Spending vs. Budget, Net Worth Trend).
    *   [ ] **Smart Search**: Global search bar (cmd+k) to find transactions or settings instantly.
    *   [ ] **Quality Gate**: 90% Unit Test coverage for Core Domain Logic.

## Phase 2: Data Foundations (Collection & Organization)
**Goal**: Build a robust pipeline to ingest, clean, and organize 10+ years of fragmented financial history.

*   **Value Proposition**: "Data Integrity". Garbage in, garbage out. This phase ensures your financial data is accurate, unified, and trusted.
*   **Key Deliverables**:
    *   [ ] **Multi-Source Ingestion**:
        *   Standardized Importers for **Alipay** and **WeChat Pay** (CSV export handling).
        *   Universal Bank Statement Parser (PDF/Excel support for major banks).
    *   [ ] **Data Hygiene Pipeline (ETL)**:
        *   **Deduplication**: Intelligent detection of overlapping imports (e.g., importing Jan-Mar then Feb-Apr).
        *   **Refund Matching**: Automatically link refunds to their original expenses.
    *   [ ] **Organization Framework**:
        *   **Merchant Normalization**: Map "STARBUCKS COFFEE SHA" and "Starbucks Beijing" -> "Starbucks".
        *   **Tagging System**: Add multi-dimensional tags (e.g., `#BusinessTrip`, `#Reimbursable`) alongside categories.

## Phase 3: The Automation Engine (Efficiency)
**Goal**: Reduce the manual effort of data entry and maintenance to near zero.

*   **Value Proposition**: "Time Freedom".
*   **Key Deliverables**:
    *   [ ] **Auto-Categorization Rules**:
        *   Regex-based rules editor ("If description contains 'Uber', set category to 'Transport'").
        *   "Smart Suggest" based on history.
    *   [ ] **Recurring Bill Detection**: Auto-identify subscriptions (Netflix, Spotify) and fixed costs (Rent).
    *   [ ] **Data Export**: One-click JSON/CSV backup (Data Sovereignty).
    *   [ ] **Privacy-First Sync**: Optional local network sync between devices (no cloud).

## Phase 4: Local Intelligence (The "Advisor")
**Goal**: Integrate Local LLMs (e.g., Llama 3 / Ollama) to enable natural language interaction.

*   **Value Proposition**: "Understand your money". Ask questions, get answers.
*   **Key Deliverables**:
    *   [ ] **RAG Engine (Retrieval-Augmented Generation)**: Index transaction history for LLM querying locally.
    *   [ ] **"Ask FinSight" Interface**:
        *   *"How much did I spend on dining out last month compared to last year?"*
        *   *"Show me my largest expenses in 2024."*
    *   [ ] **Privacy Guard**: Ensure no data leaves the local network during inference.
    *   [ ] **Insight Push**: Proactive notifications ("You've exceeded your dining budget by 15%").

## Phase 5: Financial Wisdom (Forecasting)
**Goal**: Move from "Past Reporting" to "Future Planning".

*   **Value Proposition**: "Secure your future".
*   **Key Deliverables**:
    *   [ ] **Monte Carlo Simulations**: "If I save $2k/month, when can I retire?"
    *   [ ] **Scenario Planning**: "What if I buy a Tesla? How does that impact my runway?"
    *   [ ] **Financial Health Score**: A unified metric (0-100) tracking liquidity, savings rate, and debt.
    *   [ ] **Asset Allocation**: Rebalancing recommendations for investment portfolios.

---

## 🛠 Engineering Standards (The "How")

To achieve this with a small team (User + AI), we adhere to:

1.  **AI-First Development**:
    *   Use AI for boilerplate, unit tests, and refactoring.
    *   Use AI for documentation generation.
2.  **Quality Assurance**:
    *   **No broken builds**. Main branch is always deployable.
    *   **Test Driven Development (TDD)** for complex financial logic (especially Phase 2 ETL).
3.  **Local-First Architecture**:
    *   Avoid complex microservices. Monolithic Modular Architecture is preferred for easy deployment.
    *   SQLite/H2 for dev, MySQL for prod.

## 📉 Risk Management

*   **Risk**: Data format changes from Banks/Alipay.
    *   *Mitigation (Phase 2)*: Plug-in architecture for parsers so community can contribute fixes without core code changes.
*   **Risk**: LLM Hallucination.
    *   *Mitigation (Phase 4)*: Strict prompting, citing sources (showing the transactions used to calculate), and disclaimers.
