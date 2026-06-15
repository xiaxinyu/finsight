# FinSight v1.7.0 发布后行动计划

日期：2026-06-12  
目标版本：v1.8.0 / v1.9.0  
定位：把 v1.7.0 的顾问式能力 MVP 打磨成可解释、可预测、可行动的专业个人金融分析系统。

## 1. 当前状态

v1.7.0 已经完成从“交易与报表工具”到“顾问式财务工作台雏形”的第一步：

- 已发布 v1.7.0 Release。
- 已有交易导入、预览、提交、重复检测、跳过行诊断。
- 已有交易分类、批量重分类、未分类处理、转账标记。
- 已有 Dashboard、Cashflow、Budget vs Actual、Fund Flow、Fixed vs Variable、Spending Drift、Bills Calendar。
- 已新增 Advisor 方向能力：Financial Profile、Annual Outlook、Trend Changes、Cash Risk、Advisor Recommendations。
- 已新增持久化基础表：budget、budget line、bill、goal、account snapshot、monthly metrics、profile snapshot、forecast、insight card、recommendation feedback、merchant profile。
- 已新增 `v_transaction_analytics` v2，提供方向、金额、分类、商户、退款、固定支出等语义字段。

验证结果：

- `cd frontend && npm test`：通过，13 个测试文件，49 个测试。
- `mvn test`：通过，56 个测试，1 个 Flyway/Testcontainers 集成测试因本机 Docker 不可用被跳过。
- `mvn checkstyle:check`：失败，6 个 unused import。
- `cd frontend && npm run lint`：失败，ESLint 10 的 stylish formatter 调用 `util.styleText`，当前 Node 运行时不兼容。

## 2. 关键判断

项目现在不是“缺少方向”，而是处在一个典型的 MVP 之后阶段：

1. 数据层已经开始补齐，但指标、画像、预测的准确性还需要校准。
2. 后端已经有 Profile / Forecast / Recommendations，但模型仍偏启发式，需要增强金融语义和证据链。
3. 前端已经有 Profile 页面和预测报表，但交互深度不足，还没有完整的“问题 -> 结论 -> 证据 -> 下钻 -> 行动 -> 反馈”闭环。
4. 工程质量需要在继续扩功能前先拉齐：checkstyle、lint、测试、迁移校验、Feature Flag、错误处理。

## 3. P0 Bug / 发布阻塞项

这些问题应优先修复，目标是在继续开发新功能前让 main 保持可发布状态。

### P0-1. 修复 Checkstyle 失败

现象：

- `mvn checkstyle:check` 失败。

已确认文件：

- `src/main/java/com/finsight/web/api/analytics/AdvisorApiController.java`
- `src/main/java/com/finsight/application/query/TransactionQuerySupport.java`
- `src/main/java/com/finsight/application/analytics/ForecastService.java`
- `src/main/java/com/finsight/application/analytics/LocalAiAdvisorService.java`
- `src/main/java/com/finsight/application/analytics/FinancialProfileService.java`
- `src/main/java/com/finsight/application/analytics/TrendAnalysisService.java`

修复：

- 删除 unused imports。
- CI 中加入 `mvn checkstyle:check`。

验收：

- `mvn test` 通过。
- `mvn checkstyle:check` 通过。

### P0-2. 修复前端 lint 运行环境

现象：

- `npm run lint` 因 `util.styleText is not a function` 失败。

可能原因：

- 当前 Node 运行时与 ESLint 10 formatter API 不兼容。
- Maven 配置中使用 Node 22.14.0，但本地 shell 实际运行 Node 20.x。

修复方案：

- 优先统一前端开发 Node 版本到 `v22.14.0`。
- 增加 `.nvmrc` 或 Volta/packageManager 声明。
- 或临时将 lint formatter 改为兼容格式，例如 `eslint . --format unix`。

验收：

- `cd frontend && npm run lint` 可执行，并能真实报告代码问题。

### P0-3. 修复 ProfilePage Hooks 顺序风险

现象：

