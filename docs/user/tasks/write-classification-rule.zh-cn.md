# 编写分类规则

| | |
| :--- | :--- |
| **Language** | 简体中文 · [English](write-classification-rule.md) |

按描述文本自动匹配并写入 **category**。

---

## 前置条件

- Admin 权限。
- 目标 **category** 已存在且语义正确（[set-category-semantics.zh-cn.md](set-category-semantics.zh-cn.md)）。
- 已知稳定关键词（如商户名）。

---

## 步骤

1. **Admin → Rule engine**（`/app/admin/rules`）。

2. **Add rule**（或编辑已有规则）。

3. **Pattern type**：
   - **Contains** — 默认首选。
   - **Equals** / **Starts with** — 精确匹配。
   - **Regex** — 进阶用法。

4. 填写 **pattern**（如 `STARBUCKS`）。

5. 选择 **category**（L2）。

6. 可选：**impact preview** — 看命中笔数/金额。

7. 保存；如有开关则启用。

8. **Transactions** → **Unclassified** — 确认减少。

9. 需要时：**Profile → Refresh**。

---

## 验收

| 项 | 预期 |
| :--- | :--- |
| Impact preview | 命中合理（非 0、非全库） |
| 匹配交易 | 自动带上 category |
| 报表 | 金额进入正确桶 |

---

## 建议

| 推荐 | 避免 |
| :--- | :--- |
| 短而具体的商户 token | 过宽 pattern（如 `PAY`） |
| 保存前 preview | 未测 regex |
| 一商户族一类 | 同 pattern 多类冲突 |

---

## 故障排查

| 现象 | 处理 |
| :--- | :--- |
| 零命中 | 改 Contains；核对银行描述格式 |
| 命中过多 | 加长 pattern 或改 Equals |
| Rule tree 高风险 | Admin 修正 category 关联 |

---

## 关联

- [classify-unclassified-transactions.zh-cn.md](classify-unclassified-transactions.zh-cn.md)  
- [rules-guide.zh-cn.md](../../tech/contributing/rules-guide.zh-cn.md)
