# Profile 性能排查 Runbook

适用于 v2.0.0+ 的 Profile / Advisor / Forecast 读路径性能问题。

## 1. 确认症状

- MySQL CPU 在刷新 Profile 或 Dashboard 时持续 >50%
- `/api/v1/analytics/profile` 响应 >800ms（warm cache）或 >2s（cold）
- `fin_profile_snapshot` / `fin_forecast_run` / `fin_insight_card` 行数异常增长

## 2. 检查缓存是否命中

应用日志（DEBUG）：

```text
analytics.profile cacheHit=true|false
analytics.advisor cacheHit=true|false
analytics.forecast cacheHit=true|false
```

配置 TTL（`application.yml`）：

```yaml
finsight:
  analytics:
    profile-cache-ttl-seconds: 600
    advisor-cache-ttl-seconds: 600
    forecast-cache-ttl-seconds: 600
```

## 3. 慢查询

启用 MySQL slow query log，过滤 Profile 相关 SQL：

```sql
-- 不应出现在 WHERE 子句
-- date_format(v.txn_date, '%Y-%m')

-- 期望使用范围条件
-- v.txn_date >= ? AND v.txn_date < ?
```

Explain 示例：

```sql
EXPLAIN SELECT v.category_code, sum(v.amount)
FROM v_transaction_analytics v
INNER JOIN transaction t ON t.id = v.id
WHERE v.txn_date >= '2025-07-01' AND v.txn_date < '2026-07-01'
  AND t.created_by = 'your_user';
```

确认索引 `idx_txn_owner_deleted_date` 存在（Flyway V30）。

## 4. GET 写副作用（v2.0.0 已修复）

| 端点 | 预期行为 |
|------|----------|
| GET `/api/v1/analytics/profile` | 只读，不写 `fin_profile_snapshot` |
| GET `/api/v1/advisor/recommendations` | 只读，不写 `fin_insight_card` |
| GET `/api/v1/analytics/forecast` | preview，`runId` 以 `preview-` 开头 |
| POST `/api/v1/analytics/scenarios` | 可持久化 forecast run |

手动刷新 Profile 快照（运维）：

```java
// FinancialProfileService.refreshProfileSnapshots()
```

## 5. 前端重复请求

React Query 默认 `staleTime: 600_000`（10 分钟）。Network 面板中 Profile + Advisor 不应在短时间内重复触发相同重型 API。

## 6. 回滚

若 v2.0.0 性能改动引入问题：

1. 将 `finsight.analytics.*-cache-ttl-seconds` 设为 `60` 强制更频繁刷新
2. 回滚到 v1.9.0 tag
3. 数据库 V30 索引可保留（仅 additive）
