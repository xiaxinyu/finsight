# 文档阅读说明

| | |
| :--- | :--- |
| **Language** | 简体中文 · [English](_style-guide.md) |

---

## 文档分层

| 层级 | 内容 | 示例 |
| :--- | :--- | :--- |
| **Concepts（概念）** | 指标含义与设计原因 | [data-semantics.zh-cn.md](data-semantics.zh-cn.md) |
| **Page guides（页面指南）** | Dashboard / Profile / Reports 用法 | [dashboard-profile.zh-cn.md](dashboard-profile.zh-cn.md) |
| **Technical reference（技术参考）** | API、SQL view、验收规则 | [personal-finance-reporting-guide.zh-cn.md](../../tech/finance/personal-finance-reporting-guide.zh-cn.md) |

建议先读 Concepts；仅在开发或排错时查阅 Technical reference。

---

## 标记说明

用于强调重点，**不代表错误**（非 error 场景不使用红色）。

| 标记 | 含义 |
| :--- | :--- |
| **核心** | 理解 KPI 前必须掌握 |
| **计入** | 纳入某指标或报表 scope |
| **注意** | 易误解之处 |
| **参考** | 工程向延伸阅读 |

---

## 中文表述规范

- 财务术语：Real income → 真实收入；Consumption → 生活消费；Net cashflow → 净现金流。
- IT 术语：首次出现附简要释义（如 drill-down → 下钻；snapshot → 快照）。
- 结构：表格 + 编号章节；避免大段 prose。

---

## English style（英文读者）

See [_style-guide.md](_style-guide.md) — B1-friendly sentences, defined terms, professional finance vocabulary.