- `frontend/src/pages/Profile/index.tsx` 在条件 return 后调用 `useMemo`。
- React Hook 必须在每次 render 中以相同顺序调用，否则加载态切换后可能触发 hooks order 错误。

修复：

- 将 `useMemo` 移到所有条件 return 之前。
- 当 `data` 为空时返回默认空图表配置。

验收：

- Profile 页面刷新、加载、失败、成功状态均无 React hooks 警告。
- 增加最小组件测试或静态 lint 覆盖。

### P0-4. ForecastService 预测结果未持久化明细

现象：

- `fin_forecast_run` 已写入，但 `fin_forecast_line` 没有写入。

影响：

- 无法审计历史预测。
- 无法对比“当时预测 vs 后来实际”。
- 预测模型质量无法评估。

修复：

- 每次 forecast run 写入 12 个月 `fin_forecast_line`。
- 存储 `INCOME_FORECAST`、`EXPENSE_FORECAST`、`NET_FORECAST`，并预留 lower/upper bounds。

验收：

- 一次 forecast 生成 36 条 line。
- 可查询任意 run 的预测明细。

### P0-5. RecommendationService 可能重复插入 insight card

现象：

- 推荐卡片 id 使用稳定业务 id，例如 `cashflow_risk`、profile dimension id。
- `persistCards` 每次插入 `fin_insight_card`，如果重复 id 存在可能失败，或者产生不一致行为。

修复：

- 使用 `insert ... on duplicate key update`。
- 或 card id 改为 `userId + type + date`，并定义唯一键。
- 推荐服务应区分“当前活跃建议”和“历史建议日志”。

验收：

- 多次刷新 Dashboard 不产生异常。
- 推荐历史可追踪，当前建议不重复污染。

## 4. P1 功能优化需求

### P1-1. 个人财务画像升级

当前状态：

- 已有 10 维画像：收入稳定性、支出控制、储蓄纪律、固定成本、流动性、债务压力、生活方式通胀、消费集中度、季节性风险、数据可信度。

不足：

- 部分维度仍是占位或粗略算法，例如 spending concentration 固定返回默认分。
- evidence 过大，直接塞入 metrics 列表，不够用户友好。
- userType 只有少量类型，不能准确表达用户画像。

需求：

- 为每个画像维度定义明确公式、阈值、证据、行动。
- 增加画像类型：
  - disciplined_saver
  - high_fixed_burden
  - cashflow_stressed
  - volatile_income
  - lifestyle_inflation
  - debt_pressure
  - data_quality_risk
  - balanced
- 为每个画像输出一句用户可理解的说明。

验收：

- Profile 页面每个维度都有：分数、状态、原因、关键证据、可执行动作。
- 用户能理解“为什么我被归为这个类型”。

### P1-2. 年度预测增强

当前状态：

- `ForecastService` 使用 rolling average + 简单季节系数。
- 支持 base / conservative / optimistic / stress 场景。

不足：

- 预测未区分历史实际、预算目标、账单、目标月供。
- 没有置信区间。
- 没有分类级预测。
- `lumpSumExpense` 等输入参数尚未真正作用到结果。

需求：

- 预测输入：
  - 最近 12/24 个月实际收支。
  - 已知账单。
  - 预算线。
  - 目标月供。
  - 一次性支出。
  - 收入变化百分比。
- 预测输出：
  - 年收入、年支出、年净结余。
  - 月度收入、支出、结余。
  - 赤字月份。
  - 置信区间。
  - 分类支出预测。
  - 预测解释。

验收：

- `/api/v1/analytics/scenarios` 中的 `incomeChangePct`、`newMonthlyBill`、`lumpSumExpense` 都会改变预测结果。
- Annual Outlook 能展示实际值、预测值、预算目标。

### P1-3. 趋势变化解释

当前状态：

- Trend Changes 能展示类别增长。

不足：

- 趋势解释偏少。
- 没有商户层、固定/可变层、收入稳定性层。

需求：

