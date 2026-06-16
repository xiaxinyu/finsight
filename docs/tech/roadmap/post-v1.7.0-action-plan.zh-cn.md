# FinSight v1.7.0 后续完善执行计划

日期：2026-06-16
目标版本：v1.7.x / v1.8.0
定位：不继续堆新模块，集中把现有报表、数据 insight、个人画像、预测、Transactions / Detail 打磨成专业金融分析级体验。

## 1. 当前基线

v1.7.0 之后，项目已经具备完整的顾问式个人金融分析雏形：

- Transactions / Detail：交易明细、筛选、KPI、未分类入口、内联编辑、批量删除、批量转收入/支出、转账配对、自动分类预览。
- Reports：Cashflow、Budget vs Actual、Fund Flow、Fixed vs Variable、Spending Drift、Bills Calendar、Annual Outlook、Trend Changes、Cash Risk、Subscriptions、Merchant Concentration、Merchant Drift。
- Profile：10 维个人财务画像、画像历史、证据、动作入口、用户类型。
- Forecast / Insight：年度收支预测、场景参数、置信区间、分类预测、现金风险日历、趋势分解、商户挖掘、Advisor recommendations。
- Data foundation：`v_transaction_analytics`、monthly metrics、forecast run/line、profile snapshot、merchant profile、insight/recommendation tables。

本轮验证结果：

- `mvn -q test`：通过。
- `mvn -q checkstyle:check`：通过。
- `cd frontend && npm test`：通过，25 个测试文件，91 个测试。
- `cd frontend && npm run lint`：失败，18 个 error、5 个 warning。主要集中在 React 19 lint 规则、Transactions refs、Statements Upload state 顺序、Rules hook 调用、若干 Fast Refresh 导出问题。

结论：后端质量已经回到可发布状态；前端测试通过，但 lint 发现的问题需要作为发布前 P0 处理。产品层面不应再扩大功能面，而应把“看见数据”升级为“理解变化、定位原因、验证证据、执行动作”。

## 2. 世界级金融 insight 标准

每一个重要报表和表格都应该满足 6 个标准：

1. 准确：金额方向、转账排除、退款排除、分类口径、日期范围必须一致。
2. 可解释：每个结论要告诉用户“为什么”，并显示关键证据。
3. 可下钻：KPI、图表点、表格行必须能进入分类、商户、交易明细。
4. 可比较：当前期、上期、预算、预测、置信区间要能在同一语境里比较。
5. 可行动：每个 insight 要有下一步动作，例如调预算、修分类、查看交易、处理订阅、更新账单。
6. 可校准：预测和画像要留下历史，用实际结果反向评估模型质量。

当前项目已经有这些能力的基础，但还没有完全统一成闭环。

## 3. 关键不足

### 3.1 报表和 insight

现状：

- 各报表都有 KPI、图表、表格或 insight。
- Annual Outlook 已经区分 actual / forecast，支持置信区间和分类预测。
- Trend Changes 已经输出收入、支出、储蓄率、固定成本、分类 movers、商户 movers、生活方式膨胀。
- Cash Risk 已经有日历和月度净现金流。
- Merchant reports 已经识别订阅、集中度、商户漂移。

不足：

- 报表之间的交互模型不完全一致。有些表格可点击下钻，有些只能看。
- 表格缺少金融分析常用的视觉编码：进度条、热力底色、贡献占比、风险标签、actual/forecast 分段、delta 正负色。
- Trend Changes 的分类贡献和商户贡献都以总支出变化为分母，图上并列展示时容易被用户误解为可相加贡献。
- Annual Outlook 只有 year/scenario 选择，后端已有 `incomeChangePct`、`newMonthlyBill`、`lumpSumExpense`、`targetMonthlyPayment` 场景参数，但 UI 没有让用户细化现有场景。
- Category forecast 表格没有下钻，用户不能从预测分类直接回到历史交易证据。
- Merchant reports 没有统一下钻抽屉，无法从订阅/集中度/漂移直接查看底层交易。
- Insight 文案有结论，但缺少统一的“影响金额、置信度、证据来源、建议动作、预期效果”结构。

### 3.2 Transactions / Detail

