# v1.9.0 已知问题与 v2.0.0 关闭状态

本文档对应 [v2.0.0 稳定化计划](./roadmap/v2.0.0-stability-quality-plan.zh-cn.md) §4.7 要求。

## 已关闭（v2.0.0 代码 + 文档）

| 问题 | v1.9.0 表现 | v2.0.0 处理 |
|------|-------------|-------------|
| Profile GET 写快照 | 每次刷新写 10 条 `fin_profile_snapshot` | GET 只读；`POST /api/v1/analytics/profile/snapshots/refresh` 或定时任务 |
| Advisor 全量实时计算 | 每次请求串联 Profile/Trend/Forecast/Merchant | TTL 缓存 + 请求 memo + `CombinedInsightContext` |
| Forecast GET 持久化 | 每次 GET 新建 `fin_forecast_run` | GET preview；`POST /scenarios` 才 persist |
| metrics gate inline report SQL | CPU 飙高 | 读路径 degraded 警告 + 后台 `refreshAsync` |
| concentration `date_format` WHERE | 无法走索引 | 改为 `txn_date` 范围 + V30 索引 |
| 前端重复拉取重型 API | staleTime=0 | 全局 10min + `QUERY_KEYS` |
| Profile UI 移动端过长 | Radar 难用 | 三层 IA + 条形列表 + compact Insight |

## 部分关闭 / 需运维验证

| 问题 | 状态 | 验证方式 |
|------|------|----------|
| Profile P95 < 800ms | 需生产压测 | runbook + 慢查询脚本 |
| MySQL CPU 刷新 <50% | 需生产压测 | 连续刷新 20 次观察 |
| 5 视口 UI 无溢出 | 手工 + 可选 Playwright | DevTools 360–1440；`npm run test:e2e` 仅本地 |
| 每日快照一次 | 默认 scheduler **关闭** | 设 `finsight.analytics.profile-snapshot-scheduler-enabled=true` |

## 转入 v2.0.1（不阻塞 GA 若接受）

| 问题 | 说明 |
|------|------|
| `MetricSnapshot` 强类型 DTO | 仍使用 Map 传递 |
| view 热点预聚合 | 仍用 `v_transaction_analytics` |
| Annual Outlook POST 仍 persist forecast | 设计为显式 scenario 保存 |
| 多用户 wealth/cashflow 与 batch snapshot | 本地单用户场景可接受 |

## 运维脚本

- 慢查询 / explain：`scripts/db/profile-slow-query-audit.sql`
- Forecast 清理：`scripts/db/cleanup-forecast-runs.sql`
- 发布清单：`docs/tech/ops/v2.0.0-release-checklist.zh-cn.md`
