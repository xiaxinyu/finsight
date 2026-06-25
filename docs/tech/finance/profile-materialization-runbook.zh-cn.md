# Profile 物化运行手册（v2.0.2）

## 行为

- **GET** `/api/v1/analytics/profile`：只读 `fin_profile_current`，不触发全量计算。
- **POST** `/api/v1/analytics/profile/refresh`：显式重算，写入 `fin_profile_current` 与 `fin_profile_snapshot` 日历史。
- 交易导入、分类变更、规则应用后：`AnalyticsCacheInvalidationService` 将 profile 标记为 `stale=1`，不自动重算。

## 运维

```bash
# 触发当前用户 refresh（需登录会话）
curl -X POST -b cookies.txt /api/v1/analytics/profile/refresh

# 查看物化行
select user_id, stale, computed_at, overall_score from fin_profile_current where user_id = 'alice';
```

## 性能目标

- profile-read P95 &lt; 200ms（物化命中）
- profile-refresh P95 &lt; 3s

## 故障处理

| 现象 | 处理 |
| --- | --- |
| 页面显示 needsRefresh | 用户点击 Generate/Refresh |
| refresh 返回 busy | 等待进行中的计算完成 |
| refresh 失败 | 旧物化结果保留，查应用日志 |

详见 [finance-semantic-contract.zh-cn.md](../finance/finance-semantic-contract.zh-cn.md)。
