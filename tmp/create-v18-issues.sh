#!/usr/bin/env bash
# Creates v1.8 data foundation GitHub issues. Run once from repo root.
set -euo pipefail
cd "$(dirname "$0")/.."

CODE_CONSTRAINT='## 分类 code 约束（强制）

- **禁止修改已有 `cls_category.code`**：SQL 与数据迁移只允许调整 `name`、排序、`parent_id`、`txn_types`、`deleted` 等；不得 `UPDATE` 已有行的 `code`。
- **允许改名称**：同一 `code` 下可更新 `name` 以消除歧义（例如「其它消费」子类命名优化）。
- **新增 code 必须唯一**：插入前查询 `SELECT 1 FROM cls_category WHERE code = ?`；命名遵循现有前缀规范（如 `INCOME-`、`FIXED-`、`DAILY-`），不得与已有 code 重复。
- **语义迁移走 alias/merge**：若需合并分类，保留旧 code 的 alias 记录，规则 `category_id` 指向新 code，不直接改旧 code。
- **历史交易批量变更**：必须走 migration batch 审计，禁止无记录的 ad-hoc UPDATE。

参考：[v1.8 数据基础版本计划](docs/tech/roadmap/v1.8-data-foundation-version-plan.zh-cn.md)'

create_issue() {
  local title="$1"
  local labels="$2"
  local body="$3"
  gh issue create --title "$title" --label "$labels" --body "$body"
}

# --- P0 ---

create_issue \
  "[P0][Data] 统一 merchant token 口径（报表与下钻一致）" \
  "type:bug,area:data,area:backend,area:analytics,priority:p0" \
  "$(cat <<EOF
## 背景

