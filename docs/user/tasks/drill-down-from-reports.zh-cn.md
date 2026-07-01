# 从报表下钻到交易

| | |
| :--- | :--- |
| **Language** | 简体中文 · [English](drill-down-from-reports.md) |

从图表或表格行打开 **Unified Drill Drawer**，查看 KPI 切片背后的商户与交易。

---

## 前置条件

- 已导入并完成分类。
- Dashboard **Period** 与所看报表一致。
- FinSight v2.0.2+（语义下钻）；v2.0.3 修复年度趋势矩阵下钻。

---

## 步骤

1. 打开报表，例如：
   - **Fixed vs Variable** 或 **Period Comparison**（任意两期对比）
   - **Consumption / Income / Debt Trends**（年度趋势矩阵）

2. 设置 **Period** 或 **对比年份**（趋势报表）；可选 Card / Category 筛选。

3. 点击 **饼图切片**、**柱状图** 或 **矩阵表格行**（如 Dining、Transport）。

4. 在下钻抽屉中：
   - **Insight** — 切片汇总；
   - **Breakdown** — 商户或子桶；
   - **Transactions** — 明细行。

5. 点击 breakdown 行可收窄到该商户。

6. 分类有误 → **Transactions** 编辑 category。

---

## 验收

| 项 | 预期 |
| :--- | :--- |
| 抽屉标题 | 含报表上下文 + 语义标签（如 Dining） |
| 交易笔数 | 切片有金额时 > 0 |
| 金额合计 | 与切片一致（同 Period、同筛选） |
| 语义下钻 | 按 **Reporting Classification**（`semanticFilter`），非旧版仅 expense 过滤 |
| 趋势矩阵 | v2.0.3+ 不再因 `consumeName` 与 semantic 冲突而 0 行 |

---

## 说明

- **Other** 合并切片可能不可下钻（虚拟桶）。
- 下钻继承报表的 **Card** / **Category** 筛选。
- **Period Comparison**：点击对应期别的柱（当前期 / 对比期）。
- **Consumption / Income Trends**：矩阵行按 semantic tag 下钻；日期为日历年 `YYYY-MM-DD`。

---

## 关联

- [reports-catalog.zh-cn.md](../concepts/reports-catalog.zh-cn.md)  
- [semantic-scenarios.zh-cn.md](../concepts/semantic-scenarios.zh-cn.md)  
- [reconcile-kpi-numbers.zh-cn.md](reconcile-kpi-numbers.zh-cn.md)
