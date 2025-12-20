# 技术文档

语言: 中文 | [English](TECHNICAL.md)

## 🏗 项目结构

```text
finsight/
├── docs/                 # 文档（功能、品牌、宣言）
├── src/
│   ├── main/
│   │   ├── java/com/finsight/
│   │   │   ├── application/    # 业务逻辑与服务
│   │   │   ├── domain/         # 核心领域模型
│   │   │   ├── infrastructure/ # 数据访问（Mapper）
│   │   │   └── web/            # REST 控制器与视图
│   │   └── resources/
│   │       ├── mapper/         # MyBatis XML 映射
│   │       ├── static/         # 前端资源（CSS、JS）
│   │       └── templates/      # Thymeleaf 模板
├── pom.xml               # Maven 依赖
└── README.md
```

## 🛠 技术栈

- **后端**：Spring Boot 3，Java 21
- **持久化**：MyBatis-Plus，MySQL 8.x
- **前端**：Thymeleaf，EasyUI，ECharts
- **构建工具**：Maven 3.9+

## 🚀 快速开始

### 环境准备
- JDK `21`
- Maven `3.9+`
- MySQL `8.x`（创建数据库 `finsight`）

### 安装与配置

1. **克隆仓库**
   ```bash
   git clone https://github.com/yourname/finsight.git
   cd finsight
   ```

2. **配置数据库**
   编辑 `src/main/resources/application.yml` 并更新数据库连接：
   ```yaml
   spring:
     datasource:
       url: jdbc:mysql://127.0.0.1:3306/finsight?...
       username: your_username
       password: your_password
   ```

3. **构建项目**
   ```bash
   mvn clean package -DskipTests
   ```

4. **运行应用**
   ```bash
   java -jar target/finsight-1.0.0.jar
   ```

### 使用
1. 打开浏览器访问 `http://localhost:8080/index.html`。
2. 上传你的对账单文件（如适用）或浏览演示数据。

> 🔐 隐私提示：切勿提交真实交易数据或包含密码的配置文件到 Git。请使用 `.gitignore`。

## 🤝 贡献指南

欢迎贡献！请遵循以下步骤：

1. **提交 Issue**：在实现前讨论你的变更。
2. **代码风格**：遵循现有的 Java/Spring Boot 约定。
3. **测试**：尽可能为新功能添加单元测试。
4. **拉取请求**：提交包含清晰变更说明的 PR。