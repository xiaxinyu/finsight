# Dashboard 与 Profile 阅读指南

| | |
| :--- | :--- |
| **Language** | 简体中文 · [English](dashboard-profile.md) |

> 语义基础：[data-semantics.zh-cn.md](data-semantics.zh-cn.md)

---

## 1. 页面分工

| 页面 | 时间范围 | 更新方式 | 回答的问题 |
| :--- | :--- | :--- | :--- |
| <span style="color:#2563eb">**Dashboard**</span> | Period 自选 | 实时查询 | **这段时间**过得怎样？ |
| <span style="color:#2563eb">**Profile**</span> | 最近 **12 个月** | 快照；**Refresh** | **长期**财务类型？ |

<span style="color:#d97706">**注意**</span>：勿将 Dashboard 某段 Net 与 Profile 总分直接对比（时间与 scope 不同）。

---

## 2. Dashboard 模块

```
Period Picker ──► 所有 KPI / 图表
     ├─ Real income / Consumption / Net
     ├─ Cash flow 柱状图
     ├─ Expense breakdown 饼图
     ├─ Data quality 条
     ├─ Advisor cards（可选）
     └─ Account balance
```

### KPI 解读

| KPI | 怎么读 | 下一步 |
| :--- | :--- | :--- |
| Real income | 真实收入 | Cash flow 看月度波动 |
| Consumption | 生活消费 | 饼图 top3 是否集中 |
| Net | 结余/缺口 | 负值 → Spending Drift · Budget vs Actual |

### 交互

| 操作 | 结果 |
| :--- | :--- |
| 点击 Cash flow 某月 | 该月 drill |
| 点击饼图切片 | Reporting Classification → 商户 → 交易 |

语义 drill 按 **semantic tag** 过滤，不叠加 legacy `txn_types`。

---

## 3. Profile 模块

```
Overall Score + Confidence + User Type
     ├─ 雷达图 10 维度
     ├─ Weakest / Strongest
     └─ 维度详情 → Reason · Evidence · 动作
```

### 十个维度

| ID | 名称 | 衡量 |
| :--- | :--- | :--- |
| `income_stability` | 收入稳定性 | 12 个月收入波动 |
| `spending_control` | 支出控制 | 支出 vs 收入 |
| `savings_discipline` | 储蓄纪律 | 储蓄率 |
| `fixed_burden` | 固定负担 | 固定成本占收入 |
| `liquidity_safety` | 流动性安全 | 应急月数 |
| `debt_pressure` | 债务压力 | 偿债 vs 收入 |
| `lifestyle_inflation` | 生活方式通胀 | 支出增长 |
| `spending_concentration` | 支出集中度 | Top 分类占比 |
| `seasonality_risk` | 季节性风险 | 月际波动 |
| `data_trust` | 数据可信度 | 分类完整度 |

### 快照状态

| 状态 | 操作 |
| :--- | :--- |
| Not ready | Generate profile |
| Stale | Refresh |
| Reconciliation mismatch | 补分类；见 [runbook](../../tech/finance/profile-materialization-runbook.zh-cn.md) |

---

## 4. 推荐使用节奏

| 频率 | Dashboard | Profile | Reports |
| :--- | :--- | :--- | :--- |
| 日常 | Net + top3 | — | — |
| 每月 | 确认 period | — | Cashflow · Budget vs Actual · Spending Drift |
| 每季 | — | Refresh + weakest 3 | Trend Changes · Outlook · Cash Risk |

---

## 5. 关联文档

- [reports-catalog.zh-cn.md](reports-catalog.zh-cn.md)  
- [data-semantics.zh-cn.md](data-semantics.zh-cn.md)