现状：

- 页面信息密度高，适合处理大量交易。
- 筛选、KPI、active chips、内联编辑、批量动作已经成型。
- 未分类交易可以一键过滤，自动分类有确认弹窗。

不足：

- 表格是操作型明细表，还不是分析型明细表。用户很难一眼看出哪些交易是异常、重复、订阅、固定支出、转账候选、分类低置信度。
- `ProTable` 列上声明了 `sorter: true`，但 request 没有消费 sorter 参数，用户以为能排序，实际服务端排序没有生效。
- KPI strip 只能看总额，无法一键切换收入/支出/转账/异常等分析视角。
- 交易行缺少视觉层次：金额、商户、分类、账户、memo 的主次关系可以更清楚。
- 批量动作安全性还可以提高：危险动作需要显示影响范围和金额汇总。
- 搜索仍复用 `demoArea`，只能搜 memo/description；商户下钻也靠关键词近似，和 merchant profile 的标准化商户不是同一套口径。

### 3.3 统一下钻

现状：

- `UnifiedDrillDrawer` 已经支持 insight -> breakdown -> transactions -> actions。
- 报表、Annual Outlook、Trend Changes、Cash Risk 可以打开下钻抽屉。

不足：

- 下钻明细一次最多拉 200 行，breakdown 和 merchant totals 在大数据量下会失真。
- 分类下钻有时使用 `consumeName`，而不是稳定的 `consumeID/consumeCode`，重名分类或改名后可能不准。
- 商户下钻使用 `merchantLabel` + `demoArea` 模糊搜索，不能保证覆盖同一标准化商户的所有交易。
- actions 比较通用，没有根据当前 insight 动态生成最贴近的下一步。

### 3.4 个人画像

现状：

- 画像已经有 10 维分数、level、reason、evidence、actions、userType。
- spending concentration 已经从交易分析视图计算，不再是占位。

不足：

- 部分 action 仍指向旧报表路径，例如 `/reports/income-curve`、`/reports/category-breakdown`，虽然前端有 redirect，但会让产品语义不统一。
- `fin_profile_snapshot` 每次刷新会新增当天同一维度记录，应该按 user/date/dimension upsert，避免历史图被重复刷新污染。
- 画像和 Annual Outlook / Trend Changes 没有组合成一句真正的用户指导，例如“你是高固定负担型，明年 3 月现金流承压，主要由房租和订阅增加导致”。

### 3.5 数据质量和模型校准

现状：

- 有 metrics gate、report fallback、forecast run/line、profile history。

不足：

- 报表页面没有统一显示数据可信度，例如未分类比例、转账未标记风险、最近导入覆盖范围。
- Forecast run/line 已持久化，但还没有“预测 vs 实际”的回测视图和误差指标。
- Merchant mining、classification、profile、forecast 各自有证据，但没有统一 evidence schema 展示给用户。

## 4. P0 Bug 修复计划

目标：先让当前版本达到干净可发布状态，不改产品范围。

### P0-1. 修复前端 lint

范围：

- `frontend/src/components/ClassifyConfirmModal.tsx`
- `frontend/src/components/MoneyText.tsx`
- `frontend/src/components/PeriodRangePicker.tsx`
- `frontend/src/components/drilldown/UnifiedDrillDrawer.tsx`
- `frontend/src/layouts/AppLayout.tsx`
- `frontend/src/pages/Admin/Rules/index.tsx`
- `frontend/src/pages/Planning/index.tsx`
- `frontend/src/pages/Statements/Upload/index.tsx`
- `frontend/src/pages/Transactions/index.tsx`

重点：

- 避免在 effect 中同步 setState 的 React 19 lint 问题。
- 修复 `Statements/Upload` 中 `setPreviewView` 在声明前访问的问题。
- 修复 Rules 页在 callback 内调用 hook 的问题。
- 拆分 `MoneyText`、`PeriodRangePicker` 中非组件导出，满足 Fast Refresh。
- 修复 Transactions 中 render 阶段写 ref 的问题。

验收：

- `cd frontend && npm run lint` 通过。
- `cd frontend && npm test` 通过。

### P0-2. Transactions 服务端排序

