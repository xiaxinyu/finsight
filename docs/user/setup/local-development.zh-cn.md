# 本地开发环境

| | |
| :--- | :--- |
| **Language** | 简体中文 · [English](local-development.md) |

在本地运行 FinSight 后端与可选前端 dev server。

---

## 前置条件

| 组件 | 版本 |
| :--- | :--- |
| JDK | 21+ |
| Maven | 3.9+ |
| MySQL | 8.x（已创建库，如 `finsight`） |
| Node.js（仅前端热更新） | 20+ |

---

## 操作步骤

1. 配置数据库 environment variables：

   | 变量 | 说明 |
   | :--- | :--- |
   | `SPRING_DATASOURCE_URL` | JDBC URL |
   | `SPRING_DATASOURCE_USERNAME` | 用户名 |
   | `SPRING_DATASOURCE_PASSWORD` | 密码 |
   | `ACCOUNT_DES_SIGN_KEY` | 敏感字段加密 key |

2. 启动后端：

   ```bash
   mvn spring-boot:run
   ```

3. 可选 — 前端热更新：

   ```bash
   cd frontend && npm run dev
   ```

4. 浏览器访问：
   - 打包 UI：`http://localhost:8080/app/login`
   - Dev server：`http://localhost:5173/app/`

5. Flyway 将在启动时自动迁移 schema。

---

## 验收

| 检查项 | 预期 |
| :--- | :--- |
| 日志 | `Tomcat started on port(s): 8080` |
| 登录页 | 可打开 `/app/login` |
| API | 登录后可访问 Dashboard |

---

## 生产打包

```bash
mvn clean package
```

SPA 输出至 `src/main/resources/static/app/`。

---

## 清理（可选）

终止 `mvn spring-boot:run` 进程（Ctrl+C）。

---

## 关联文档

- [getting-started.zh-cn.md](../concepts/getting-started.zh-cn.md)  
- [technical.zh-cn.md](../../tech/architecture/technical.zh-cn.md)
