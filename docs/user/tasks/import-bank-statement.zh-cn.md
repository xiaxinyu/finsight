# 导入银行流水

| | |
| :--- | :--- |
| **Language** | 简体中文 · [English](import-bank-statement.md) |

上传银行/卡导出文件，将交易写入 FinSight ledger。

---

## 前置条件

- FinSight 已运行（本地或部署环境）。
- 已有银行导出文件（CSV/Excel，需对应已适配的 bank adapter）。
- 明确文件所属的 **bank card / account**。

---

## 操作步骤

1. 打开 **Transactions → Import**（`/app/statements/upload`）。

2. 选择与实际文件一致的 **银行** 与 **账户/卡**。

3. 选择文件并上传。

4. 等待导入完成，留意 UI 错误提示。

5. 打开 **Transactions**（`/app/transactions`）：
   - 确认新交易日期、金额正确。
   - 如有空分类，使用 **Unclassified** 筛选。

6. 可选：**Import history**（`/app/statements`）查看历史导入。

---

## 验收

| 检查项 | 预期 |
| :--- | :--- |
| 交易笔数 | 与文件行数大致一致 |
| 日期 | 覆盖 statement period |
| 金额 | 抽样 3–5 笔与源文件一致 |
| Dashboard | 设置 Period 后 KPI 反映新数据 |

---

## 导入后

| 下一步 | 指南 |
| :--- | :--- |
| 补分类 | [classify-unclassified-transactions.zh-cn.md](classify-unclassified-transactions.zh-cn.md) |
| 更新 Profile | [refresh-profile.zh-cn.md](refresh-profile.zh-cn.md) |
| 修正分类语义 | [set-category-semantics.zh-cn.md](set-category-semantics.zh-cn.md) |

---

## 关联文档

- [getting-started.zh-cn.md](../concepts/getting-started.zh-cn.md)  
- [local-development.zh-cn.md](../setup/local-development.zh-cn.md)