问题：

- Transactions Detail 的列声明了可排序，但 `request` 没有把 sorter 传给后端。

修复：

- 明确支持 Date、Amount、Card、Type 等可排序字段。
- 前端将 ProTable sorter 转成白名单参数。
- 后端只允许白名单字段，避免 SQL 注入。

验收：

- 点击 Date / Amount 排序后，跨分页顺序正确。
- 前端测试覆盖 sorter 参数映射。
- 后端测试覆盖非法 sort 字段被拒绝或忽略。

### P0-3. Cash Risk 年份切换选中日错误

问题：

- `calendarValue` 会把 selected day 显示年份调整到当前 year，但 `selectedKey` 仍使用原始 `selectedDay.format('YYYY-MM-DD')`。
- 切换年份后，右侧 day detail 可能查旧年份。

修复：

- `selectedKey` 应基于 `calendarValue`。
- 年份切换时将 selected day 同步到目标年份。

验收：

- 切换 2025 / 2026 / 2027，右侧 day detail 和日历选中日期一致。

### P0-4. 下钻数据 200 行截断失真

问题：

- `UnifiedDrillDrawer` 使用 `rows: 200` 拉明细后在前端聚合分类/商户，数据量大时 breakdown 不准确。

修复：

- 第一阶段：如果 `total > rows.length`，显示“partial data”提示，避免误导。
- 第二阶段：增加后端 drill breakdown endpoint，直接返回分类/商户聚合和前 200 条交易样本。

验收：

- 大数据量下用户能看到是否被截断。
- 报表 breakdown 金额和对应报表总额一致。

### P0-5. Profile action 路径统一

问题：

- 画像动作仍含旧报表路径。

修复：

- `/reports/income-curve` -> `/reports/cashflow`
- `/reports/income-vs-expense` -> `/reports/cashflow`
- `/reports/category-breakdown` -> `/reports/budget-vs-actual`
- `/reports/category-comparison` -> `/reports/spending-drift`
- `/reports/monthly-comparison` -> `/reports/cashflow`

验收：

- Profile 页面所有 action 都指向当前菜单中可见的目标。
- routes 测试覆盖这些 path。

### P0-6. Profile snapshot 去重

问题：

- 刷新画像会重复插入当天同一维度 snapshot。

修复：

- 为 `user_id + snapshot_date + dimension` 建唯一约束。
- insert 改成 upsert。

验收：

- 同一天多次刷新，历史图每个维度只保留一个点。

## 5. P1 报表体验完善计划

目标：不增加新报表，而是把现有报表统一成专业分析工作流。

### P1-1. 建立统一报表骨架

每个报表统一为：

- Decision KPI：当前最重要的 3-6 个指标。
- Narrative Insight：一句 headline + 2-4 条证据。
- Primary Chart：趋势、贡献或分布。
- Evidence Table：可排序、可下钻、带视觉编码。
- Drill Drawer：分类、商户、交易、动作。
- Data Quality：未分类比例、数据来源、是否 fallback、是否 partial。

验收：

- Cashflow、Budget vs Actual、Spending Drift、Annual Outlook、Trend Changes、Cash Risk、Merchant reports 结构一致。
- 用户点击任何 KPI / 图表点 / 表格行，都能进入相同交互模型。

### P1-2. 升级报表表格视觉化

对 `FsDataTable` 增加可复用的金融表格能力：

- 金额列：正负色、千分位、对齐、单位一致。
- Delta 列：红/绿方向、箭头、百分比和金额同时显示。
- Contribution 列：横向进度条或热力底色。
- Risk 列：low / medium / high tag。
- Forecast 列：actual / forecast / budget / confidence band 标识。
- Row explanation：hover 或展开显示该行为什么重要。

验收：

- Trend Changes movers 表能看出最大驱动项。
- Annual Outlook monthly breakdown 能看出 actual/forecast/deficit/budget gap。
- Merchant Concentration 表能看出 top merchant share 和 subscription 标识。

### P1-3. Annual Outlook 细化

范围：

