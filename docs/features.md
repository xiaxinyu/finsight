# Features Overview

## Core Modules

### 1. Transaction Bill
- Multi-dimensional query: filter by date range, type, card, and keyword.
- Consumption tree: hierarchical category view for spending analysis.
- Smart display: auto-fit columns, striped rows, ellipsis for long text.
- Path: `/account/transaction/transaction_bill.html`

### 2. Income & Assets
- Salary: record and query monthly salary details.
  - Path: `/account/salary`
- Accumulation (Provident Fund): track housing provident fund payments.
  - Path: `/account/accumulation`

### 3. Social Insurance & Benefits
- Medical: medical insurance records.
  - Path: `/account/medical`
- Endowment (Pension): pension insurance payments.
  - Path: `/account/endowment`
- Unemployment: unemployment insurance records.
  - Path: `/account/unemployment`
- House Rent: rental expenditure management.
  - Path: `/account/house-rent`

## Product Characteristics

- Unified routing: all modules under `/account/*` for clarity.
- Template rendering: Thymeleaf HTML templates instead of legacy JSP.
- Security: Spring Security–based access controls.
- Visualization: ECharts-powered charts for trends and breakdowns.