- 增加趋势类型：
  - YoY 收入变化。
  - YoY 支出变化。
  - 储蓄率变化。
  - 固定成本变化。
  - Top category movers。
  - Top merchant movers。
  - 生活方式通胀检测。
- 每条趋势必须输出：
  - delta amount
  - delta percent
  - contribution to total change
  - related transactions drill-down params

验收：

- 用户能看到“今年支出多了多少，主要是谁造成的”。
- 趋势结果可下钻到分类、商户、交易。

### P1-4. Advisor 推荐闭环

当前状态：

- Dashboard 已接入 advisor recommendations。
- 支持 feedback dismiss。

不足：

- accept/snooze 的行为语义不完整。
- 推荐证据没有在 UI 展开。
- impactAmount 大多为 0。
- 推荐卡片没有明确紧急程度。

需求：

- 推荐字段标准化：
  - priority
  - urgency
  - impactAmount
  - confidence
  - reason
  - evidenceRefs
  - actions
  - expiresAt
- UI 支持：
  - 查看证据。
  - 接受建议。
  - 暂缓提醒。
  - 忽略 7 天。
  - 跳转到相关报表或交易。

验收：

- 首页 3 张卡片均能说明“为什么重要、影响多少钱、下一步做什么”。

### P1-5. 商户和订阅挖掘

当前状态：

- 已有 `MerchantMiningService` 和 `fin_merchant_profile`。

不足：

- 订阅识别规则偏粗，只看交易次数。
- 商户归一化还依赖原始 token。

需求：

- 商户归一化：
  - 去除订单号、门店号、支付通道噪声。
  - 合并同一商户的不同描述。
- 订阅识别：
  - 固定周期。
  - 金额稳定。
  - 商户重复。
  - 自动标记 suspected_subscription。
- 新增报表：
  - Subscriptions
  - Merchant Concentration
  - Merchant Drift

验收：

- 能识别每月/每季度重复扣费。
- 能展示订阅总额和可优化金额。

## 5. P2 交互和体验优化

### P2-1. Profile 页面增强

- 增加画像历史趋势。
- 增加维度详情抽屉。
- 展示 evidence 而不是只给 summary。
- 将 action 链接指向精确页面和过滤条件。

### P2-2. Annual Outlook 页面增强

- 支持 scenario 切换。
- 显示置信区间。
- 显示赤字月份高亮。
- 支持“把预测转成预算建议”。

### P2-3. Cash Risk 页面增强

- 以月历形式展示现金压力。
- 展示账单日、收入日、目标扣款日。
- 支持调整参数立即重算。

### P2-4. Drill-down 统一

- 所有图表点击都进入统一的 drill-down drawer。
- 第一层：指标解释。
- 第二层：分类 / 商户贡献。
- 第三层：交易明细。
- 第四层：行动按钮，例如调整预算、创建规则、标记转账、忽略建议。

## 6. P3 工程和质量优化

### P3-1. CI/CD

新增 GitHub Actions：

- backend-test：`mvn test`
- backend-style：`mvn checkstyle:check`
- frontend-test：`cd frontend && npm test`
- frontend-lint：`cd frontend && npm run lint`
- frontend-build：`cd frontend && npm run build`

### P3-2. 数据迁移验证

- Flyway migration 必须能在 CI 中通过。
- 如果没有 Docker，提供 H2 或 MySQL service container。
- 保留 `/api/v1/maintenance/verify-schema-migration` 作为运行时检查。

### P3-3. Feature Flag

- Advisor、Profile、Forecast、Local AI、Merchant Mining 都需要 feature flag。
- 默认开启稳定功能，实验功能可关闭。

### P3-4. 安全和生产配置

- 检查 CSRF 策略。
- 限制 `/actuator/**` 暴露。
- 限制 `/encrypt/**` 仅管理员或本地初始化使用。
- 生产环境禁止默认密钥和默认数据库密码。

## 7. GitHub Issues 拆分建议

建议创建以下 labels：

