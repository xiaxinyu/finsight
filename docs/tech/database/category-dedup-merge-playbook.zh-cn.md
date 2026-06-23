# 分类去重与合并操作手册

适用场景：Sprint2 seed 后出现的 **重复 L1**（如 `INC` + `INCOME` 都叫「收入」）及 **语义重复 L2**（如 `INCOME-01` vs `INC-01`）。

**原则**：保留老 code（`INC-*` 等细分类），合并新 seed 的平行 code；**禁止改已有 code**。

---

## 前置条件

1. 应用已部署本 PR（合并逻辑修复 + `cascade=true` 默认）
2. 已跑 Flyway V23（`report_role` 列）
3. **先导出 baseline**（合并前）：

```bash
RUN_TAG=before-category-dedup ./scripts/db/export-classification-audit-baseline.sh
```

---

## 第一步：合并重复 L1「收入」

目标：只保留 **`INC`** 作为收入根；删除 **`INCOME`** L1。

### UI 操作（推荐）

1. 打开 **Administration → Categories**
2. 在树中选中 **`INCOME`**（重复的收入根，子类较少的那棵）
3. 「Merge into…」选择 **`INC (收入)`**
4. 查看 Impact Preview：
   - 应显示「N child categories will be reparented under INC」
5. 确认 **Merge categories**

系统会把 `INCOME` 下所有 L2 改挂到 `INC`，并 soft-delete `INCOME` L1。

### 验证

- 树里只剩一个「收入」根（`INC`）
- `INCOME-02` 副业经营等出现在 `INC` 下

---

## 第二步：合并语义重复的 L2

按下面表 **逐对** 合并（源 → 目标）。每次合并前看 Impact Preview 里的交易笔数。

| 源（删除） | 目标（保留） | 说明 |
|------------|--------------|------|
| `INCOME-01` | `INC-01` | 工资薪金 |
| `INCOME-03` | `INC-04` | 投资收益（若无 INC-04 则保留 INCOME-03 并改 parent 为 INC） |
| `INCOME-99` | `INC-99` | 其他收入 |
| `INCOME-02` | — | **保留**（副业经营；老库可能没有对应项） |

### UI 操作

1. 选中源分类（如 `INCOME-01`）
2. Merge into → `INC-01`
3. Preview 应提示会更新交易并 remap 规则
4. 确认合并

> 合并默认 **cascade=true**：交易 `consume_code/category_code` 与规则 `category_id` 一并迁到目标。

### 保留不合并的老分类（建议保留）

这些只有老 `INC-*` 有，**不要删**：

- `INC-02` 奖金/提成/年终奖
- `INC-05` 房租/资产出租收入
- `INC-06` 亲友资助/赠与
- `INC-07` 政府补贴/退税/公积金提取
- `INC-08/09` 借款
- `INC-10` 报销到账

---

## 第三步：交通「交通与车辆」去重

若存在两棵交通 L1（少见）：

1. L1 合并：重复根 → `TRAVEL`
2. 保留 `TRAVEL-01` 公共交通
3. 保留 seed 新增的 `TRANS-02`～`TRANS-07`（打车、停车、油费等）

**不要**把 `TRAVEL-01` merge 进 `TRANS-02`（不同粒度）。

---

## 第四步：其它 L1

| L1 | 策略 |
|----|------|
| `FIXED` / `LIVING` / `SHOPPING` | 老类保留；seed 的 `FIXED-*`/`DAILY-*`/`SHOP-*` 仅补缺 |
| `REIM` | 新 L1；与老 `INC-10` 报销到账并存（报表口径不同） |
| `OTHER` | 只留一个 catch-all `OTHER-01` |

---

## 第五步：合并后验证

```bash
# 1. 再导出 baseline
RUN_TAG=after-category-dedup ./scripts/db/export-classification-audit-baseline.sh

# 2. API 检查
# GET /api/v1/maintenance/classification-audit-summary
# 期望：active_orphan_rules 不增加

# 3. 可选：字段 drift 修复（审阅后手动）
# mysql ... < docs/tech/database/transaction-category-field-remediation.sql
```

Categories 页刷新后应只剩 **一棵** 收入树，且含奖金等老细分类 + 副业等新类。

---

## 手动 SQL（仅当 UI 不可用时）

见 [`category-l1-dedup.sql`](./category-l1-dedup.sql)（**禁止自动执行**）。

---

## 合并行为说明（本 PR 修复）

| 场景 | 行为 |
|------|------|
| L1 → L1 | 子类 reparent 到目标 L1；删源 L1 |
| L2 → L2 + cascade | 交易全字段同步 + 规则 remap + 删源 L2 |
| L2 → L1 | 仅 reparent（改 parent） |

关联：[classification-l2-target-catalog.zh-cn.md](./classification-l2-target-catalog.zh-cn.md)
