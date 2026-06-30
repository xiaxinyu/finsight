# 刷新财务 Profile

| | |
| :--- | :--- |
| **Language** | 简体中文 · [English](refresh-profile.md) |

在导入数据或修改分类后，重建 **近 12 个月 Financial Profile** 快照。

---

## 前置条件

- 已登录系统。
- 已导入交易（见 [import-bank-statement.zh-cn.md](import-bank-statement.zh-cn.md)）。
- 建议：已处理 unclassified 交易（[classify-unclassified-transactions.zh-cn.md](classify-unclassified-transactions.zh-cn.md)）。

---

## 操作步骤

1. 左侧菜单打开 **Profile**（`/app/profile`）。

2. 阅读顶部提示：
   - **Profile snapshot not ready** — 尚无快照。
   - **Profile may be outdated** — 数据已变，快照过期。

3. 点击页头按钮：
   - **Generate profile** — 首次生成。
   - **Refresh** — 更新已有快照。

4. 等待雷达图与 overall score 加载完成。

5. 查看 **Overall score**、**Confidence**、**Weakest** 维度；点击维度查看 Reason / Evidence。

---

## 验收

| 检查项 | 预期 |
| :--- | :--- |
| 顶部 banner | 无 not ready / outdated（除非之后又改数据） |
| **asOf** | 为近期日期 |
| 雷达图 | 10 个维度，0–100 分 |
| Data trust | 补分类后应上升 |

---

## 何时 Refresh

| 事件 | 操作 |
| :--- | :--- |
| 新导入流水 | Refresh |
| 修改 category / semantic tag | Refresh |
| 批量规则重分类 | Refresh |
| 月度复盘 | 出现 stale 提示时 Refresh |

---

## 故障排查

| 现象 | 处理 |
| :--- | :--- |
| Reconciliation mismatch | 补分类；见 [reconcile-kpi-numbers.zh-cn.md](reconcile-kpi-numbers.zh-cn.md) |
| Data trust 偏低 | [classify-unclassified-transactions.zh-cn.md](classify-unclassified-transactions.zh-cn.md) |
| 分数未变 | 确认 Refresh 完成；刷新页面 |

---

## 关联文档

- [dashboard-profile.zh-cn.md](../concepts/dashboard-profile.zh-cn.md)  
- [finance-semantic-contract.zh-cn.md](../../tech/finance/finance-semantic-contract.zh-cn.md)
