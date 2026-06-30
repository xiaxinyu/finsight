# How to read FinSight documentation

| | |
| :--- | :--- |
| **Language** | English · [简体中文](_style-guide.zh-cn.md) |

---

## Document layers

| Layer | You learn | Example |
| :--- | :--- | :--- |
| **Concepts** | What numbers mean and why | [data-semantics.md](data-semantics.md) |
| **Page guides** | How to use Dashboard, Profile, Reports | [dashboard-profile.md](dashboard-profile.md) |
| **Technical reference** | APIs, SQL views, acceptance tests | [personal-finance-reporting-guide.md](../../tech/finance/personal-finance-reporting-guide.md) |

Read Concepts first. Open Technical reference only when you develop or debug.

---

## Highlight markers

These colors show importance. They are **not** error messages (we do not use red for notes).

| Marker | Meaning |
| :--- | :--- |
| **Core** | Must understand before trusting KPIs |
| **Included** | Counted inside a metric or report scope |
| **Note** | Common misunderstanding — read carefully |
| **Reference** | Optional deep dive for engineers |

---

## English style (B1-friendly)

User docs follow these rules:

- Short sentences. One idea per line where possible.
- Finance terms stay professional (e.g. *cashflow*, *liability*) but are explained in plain English on first use.
- IT terms are spelled out once (e.g. *API*, *snapshot*, *drill-down*).
- **You** = the person using FinSight in the browser.

---

## Chinese style（中文）

- 财务术语采用行业通用译法（cashflow → 现金流，semantic → 语义）。
- IT 术语保留英文原名并在首次出现时括号释义（如 drill-down、snapshot）。
- 表格优先于长段落；章节编号固定，便于交叉引用。
