# L2 分类候选清单 — Sprint 1（Issue #68）

日期：2026-06-23  
阶段：`BEFORE`  
数据来源：`baseline-unclassified-top100.csv`、`baseline-other-consumption-top100.csv`（运行 export 脚本后核对 `expected_rule_count`）

## 说明

- **禁止修改已有 `cls_category.code`**；下表均为**新增候选 code**（创建前在 DB 中确认唯一）。
- `report_role` 对齐 v1.8 报表口径：`budget` | `cashflow` | `asset` | `liability` | `investment` | `refund` | `transfer`（及 `expense`/`income` 等现有 L1 口径）。
- `expected_rule_count`：从 Top100 导出中按 `raw_text` / 商户关键词聚类估算；**运行 export 后替换「待核对」**。

## 从未分类（§18）拆出的 L2 候选

| code（唯一） | name | parent L1 code | report_role | expected_rule_count | 规则关键词示例 |
|--------------|------|----------------|-------------|---------------------|----------------|
| `EXP_FOOD_DELIVERY` | 外卖 / 餐饮配送 | `EXP_FOOD` 或现有餐饮 L1 | budget | 待核对 | 美团外卖、饿了么、京东到家 |
| `EXP_SUBSCRIPTION` | 订阅服务 | `EXP_DIGITAL` 或 `EXP_OTHER` | budget | 待核对 | Apple、Spotify、Netflix、iCloud |
| `EXP_PARKING_TOLL` | 停车 / 路桥 | `EXP_TRANSPORT` | budget | 待核对 | 停车、ETC、高速 |
| `EXP_MEDICAL` | 医疗 / 药店 | `EXP_HEALTH` | budget | 待核对 | 医院、药房、医保 |
| `EXP_EDUCATION` | 教育 / 培训 | `EXP_OTHER` | budget | 待核对 | 学校、培训、Coursera |

## 从 OTHER / 其它消费（§19）拆出的 L2 候选

| code（唯一） | name | parent L1 code | report_role | expected_rule_count | 备注 |
|--------------|------|----------------|-------------|---------------------|------|
| `EXP_UTILITIES` | 水电燃气 | `EXP_HOUSING` | budget | 待核对 | 从 OTHER 大额 recurring 识别 |
| `EXP_INSURANCE` | 保险 | `EXP_OTHER` | cashflow | 待核对 | 年缴/月缴 |
| `EXP_TAX_FEE` | 税费 / 手续费 | `EXP_OTHER` | cashflow | 待核对 | 非转账类银行手续费 |
| `INC_REIMBURSE` | 报销入账 | `INC_OTHER` | refund | 待核对 | 公司报销、退款入账 |
| `TRF_INTERNAL` | 本人账户互转 | `TRF` 或转账 L1 | transfer | 待核对 | 与 §17 转账口径对齐 |

## 非消费类 report_role 映射（创建时一并设 `txn_types`）

| 场景 | 建议 L2 code 前缀 | report_role | txn_types 提示 |
|------|---------------------|-------------|------------------|
| 信用卡还款 | `LIAB_CC_REPAY` | liability | 负债减少 |
| 理财申购 / 赎回 | `INV_FUND_*` | investment | 资产变动 |
| 退款原路返回 | `REF_*` | refund | 关联原消费 |
| 账户间转账 | `TRF_*` | transfer | 排除收支报表 |

## 验收（Issue #68）

- [ ] 每个候选行具备：**唯一 code、name、parent L1、report_role、expected_rule_count**
- [ ] `expected_rule_count` 已与本地 Top100 CSV 交叉验证
- [ ] 设计评审后进入 Categories「Create」步骤（[#70](https://github.com/xiaxinyu/finsight/issues/70) 工作流 Step 3）

关联：[classification-governance-workflow.zh-cn.md](../classification-governance-workflow.zh-cn.md)