- `type:bug`
- `type:feature`
- `type:tech-debt`
- `area:advisor`
- `area:analytics`
- `area:frontend`
- `area:backend`
- `area:data`
- `priority:p0`
- `priority:p1`
- `priority:p2`

建议创建 issues：

1. `[P0][Backend] Fix Checkstyle unused imports`
2. `[P0][Frontend] Fix ESLint runtime compatibility`
3. `[P0][Frontend] Fix ProfilePage hook order`
4. `[P0][Analytics] Persist forecast lines for every forecast run`
5. `[P0][Advisor] Make insight card persistence idempotent`
6. `[P1][Profile] Define formulas and evidence for all profile dimensions`
7. `[P1][Forecast] Apply scenario input parameters to forecast output`
8. `[P1][Forecast] Add confidence intervals and category forecasts`
9. `[P1][Trends] Add merchant and fixed-cost trend decomposition`
10. `[P1][Advisor] Add evidence drawer and accept/snooze feedback`
11. `[P1][Merchant] Improve merchant normalization and subscription detection`
12. `[P2][Reports] Add scenario switcher to Annual Outlook`
13. `[P2][Reports] Add cash-risk calendar view`
14. `[P2][UX] Standardize drill-down flow across reports`
15. `[P3][CI] Add GitHub Actions for backend/frontend validation`
16. `[P3][Security] Harden production security defaults`

## 8. 建议开发顺序

### Sprint 1：恢复工程健康

目标：main 分支可验证、可持续发布。

- 修复 Checkstyle。
- 修复 lint 运行环境。
- 修复 ProfilePage hook 顺序。
- 增加 GitHub Actions。
- 补 Profile / Forecast / Advisor 关键单测。

完成标准：

- `mvn test`
- `mvn checkstyle:check`
- `cd frontend && npm test`
- `cd frontend && npm run lint`
- `cd frontend && npm run build`

### Sprint 2：让预测可信

目标：Annual Outlook 可以指导用户看全年现金流。

- forecast lines 持久化。
- scenario 参数真正生效。
- 加入预算、账单、目标月供。
- 加入 deficit month 解释。
- Annual Outlook 支持 scenario 切换。

完成标准：

- 用户能看到全年预计收入、支出、结余。
- 用户能知道哪些月份可能现金流为负。
- 用户能调整场景并看到变化。

### Sprint 3：让画像可解释

目标：Profile 不只是分数，而是用户能理解的财务画像。

- 完善 10 维画像公式。
- 增加 user type 分类。
- 增加证据摘要。
- Profile UI 增加详情抽屉。
- 画像历史趋势。

完成标准：

- 每个维度都有公式、状态、证据、行动。
- 用户知道自己的核心财务问题是什么。

### Sprint 4：让建议可行动

目标：首页成为“今日财务任务台”。

- 推荐卡片加 urgency、confidence、impactAmount。
- Evidence drawer。
- accept / snooze / dismiss 行为完整化。
- 建议跳转到精确 drill-down。
- 根据用户反馈降低重复打扰。

完成标准：

- 用户打开 Dashboard 能看到 3 个最重要的行动。
- 每个行动都可解释、可执行、可反馈。

### Sprint 5：商户和订阅挖掘

目标：从分类分析进入商户级数据挖掘。

- 商户归一化。
- 周期性扣费识别。
- 订阅报告。
- 商户支出漂移。
- 推荐取消或审查异常订阅。

完成标准：

- 用户能看到每月订阅总额。
- 用户能发现重复扣费和异常商户增长。

## 9. v1.8.0 建议范围

建议 v1.8.0 不要追求“大而全 AI”，而是聚焦三件事：

1. 工程健康：测试、lint、checkstyle、CI 全绿。
2. 可信预测：Annual Outlook / Cash Risk 可用。
3. 可解释画像：Profile 能真正说明用户财务类型和风险。

建议 v1.9.0 再重点做：

1. Advisor 推荐闭环。
2. 商户和订阅挖掘。
3. 本地 AI 问答。
4. 更完整的 Scenario Lab。

