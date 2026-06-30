# 设置月度预算

| | |
| :--- | :--- |
| **Language** | 简体中文 · [English](set-monthly-budget.md) |

设置**月度支出上限**，并在 Budget vs Actual 中与 **Consumption** 对比。

---

## 前置条件

- 已登录。
- 已导入交易。
- 已明确目标月生活支出。

---

## 步骤

1. 打开 **Planning**（`/app/planning`）。

2. **Overview** → **Monthly budget**。

3. 输入 **limit amount**（CNY）。

4. 点击保存。

5. 打开 **Reports → Budget vs Actual**（`/app/reports/budget-vs-actual`）。

6. Period 与所审月份一致。

7. 对比 **Limit** 与 **Spent**（Spent = Consumption scope）。

---

## 验收

| 项 | 预期 |
| :--- | :--- |
| 保存 | 成功提示 |
| Budget vs Actual | Limit 与设定值一致 |
| Utilization | Spent ÷ Limit 反映进度 |

---

## 说明

- 当前 UI 保存单一桶 `all`（月度总上限）。
- **Spent** 不含转账、还贷、投资 — 与 Dashboard **Consumption** 同口径。

---

## 关联

- [data-semantics.zh-cn.md](../concepts/data-semantics.zh-cn.md)  
- [reports-catalog.zh-cn.md](../concepts/reports-catalog.zh-cn.md)  
- [reconcile-kpi-numbers.zh-cn.md](reconcile-kpi-numbers.zh-cn.md)
