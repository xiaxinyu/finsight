# 设置分类语义（Admin）

| | |
| :--- | :--- |
| **Language** | 简体中文 · [English](set-category-semantics.md) |

为 category 配置 **report role** 与 **semantic tag**，使 Dashboard KPI 与报表 scope 正确。

---

## 前置条件

- 具备 Admin 权限。
- 明确该分类属于生活消费、收入、转账、税务等（见 [数据语义](../concepts/data-semantics.zh-cn.md)）。

---

## 操作步骤

1. 打开 **Admin → Categories**（`/app/admin/categories`）。

2. 选择目标 category（通常为 L2）。

3. 在 **Finance semantics** 区域设置：
   - **Transaction type**（`txn_types`）
   - **Report role**
   - **Reporting Classification**（semantic tag）
   - Inclusion 预览（income / expense / budget trend）

4. 示例：
   - 餐饮 → expense trend **开**，tag `dining_spending`
   - 工资 → income trend **开**，report role **income**
   - 信用卡还款 → expense trend **关**，nature **liability**

5. **保存**（可能触发 taxonomy version bump，属正常）。

6. 可选：执行 **Sync transactions** 级联历史行。

7. **Transactions** 抽样核对。

8. **Dashboard** 核对 Period 内 Consumption / Real income。

9. **Profile → Refresh**。

---

## 验收

| 检查项 | 预期 |
| :--- | :--- |
| Semantics 预览 | inclusion 与业务一致 |
| Dashboard KPI | 交易正确计入/排除 Consumption、Real income |
| Tax / Transfer 报表 | 仅出现在对应 scope |
| Profile | Refresh 后 Data trust 稳定或提升 |

---

## 禁止默认行为

- 勿将转账、还贷标为普通 **Consumption**（除非业务明确要求）。
- 勿将退款、报销标为 **Real income**。

详见 [finance-semantic-contract.zh-cn.md](../../tech/finance/finance-semantic-contract.zh-cn.md)。

---

## 关联文档

- [classify-unclassified-transactions.zh-cn.md](classify-unclassified-transactions.zh-cn.md)  
- [reconcile-kpi-numbers.zh-cn.md](reconcile-kpi-numbers.zh-cn.md)
