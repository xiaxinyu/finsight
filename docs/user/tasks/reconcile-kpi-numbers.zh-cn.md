# 核对 KPI 数字

| | |
| :--- | :--- |
| **Language** | 简体中文 · [English](reconcile-kpi-numbers.md) |

排查 **Dashboard**、**银行 App**、**报表** total 不一致的原因并修正。

---

## 开始前

FinSight **Consumption** ≠ 银行「总支出」。见 [数据语义](../concepts/data-semantics.zh-cn.md)。

---

## 步骤 1 — 对齐 date range

| 页面 | 规则 |
| :--- | :--- |
| Dashboard | **Period** 选择器 |
| Cashflow 报表 | 与 Dashboard 同 Period |
| Profile | 固定近 **12 个月** — 勿与单月 Dashboard Net 直接比 |

**动作：** 将 Dashboard Period 设为与对比对象一致。

---

## 步骤 2 — 对齐 report scope

| 对比对象 | 打开 | Scope |
| :--- | :--- | :--- |
| 生活消费 | Dashboard Consumption / Cashflow | Expense trend |
| 银行全部流出 | 银行 App | 含转账、贷款、税 |
| 转账/投资 | Transfer & Finance | `non_pnl` |
| 税务 | Tax Summary | `tax` |

---

## 步骤 3 — 检查分类

1. **Transactions** 按 Period + category 筛选。
2. 查找误标（如 transfer 记为 expense）。
3. 通过 [set-category-semantics.zh-cn.md](set-category-semantics.zh-cn.md) 或逐笔修正。
4. **Profile → Refresh**。

---

## 步骤 4 — 检查 data quality

| 信号 | 动作 |
| :--- | :--- |
| unclassified 多 | [classify-unclassified-transactions.zh-cn.md](classify-unclassified-transactions.zh-cn.md) |
| Reconciliation mismatch | 先补分类；等待 metric repair |
| 图表有数、drill 为 0 | 硬刷新；确认 v2.0.2+ semantic drill 已部署 |

---

## 步骤 5 — 验收对照

同一 Period、同一用户下：

| 对比项 | 预期 |
| :--- | :--- |
| Dashboard Net | ≈ Real income − Consumption |
| Dashboard vs Cashflow | Real income、Consumption 一致 |
| Consumption vs Budget Spent | 同为 consumption scope |
| Transfer & Finance | 不计入 Consumption |

---

## 仍无法对齐？

| 角色 | 文档 |
| :--- | :--- |
| 用户 | [reports-catalog.zh-cn.md](../concepts/reports-catalog.zh-cn.md) |
| 研发 | [personal-finance-reporting-guide.zh-cn.md](../../tech/finance/personal-finance-reporting-guide.zh-cn.md) |

---

## 关联文档

- [data-semantics.zh-cn.md](../concepts/data-semantics.zh-cn.md)  
- [refresh-profile.zh-cn.md](refresh-profile.zh-cn.md)