- 保留现有 year/scenario，不新增独立模块。
- 把已有后端场景参数做成折叠的 scenario inputs：收入变化、一次性支出、新月度账单、目标月供。
- 增加 actual vs forecast vs budget gap 的表格列。
- 分类预测表支持点击下钻到历史分类交易。
- 解释中显示：历史窗口、实际月份数、置信区间方法、使用了哪些用户输入。

验收：

- 调整 `incomeChangePct`、`newMonthlyBill`、`lumpSumExpense` 后，图表和 KPI 明显变化。
- 每个赤字月份能说明主要原因和可执行动作。

### P1-4. Trend Changes 细化

范围：

- 分类 movers 和商户 movers 分区展示，不在同一贡献图里暗示可相加。
- 每条 trend 显示 `from`、`to`、`delta amount`、`delta %`、`contribution %`。
- 生活方式膨胀显示收入增速、支出增速、gap。
- 商户下钻改用标准化 merchant token，而不是 label 关键词。

验收：

- 用户能回答：“今年多花/少花多少钱，主要由哪些分类和商户造成，各自贡献多少。”

### P1-5. Cash Risk 细化

范围：

- 日历风险色和右侧明细完全同步。
- 月度净现金流图加入赤字 threshold line。
- 日 detail 中区分 bill / income / goal / forecast event。
- 高风险日直接给出动作：打开 Planning、调整账单、查看该月交易。

验收：

- 用户能回答：“哪几天危险、为什么危险、我应该做什么。”

### P1-6. Merchant reports 细化

范围：

- Subscriptions：显示可优化金额、最近扣款日、频率稳定性、置信度解释。
- Concentration：显示 top 1 / top 5 share，避免只看金额。
- Drift：分为新增商户、增长商户、下降商户。
- 所有 merchant 行支持统一下钻到交易明细。

验收：

- 用户能从商户报表直接定位底层交易，并决定保留、取消、重分类或加入预算。

## 6. P2 Transactions / Detail 完善计划

目标：把交易明细从“数据表”打磨成“交易分析工作台”。

### P2-1. 表格布局

改进：

- 固定 Date / Transaction / Amount / Actions，减少横向滚动迷失。
- Transaction 单元格分三层：商户/描述主文本、memo 次文本、分类/规则/来源 tag。
- Card 单元格显示银行、账户尾号、卡类型，长文本使用 tooltip。
- Amount 单元格统一显示收入/支出/转账方向和颜色。

验收：

- 1366px 宽度下无需频繁横向滚动即可完成分类和编辑。
- 交易行主信息在 2 秒内可扫读。

### P2-2. 表格数据可视化

改进：

- 金额列增加条形强度或异常标记。
- 分类列显示未分类、规则命中、低置信度状态。
- 固定支出、订阅、转账候选、退款候选用 tag 标识。
- Active filters 区显示当前筛选影响：交易数、收入、支出、净额。

验收：

- 用户不用打开报表，也能在 Detail 页面发现异常支出、未分类、订阅和转账问题。

### P2-3. 交互细化

改进：

- 批量动作前显示选中行数量、收入/支出总额、最早/最晚日期。
- Auto-classify 弹窗显示变更前后分类、置信度、原因和可编辑建议。
- Apply filter / dirty 状态更清楚，避免用户误以为筛选已经生效。
- 双击编辑、保存、取消、错误提示保持一致。

验收：

- 批量改分类和批量转收入/支出的误操作率降低。
- 用户能理解每一次自动分类为什么这样建议。

### P2-4. 与报表下钻打通

改进：

- Transactions 支持从 URL 接收 stable params：`consumeID`、`merchantToken`、`txnTypes`、date range。
- 报表跳转到 Transactions 时保留筛选 chip，并显示“来自哪个 insight”。
- Detail 页面可返回来源报表。

验收：

- 从 Trend Changes 的某个商户 -> Drill -> Transactions，金额和交易集合一致。

## 7. P3 数据挖掘和用户指导完善

目标：把 Profile、Forecast、Trend、Merchant、Transactions 串成一条用户能执行的建议链。

### P3-1. 组合型 insight

输出模板：

