# 处理未分类交易

| | |
| :--- | :--- |
| **Language** | 简体中文 · [English](classify-unclassified-transactions.md) |

为缺少 **Reporting Classification** 的交易补全分类，提升 Dashboard / Profile **Data trust**。

---

## 前置条件

- 已导入交易（[import-bank-statement.zh-cn.md](import-bank-statement.zh-cn.md)）。
- 已阅读 [数据语义](../concepts/data-semantics.zh-cn.md) 中 Real income / Consumption 区别。

---

## 操作步骤

1. 打开 **Transactions**（`/app/transactions`）。

2. 筛选 **Unclassified** 或 **Data quality** 快捷项。

3. 按 **金额** 降序 — 优先处理大额。

4. 逐笔或批量：
   - 选择正确 **category（L2）**；
   - 确认 **Transaction type**（Expense、Transfer 等）；
   - 保存。

5. 可选：重复商户建规则 — [write-classification-rule.zh-cn.md](write-classification-rule.zh-cn.md)。

6. 未分类数量下降后，查看 Dashboard data quality 条。

7. **Profile → Refresh**（[refresh-profile.zh-cn.md](refresh-profile.zh-cn.md)）。

---

## 验收

| 检查项 | 预期 |
| :--- | :--- |
| Unclassified 筛选 | 笔数减少 |
| Dashboard | unclassified 计数下降 |
| 报表/饼图 | 总额合理，切片归入正确桶 |
| Profile Data trust | Refresh 后提升 |

---

## 常见情形

| 情形 | 分类建议 |
| :--- | :--- |
| 账户间划转 | Transfer，非 Dining |
| 信用卡还款 | Finance / liability，非 Consumption |
| 工资 | Income + `report_role=income` |
| 税费 | Tax 相关 → Tax Summary |

语义不对？见 [set-category-semantics.zh-cn.md](set-category-semantics.zh-cn.md)。

---

## 关联文档

- [set-category-semantics.zh-cn.md](set-category-semantics.zh-cn.md)  
- [reconcile-kpi-numbers.zh-cn.md](reconcile-kpi-numbers.zh-cn.md)
