# Dashboard 与 Profile 指南

| | |
| :--- | :--- |
| **Language** | 简体中文 · [English](dashboard-profile.md) |

> KPI 定义：[data-semantics.zh-cn.md](data-semantics.zh-cn.md)

---

## 1. 两页分工

| 页面 | 时间窗口 | 更新机制 | 核心问题 |
| :--- | :--- | :--- | :--- |
| **Dashboard** | 自选 **Period** | 实时查询 | **当前区间**表现如何？ |
| **Profile** | 固定 **近 12 个月** | **Snapshot**；**Refresh** 重算 | **长期**财务模式如何？ |

**注意：** 勿将 Dashboard 某段 Net 与 Profile 总分直接对比（date range 与 scope 不同）。

---

## 2. Dashboard 结构

| 区域 | 内容 | 建议动作 |
| :--- | :--- | :--- |
| **KPI 卡片** | Real income · Consumption · Net | 每次访问先看 |
| **Cash flow 图** | 按月三项指标 | 点击月份 drill-down |
| **Expense 饼图** | Top Reporting Classifications | 点击切片 → merchant → transaction |
| **Data quality 条** | Unclassified 计数 | trust 低则补分类 |
| **Advisor cards** | 建议动作（feature flag） | 可选 |
| **Account balance** | 账户余额 | 与 period 消费不同维度 |

### KPI 解读

| KPI | 业务读法 | 异常时 |
| :--- | :--- | :--- |
| Real income | 区间内真实收入 | 查 [data-semantics.zh-cn.md](data-semantics.zh-cn.md) |
| Consumption | 区间内生活消费 | 看饼图 + Budget vs Actual |
| Net | 盈余（+）或缺口（−） | 负值 → Period Comparison 或 Budget vs Actual |

**Drill-down：** 饼图按 **semantic tag** 过滤，不仅依赖 legacy category tree。

---

## 3. Profile 结构

| 区域 | 内容 |
| :--- | :--- |
| **Overall score** | 加权健康分（0–100） |
| **Confidence** | 分数可信度 |
| **User type** | 类型标签（如 Disciplined saver） |
| **Radar** | 10 维度 |
| **Weakest / Strongest** | 改进与优势 |
| **维度详情** | Reason · Evidence · 跳转 |

### 十维度（业务含义）

| 维度 | 衡量 |
| :--- | :--- |
| Income stability | 收入稳定性 |
| Spending control | 支出相对收入的控制 |
| Savings discipline | 储蓄纪律 |
| Fixed burden | 固定成本占收入比 |
| Liquidity safety | 流动性 runway（月） |
| Debt pressure | 偿债压力 |
| Lifestyle inflation | 支出膨胀 |
| Spending concentration | 分类集中度 |
| Seasonality risk | 月际波动 |
| Data trust | 分类完整度 |

### Snapshot 状态

| 状态 | 含义 | 操作 |
| :--- | :--- | :--- |
| Not ready | 无快照 | Generate profile |
| Stale | 数据已变 | Refresh |
| Reconciliation mismatch | 物化指标与重算不一致 | 补分类；见 runbook |

---

## 4. 使用频率建议

| 频率 | Dashboard | Profile | Reports |
| :--- | :--- | :--- | :--- |
| 每日（5 min） | Net + 饼图 top3 | — | — |
| 每月（15 min） | 确认 Period | — | Cashflow · Budget vs Actual · Period Comparison |
| 每季（30 min） | — | Refresh + weakest 3 | Income / Consumption / Debt Trends · Annual Outlook · Cash Risk |

---

## 5. 关联文档

- [reports-catalog.zh-cn.md](reports-catalog.zh-cn.md)  
- [data-semantics.zh-cn.md](data-semantics.zh-cn.md)