- 用户画像：例如 high fixed burden / volatile income / disciplined saver。
- 变化趋势：今年相对去年增加/减少的关键项。
- 未来影响：年度预测中的赤字月份或预算压力。
- 证据：分类、商户、交易样本。
- 动作：调预算、减少订阅、修分类、设置账单/目标。

示例：

> 你当前属于 high fixed burden 型。2026 年固定支出同比增加 ¥X，其中房租和订阅贡献最高；在 stress 场景下 2026-03、2026-04 可能出现赤字。建议先处理 Top 3 recurring charges，并把月度预算上限调整到 ¥Y。

验收：

- Dashboard / Profile / Annual Outlook 至少能出现 3 类组合型建议。
- 每条建议都能下钻到证据。

### P3-2. 预测回测

改进：

- 每个月实际数据完成后，对上一轮 forecast line 做误差计算。
- 输出 MAPE / MAE / bias，按收入、支出、净额、分类预测分开。
- 在 Annual Outlook 显示“本模型过去 3 个月平均误差”。

验收：

- 用户能判断预测是否可信。
- 模型调整有数据依据。

### P3-3. 数据质量贯穿

改进：

- 每个核心报表显示数据质量条：未分类比例、转账候选数、最近导入日期、fallback source。
- 高未分类比例时，报表 insight 降低置信度并引导用户先修分类。

验收：

- 用户知道什么时候应该先清理数据，再相信结论。

## 8. 实施节奏

### Sprint 1：发布卫生和关键 bug

周期：1-2 天
范围：P0-1 到 P0-6
输出：

- 前端 lint 通过。
- Cash Risk 年份 bug 修复。
- Transactions 排序修复。
- Profile action 和 snapshot 修复。
- 下钻 partial data 提示。

### Sprint 2：报表表格和下钻统一

周期：3-5 天
范围：P1-1、P1-2、P1-4、P1-6
输出：

- `FsDataTable` 支持金融视觉编码。
- Trend / Merchant / Generic reports 下钻一致。
- 商户下钻从关键词升级为 stable token。

### Sprint 3：Annual Outlook 和 Cash Risk 精修

周期：3-4 天
范围：P1-3、P1-5
输出：

- 场景输入可调。
- 预测解释更完整。
- 赤字月份和风险日能直接指导行动。

### Sprint 4：Transactions Detail 专业化

周期：4-6 天
范围：P2 全部
输出：

- 表格布局、视觉编码、批量动作、下钻跳转全部升级。
- Detail 成为分析和修复交易数据的主工作台。

### Sprint 5：组合 insight 和模型校准

周期：5-8 天
范围：P3 全部
输出：

- Profile + Forecast + Trend + Merchant 的组合建议。
- 预测回测和数据质量贯穿。

## 9. 验收清单

发布前必须满足：

- `mvn -q test` 通过。
- `mvn -q checkstyle:check` 通过。
- `cd frontend && npm test` 通过。
- `cd frontend && npm run lint` 通过。
- 主要页面桌面端和移动端无重叠、无不可读文本、无空白图表。
- Reports 所有表格行的点击行为一致。
- Transactions Detail 排序、筛选、分页、批量动作一致。
- Annual Outlook 调整场景参数后，KPI、图表、表格同步变化。
- Trend Changes 的分类/商户贡献说明不误导用户。
- Cash Risk 年份切换和 selected day detail 一致。
- Profile 历史同一天不重复污染。

## 10. GitHub 拆分建议

建议拆成以下 issue / PR：

1. `fix(frontend): resolve React 19 lint blockers`
2. `fix(transactions): implement stable server-side table sorting`
3. `fix(reports): correct cash risk selected date when switching year`
4. `fix(profile): normalize action paths and upsert daily snapshots`
5. `feat(reports): add partial-data warning and stable drilldown contract`
6. `feat(ui): add financial visual encoding to report tables`
7. `feat(reports): refine trend and merchant report drilldowns`
8. `feat(reports): expose annual outlook scenario inputs`
9. `feat(transactions): polish detail table layout and risk indicators`
10. `feat(insights): connect profile, trend, forecast, and merchant evidence`

注意：这里的 `feat` 是工程语义，不代表扩展产品范围；实际目标是完善现有功能的表达、交互和可执行性。