\`MerchantMiningService\` 使用 Java \`MerchantNormalizer\` 生成 stable token，但 \`v_transaction_analytics\` 与 \`TransactionMapper\` drill SQL 仍使用 \`lower(trim(opponent_name/transaction_desc))\`，导致 Merchant reports 下钻可能漏交易。

来源：v1.8 计划 §2.3、P0-1、Sprint 4

## 任务

- [ ] 定义单一 merchant token 契约（DB 列或视图字段），与 \`MerchantNormalizer\` 对齐
- [ ] 更新 \`v_transaction_analytics\`、\`drillMerchantBreakdown\`、\`filterMerchantTokenT\`、Merchant reports 查询
- [ ] 增加一致性测试：同一 token 在 Merchant report / Trend Changes / Drilldown / Transactions URL 返回相同交易集合

## 验收

- Merchant report 下钻金额与报表 aggregate 一致
- 回归 Merchant Concentration、Subscriptions、Merchant Drift、Unified Drilldown

## 关联

- Milestone: v1.8.0
EOF
)"

create_issue \
  "[P0][Classification] 清理 orphaned rules（归零或 inactive legacy）" \
  "type:bug,area:data,area:backend,priority:p0" \
  "$(cat <<EOF
## 背景

当前约 24 条 orphaned rules（目标分类已删除或无效），影响 Rule Engine 可信度与历史迁移安全。

来源：v1.8 计划 §2.2、P0-2、Sprint 2

## 任务

- [ ] 用 \`docs/tech/database/classification-data-audit.sql\` §3 导出 orphan 清单
- [ ] 可映射规则：\`category_id\` 批量指向有效 **category code**（不改 code，只改规则指向）
- [ ] 不可映射规则：设 \`active=0\`，\`remark\` 标记 \`inactive legacy\` + 原因
- [ ] Rule Engine UI  Orphaned 计数为 0 或仅显示已归档 legacy

$CODE_CONSTRAINT

## 验收

- Orphaned = 0，或全部有明确 remark 且 inactive
- 变更前后各跑一次 SQL audit 并保存结果

## 关联

- Milestone: v1.8.0
- Depends on: SQL 审计基线 issue
EOF
)"

create_issue \
  "[P0][Classification] 清理 invalid/legacy rules" \
  "type:bug,area:data,area:backend,priority:p0" \
  "$(cat <<EOF
## 背景

约 6 条 invalid/legacy rules（空 pattern 等），干扰规则总数理解且不产生命中。

来源：v1.8 计划 §2.2、P0-3

## 任务

- [ ] SQL audit §4–§5 导出 invalid 清单
- [ ] 无业务价值：soft-delete 或 inactive + remark
- [ ] 可修复：补 pattern 或指向有效 category code
- [ ] 文档记录每条处理决定

$CODE_CONSTRAINT

## 验收

- Invalid/legacy = 0，或每条均有 remark 且 inactive

## 关联

- Milestone: v1.8.0
EOF
)"

create_issue \
  "[P0][Data] 修复交易分类字段漂移（consume_code 为事实源）" \
  "type:bug,area:data,area:backend,priority:p0" \
  "$(cat <<EOF
## 背景

交易表同时存在 \`consume_id\`、\`consume_code\`、\`consume_name\`、\`category_code/category_name\`，部分更新可能导致报表口径不一致。

来源：v1.8 计划 §2.3、P0-5、Sprint 4

## 任务

- [ ] 定义事实源：\`consume_code\` 为主；\`consume_name\` 由 \`cls_category\` join 派生
- [ ] 写操作统一经 service 同步字段（分类变更、导入、批量重分类）
- [ ] SQL audit 增加/固化字段一致性检查；修复现有 drift 行
- [ ] 单元测试覆盖 write path

$CODE_CONSTRAINT

## 验收

- 分类字段一致性审计结果为 0（或可解释例外清单）
- 报表不再混用 \`consume_name\` 与 \`consume_code\` 得出不同分类

## 关联

- Milestone: v1.8.0
EOF
)"

create_issue \
  "[P0][Categories] 分类删除/迁移前影响预览" \
  "type:feature,area:frontend,area:backend,priority:p0" \
  "$(cat <<EOF
## 背景

分类 soft-delete 会停用关联规则，但 UI 缺少删除/迁移前的交易数、金额、规则、报表影响预览，易导致报表跳变。

来源：v1.8 计划 §2.1、P0-4、Sprint 3

## 任务

- [ ] 后端 API：给定 category code，返回 affected transactions / amount by month / affected rules / report impact 摘要
- [ ] Categories UI：删除、merge、改名前展示 impact preview + 二次确认
- [ ] Merge 仅改规则与 alias 指向，**不修改已有 category code**

$CODE_CONSTRAINT

## 验收

- 用户删除或迁移分类前能看到完整影响范围
- 无 impact preview 不允许执行 destructive 操作

## 关联

- Milestone: v1.8.0
EOF
)"

create_issue \
  "[P0][Docs] v1.8 SQL 审计基线与分类治理工作流" \
  "documentation,area:data,priority:p0" \
  "$(cat <<EOF
## 背景

v1.8 定义为 Data Foundation Release，发布前必须可审计。已有 \`classification-data-audit.sql\`，需固化流程与基线输出模板。

来源：v1.8 计划 §1、§7、Sprint 1、Issue 拆分 #1

## 任务

- [ ] 确认 \`docs/tech/database/classification-data-audit.sql\` 覆盖计划 §7 全部检查项
- [ ] 文档化「审计 → 设计 → 创建 → 映射规则 → 预览 → 应用 → 刷新 → 验证」八步流程
- [ ] 提供基线输出模板：未分类 Top100、其它消费 Top100、orphan/invalid 清单、字段 drift、merchant token 样本
- [ ] 在 roadmap 文档中链接到 issue 看板

$CODE_CONSTRAINT

## 验收

- 任何人可按文档跑完 audit 并产出 remediation 清单
- 分类大调整前后各保存一份 audit 结果

## 关联

- Milestone: v1.8.0-beta.1
- Blocks: 所有分类/规则数据治理 issue
EOF
)"

# --- P1 v1.8.0 ---

create_issue \
  "[P1][Data] 执行 SQL 审计并产出分类/规则修复清单" \
  "type:feature,area:data,priority:p1" \
  "$(cat <<EOF
## 背景

Sprint 1 交付物：用真实交易分布驱动分类设计，而非主观猜测。

来源：v1.8 计划 §6 Sprint 1

## 任务

- [ ] 执行完整 SQL audit，保存 baseline 快照（日期 + 结果文件）
- [ ] 导出：未分类 Top100、高金额其它消费 Top100、orphan/invalid rules、duplicate patterns、字段 drift、merchant token 不一致样本
- [ ] 为每个候选新增 L2 分类标注 report_role（budget/cashflow/asset/liability/investment/refund/transfer）
- [ ] 产出规则修复优先级列表

$CODE_CONSTRAINT

## 验收

- 清单文件入库 \`docs/tech/database/audit-results/\`（或 issue 附件）
- 每个新增分类建议含：建议 code（查重后）、建议 name、parent L1、预期规则数

## 关联

- Milestone: v1.8.0-beta.1
- Depends on: SQL 审计基线文档 issue
EOF
)"

create_issue \
  "[P1][Classification] 补齐二级分类（审计驱动，code 不可改）" \
  "type:feature,area:data,area:backend,priority:p1" \
  "$(cat <<EOF
## 背景

§1.3 目标字典定义了收入/固定支出/日常生活等 L2 子类。需按 audit 结果分批落地，减少「其它消费」承载。

来源：v1.8 计划 §1.3、§3 v1.8.0、Sprint 2

## 任务

- [ ] 按 audit 清单 Flyway 或 admin 脚本 **INSERT 新 L2**（仅新增，不改已有 code）
- [ ] 可调整已有分类 **name** 消除歧义
- [ ] 为报销/退款/转账/投资/负债/资产 L2 明确 \`txn_types\` / report_role
- [ ] 新分类默认仅用于新规则匹配，不立即批量改历史交易
- [ ] 更新 function-list / admin 分类树文档

$CODE_CONSTRAINT

## 建议 code 前缀（新增时查重）

| L1 | 建议前缀示例 |
|----|-------------|
| 收入 | \`INCOME-\` |
| 固定支出 | \`FIXED-\` |
| 日常生活 | \`DAILY-\` |
| 购物 | \`SHOP-\` |
| 交通 | \`TRANS-\` |
| 娱乐 | \`ENT-\` |
| 人情 | \`GIFT-\` |
| 报销返还 | \`REIM-\` |
| 资产 | \`ASSET-\` |
| 负债 | \`DEBT-\` |
| 投资 | \`INV-\` |
| 理财 | \`WEALTH-\` |
| 手续费 | \`FEE-\` |
| 其它 | \`OTHER-\` |

## 验收

- 新增 code 在 DB 中唯一
- 其它消费金额占比下降（阈值在 audit baseline 中定义）
- 每个 L1 至少有 usage 与 rules

## 关联

- Milestone: v1.8.0-beta.2
- Depends on: 审计清单 issue
EOF
)"

create_issue \
  "[P1][Classification] 安全分类变更八步工作流" \
  "type:feature,area:backend,area:frontend,priority:p1" \
  "$(cat <<EOF
## 背景

v1.8 核心能力：任何分类/规则调整必须先审计、预览、确认，再应用与验证。

来源：v1.8 计划 §5 P1-0

## 工作流

1. **Audit** — 跑 SQL audit
2. **Design** — 基于分布决定新增 L2
3. **Create** — 新增分类（inactive 或仅新交易）
4. **Map Rules** — 规则 dry-run
5. **Preview Migration** — 历史交易 before/after
6. **Apply** — 用户确认 + migration batch id
7. **Refresh** — metrics / merchant profiles
8. **Verify** — 再跑 audit

## 任务

- [ ] 后端 orchestration 或 maintenance API 骨架
- [ ] Admin UI 引导步骤或 checklist
- [ ] 禁止跳过 preview 的 bulk apply

$CODE_CONSTRAINT

## 验收

- 文档与 UI 一致；无 preview 不能 apply
- Apply 产生 migration batch 记录

## 关联

- Milestone: v1.8.0-rc.1
EOF
)"

create_issue \
  "[P1][Categories] 分类资产页：usage / rule coverage / report impact" \
  "type:feature,area:frontend,area:backend,priority:p1" \
  "$(cat <<EOF
## 背景

Categories 页目前只有树和编辑表单，缺少使用量、规则覆盖、数据质量与报表影响指标。

来源：v1.8 计划 §5 P1-1

## 任务

- [ ] 每分类展示：交易数、金额、最近交易日期
- [ ] 展示：active/off/orphan rules 数
- [ ] 展示：未分类候选、其它消费候选（可选）
- [ ] 右侧详情：View transactions / View rules / View report impact
- [ ] 「从 audit 候选创建子分类」入口（草案 → 确认后 INSERT 新 code）

$CODE_CONSTRAINT

## 验收

- 空分类、规则不足、异常集中分类可一眼识别
- 点击跳转 Transactions / Rule Engine 带筛选

## 关联

- Milestone: v1.8.0-rc.1
EOF
)"

create_issue \
  "[P1][Rules] 检测重复、过宽与冲突规则" \
  "type:feature,area:backend,area:frontend,priority:p1" \
  "$(cat <<EOF
## 背景

日常生活 184 条规则不代表质量高；需识别 duplicate pattern、broad keyword（支付/消费/转账）、冲突 priority。

来源：v1.8 计划 §2.2、§5 P1-2、Sprint 2

## 任务

- [ ] 后端分析：duplicate active patterns、跨分类冲突、broad keyword 风险、方向错误（收入规则命中支出）
- [ ] Rule Engine UI 展示风险标签与建议处理
- [ ] 导出修复清单（调整 priority / 停用 / 拆分规则）；规则 \`category_id\` 仅指向有效 code

$CODE_CONSTRAINT

## 验收

- SQL audit §6 duplicate patterns 可 UI 复现
- 用户可筛选「高风险规则」列表

## 关联

- Milestone: v1.8.0-beta.2
EOF
)"

create_issue \
  "[P1][Rules] 规则变更 dry-run 预览与命中样本" \
  "type:feature,area:backend,area:frontend,priority:p1" \
  "$(cat <<EOF
## 背景

新增/修改规则前无法预览影响多少未分类/已分类交易，也无法解释为何命中 A 而非 B。

来源：v1.8 计划 §2.2、§5 P1-2、Sprint 3

## 任务

- [ ] API：对 rule draft test against recent transactions
- [ ] 返回 top candidate rows、matched amount、before/after category
- [ ] 支持影响分层：仅未分类 / 会覆盖已分类 / 仅新导入
- [ ] UI：新增/编辑规则页集成 test + dry-run apply

$CODE_CONSTRAINT

## 验收

- 规则变更前必须能看到 matched sample 与金额影响
- 展示 priority 冲突解释（为何此规则胜出）

## 关联

- Milestone: v1.8.0-rc.1
EOF
)"

create_issue \
  "[P1][Data] 分类数据模型补强（alias、report_role、budgetable）" \
  "type:feature,area:data,area:backend,priority:p1" \
  "$(cat <<EOF
## 背景

缺少 alias、report_role、budgetable、cashflow_impact 等字段，报表易硬编码口径。

来源：v1.8 计划 §5 P1-3

## 任务

- [ ] Flyway：\`cls_category_alias\` 或 \`cls_category.aliases\` JSON
- [ ] \`cls_category.report_role\`、\`budgetable\`、\`cashflow_impact\`
- [ ] \`cls_rule.last_matched_at\`、\`hit_count\`、\`impact_amount_90d\`（可分阶段）
- [ ] 回填现有 L1/L2 的 report_role；**不改 code**

$CODE_CONSTRAINT

## 验收

- 报表可从 category 元数据读出口径，而非散落 magic string
- Alias 记录旧 code/旧名称，支持 merge 追溯

## 关联

- Milestone: v1.8.0
EOF
)"

create_issue \
  "[P1][Data] classification_migration_batch 审计表" \
  "type:feature,area:data,area:backend,priority:p1" \
  "$(cat <<EOF
## 背景

批量分类变更必须可回滚、可审计：交易 id、旧分类、新分类、原因、操作者、时间。

来源：v1.8 计划 §1.2、§5 P1-3、§3 v1.9 前置

## 任务

- [ ] Flyway：\`classification_migration_batch\` + detail 行表
- [ ] Apply 路径写入 batch；支持按 batch 查询影响
- [ ] 为 v1.9 批量重分类预留 API

$CODE_CONSTRAINT

## 验收

- 每次 bulk 分类变更有 batch id
- 可列出 batch 内 before/after 统计

## 关联

- Milestone: v1.8.0-rc.1
EOF
)"

# --- P1 v1.8.1 ---

create_issue \
  "[P1][Reports] 核心报表 Data Quality strip（v1.8.1）" \
  "type:feature,area:analytics,area:frontend,priority:p1" \
  "$(cat <<EOF
## 背景

v1.8.0 分类稳定后，每个核心报表需展示数据可信度，避免用户在脏数据上读趋势。

来源：v1.8 计划 §3 v1.8.1、§5 P1-5、Sprint 5

## 任务

- [ ] Data Quality strip：未分类占比、transfer/refund excluded、orphan category count、merchant token coverage、metrics source
- [ ] 接入：Cashflow、Budget vs Actual、Spending Drift、Trend Changes、Annual Outlook、Cash Risk、Merchant reports
- [ ] 未分类过高时降低 insight confidence + 引导 Rule Engine / 未分类交易

## 验收

- 上述报表均显示 data quality
- SQL audit 指标与 UI 字段一一对应

## 关联

- Milestone: v1.8.1
- Depends on: v1.8.0 分类治理完成
EOF
)"

create_issue \
  "[P1][Reports] 下钻 provenance 元数据（v1.8.1）" \
  "type:feature,area:analytics,area:frontend,priority:p1" \
  "$(cat <<EOF
## 背景

用户需要知道报表数字从哪来、样本是否截断、聚合总额是否完整。

来源：v1.8 计划 §3 v1.8.1

## 任务

- [ ] UnifiedDrillDrawer / drill API 展示：report id、source view、filter params、aggregate total、sample count、truncated flag
- [ ] 与现有 drill-breakdown partial warning 整合

## 验收

- 任意报表 KPI/表格行下钻可看到 provenance
- aggregate total 与报表一致

## 关联

- Milestone: v1.8.1
EOF
)"

# --- P2 v1.9 ---

create_issue \
  "[P2][Classification] 规则 impact preview（未分类 + 已分类变更）" \
  "type:feature,area:backend,area:frontend,priority:p2" \
  "$(cat <<EOF
## 背景

v1.9：修改高影响规则前预览 matched unclassified rows 与 would-change classified rows。

来源：v1.8 计划 §3 v1.9.0

## 任务

- [ ] 扩展 dry-run：按 category 汇总 amount impact
- [ ] UI 对比 before/after 分布

$CODE_CONSTRAINT

## 验收

- 用户修改规则前看到完整影响范围

## 关联

- Milestone: v1.9.0
EOF
)"

create_issue \
  "[P2][Classification] dry-run 批量重分类与用户确认应用" \
  "type:feature,area:backend,area:frontend,priority:p2" \
  "$(cat <<EOF
## 背景

不直接覆盖历史；输出 before/after，用户确认后批量 apply。

来源：v1.8 计划 §3 v1.9.0

## 任务

- [ ] Preview API + Apply API（写 migration batch）
- [ ] 仅更新 \`consume_code\` 等字段，不修改 category 字典 code
- [ ] Apply 后触发 dirty 标记

$CODE_CONSTRAINT

## 验收

- 无确认不能 apply；可回查 batch

## 关联

- Milestone: v1.9.0
- Depends on: migration_batch 表
EOF
)"

create_issue \
  "[P2][Data] taxonomy / rule set / metric refresh 版本化" \
  "type:feature,area:data,area:backend,priority:p2" \
  "$(cat <<EOF
## 背景

分类口径变化后历史报表需可比；需记录 taxonomy version、rule set version、metric refresh version。

来源：v1.8 计划 §3 v1.9.0

## 任务

- [ ] 版本表与 bump 规则（分类/规则/metric 变更时递增）
- [ ] 报表/API 返回当前 version

## 验收

- 报表可显示当前 taxonomy/rule version

## 关联

- Milestone: v1.9.0
EOF
)"

create_issue \
  "[P2][Data] 分类变更后 dirty 月份指标与 merchant profile 重算" \
  "type:feature,area:data,area:backend,priority:p2" \
  "$(cat <<EOF
## 背景

分类/规则变更后需 refresh \`fin_metric_monthly\`、merchant profiles、combined insights。

来源：v1.8 计划 §3 v1.9.0、§6 Sprint 3

## 任务

- [ ] dirty month 标记与 async/sync refresh job
- [ ] 刷新 merchant profiles 与 advisor combined insights
- [ ] maintenance API 或 admin 触发

## 验收

- Apply 后受影响月份 metrics 更新
- 重算可追踪 version

## 关联

- Milestone: v1.9.0
EOF
)"

# --- P3 v2.0 ---

create_issue \
  "[P3][Advisor] 可审计证据链 Advisor card（v2.0）" \
  "type:feature,area:advisor,area:frontend,priority:p3" \
  "$(cat <<EOF
## 背景

Advisor 需展示数据质量、分类证据、规则证据、交易样本、预测影响、推荐动作；用户反馈进入闭环。

来源：v1.8 计划 §3 v2.0

## 任务

- [ ] Advisor card 证据链 UI
- [ ] 接受/忽略、误分类标记、商户别名、预算调整跟踪

## 验收

- 用户能验证「为什么给我这个建议」

## 关联

- Milestone: v2.0.0
EOF
)"

create_issue \
  "[P3][Forecast] 预测回测 forecast vs actual（v2.0）" \
  "type:feature,area:analytics,area:backend,priority:p3" \
  "$(cat <<EOF
## 背景

需 MAPE/MAE/bias、分类预测误差、商户 recurring 误差，让用户判断预测可信度。

来源：v1.8 计划 §3 v2.0

## 任务

- [ ] 每月 actual 完成后对比上轮 forecast line
- [ ] Annual Outlook 展示近期平均误差

## 验收

- 用户能判断预测是否可信

## 关联

- Milestone: v2.0.0
EOF
)"

echo "Done. Created v1.8 data foundation issues."
