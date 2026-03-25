# 技术文档

| | |
| :--- | :--- |
| **语言** | 简体中文 · [English](TECHNICAL.md) |

本文面向 **运维与研发**：部署、配置与二次开发。产品叙述见 [产品指南](PRODUCT_GUIDE.zh-CN.md)；战略与路线图见 [文档索引](README.zh-CN.md)。

---

## 代码结构

```text
finsight/
├── docs/                 # 产品、战略、工程文档
├── src/main/java/com/finsight/
│   ├── application/      # 用例、服务、导入、分类等
│   ├── domain/           # 领域模型
│   ├── infrastructure/   # MyBatis 等基础设施
│   └── web/              # 控制器、REST 模型、Thymeleaf 视图
├── src/main/resources/
│   ├── mapper/           # MyBatis XML
│   ├── static/           # CSS、JS
│   └── templates/        # Thymeleaf 页面
├── pom.xml
└── README.md
```

---

## 技术栈

| 层次 | 技术 |
| :--- | :--- |
| 运行时 | Java **21**，Spring Boot **3.x** |
| 数据 | MyBatis-Plus，**MySQL 8.x** |
| 前端 | Thymeleaf，jQuery EasyUI，ECharts |
| 构建 | Maven **3.9+** |

---

## 环境要求

- JDK **21**
- Maven **3.9+**
- MySQL **8.x**，并创建应用使用的库（如 `finsight`）

---

## 配置说明

`src/main/resources/application.yml` 中敏感项可通过 **环境变量** 覆盖。生产环境建议至少配置：

| 变量 | 用途 |
| :--- | :--- |
| `SPRING_DATASOURCE_URL` | JDBC 连接串 |
| `SPRING_DATASOURCE_USERNAME` | 数据库用户名 |
| `SPRING_DATASOURCE_PASSWORD` | 数据库密码 |
| `ACCOUNT_DES_SIGN_KEY` | 账户相关加签密钥（务必替换默认值） |

文件内默认值仅供 **本地开发**；勿将真实凭据提交到版本库。

---

## 构建与运行

```bash
git clone <你的仓库地址>
cd finsight
mvn clean package -DskipTests
java -jar target/finsight-1.6.0.jar
```

或使用：

```bash
mvn spring-boot:run
```

浏览器访问 `http://localhost:8080/index.html`（若修改了端口或上下文路径请相应调整）。

---

## 安全与数据

- 密钥使用环境变量或平台密钥管理，勿写入仓库。
- 勿将真实交易数据导出文件纳入版本控制。

---

## 贡献方式

1. 较大改动先通过 Issue 或简短设计说明对齐。
2. 遵循现有 Java/Spring 风格，改动保持聚焦。
3. 行为变更时补充或更新测试。
4. 提交 PR 并写清摘要与风险点。
