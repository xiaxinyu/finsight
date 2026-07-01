# FinSight 安全指南

| | |
| :--- | :--- |
| **Language** | 简体中文 · [English](SECURITY.md) |

自建部署 FinSight 的安全控制说明。

---

## 1. 认证与登录

| 控制项 | 实现 |
| :--- | :--- |
| 密码存储 | BCrypt（Spring Security） |
| 会话 | `JSESSIONID` Cookie，空闲 30 分钟超时 |
| 暴力破解防护 | `LoginRateLimitFilter` — 每 IP 15 分钟内 8 次失败（可配置） |
| 用户名枚举 | 统一错误提示；`hideUserNotFoundExceptions` |
| 登出 | 销毁会话；清除 `JSESSIONID`、`XSRF-TOKEN` |

**API：** `GET /api/v1/auth/me` 返回 `{ authenticated, username, roles, admin }`。

---

## 2. 授权

| 路径 | 要求 |
| :--- | :--- |
| `/api/v1/users/**` | `ROLE_ADMIN` |
| `/api/v1/maintenance/**` | `ROLE_ADMIN` |
| 卡号 / 分类 / 规则管理写操作 | `ROLE_ADMIN` |
| 其他 `/api/v1/**` | 已登录用户 |

角色表：`fs_role` / `fs_user_role`。迁移 **V50** 初始化 `ADMIN` / `USER`，并为现有用户授予 `ADMIN`（升级友好）。

前端：`/auth/me` 中 `admin: true` 才显示 **Admin** 菜单与路由。

---

## 3. CSRF（生产环境）

`finsight.security.csrf-enabled=true`（`application-prod.yml` 默认）时：

- Cookie `XSRF-TOKEN`（SPA 可读）
- 写请求携带 `X-XSRF-TOKEN` 头
- 预取：`GET /api/v1/auth/csrf`

开发环境默认关闭 CSRF，便于本地调试。

---

## 4. 数据保护

| 数据 | 策略 |
| :--- | :--- |
| 卡号（PAN） | 日志脱敏 `****1234`；非 Admin 的列表 API 返回掩码卡号 |
| 密码哈希 | `GET /api/v1/users` 不返回 |
| 签名密钥 | 启动日志仅 `(configured, length=N)` |
| CVV | 不采集、不存储 |

生产环境必须设置强 `ACCOUNT_DES_SIGN_KEY`（`ProdStartupValidator` 拦截默认值）。

---

## 5. 传输与响应头（生产）

- Session Cookie：`Secure` · `HttpOnly` · `SameSite=Strict`
- HSTS（prod）
- `X-Frame-Options: SAMEORIGIN`
- `/actuator/health` 默认需认证
- prod 下 `/encrypt/**` 拒绝访问

---

## 6. 配置项

| 属性 | 开发默认 | 生产 |
| :--- | :--- | :--- |
| `finsight.security.csrf-enabled` | `false` | `true` |
| `finsight.security.actuator-public` | `true` | `false` |
| `finsight.security.login-max-attempts` | `8` | `8` |
| `finsight.security.login-lockout-seconds` | `900` | `900` |

环境变量：`SPRING_DATASOURCE_*`、`ACCOUNT_DES_SIGN_KEY`。

---

## 7. 验收清单

- [ ] 生产密钥非开发默认值
- [ ] 非 Admin 无法访问 `/admin/*` 与 `/api/v1/users`
- [ ] 连续登录失败后触发限流
- [ ] `/api/**` 未授权返回 JSON 401/403
- [ ] 非 Admin 卡号列表仅显示掩码
- [ ] 生产环境 SPA 写操作在 CSRF 开启下正常

参见 [FEATURE_FLAGS.md](FEATURE_FLAGS.md)
