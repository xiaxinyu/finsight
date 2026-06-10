# Technology Stack

## Core Sections (Required)

### 1) Runtime Summary

| Area | Value | Evidence |
|------|-------|----------|
| Primary language | Java 21 | `pom.xml` |
| Runtime + version | Spring Boot 3.5.7 | `pom.xml` |
| Package manager | Maven | `pom.xml` |
| Module/build system | Maven (jar packaging) | `pom.xml` |

### 2) Production Frameworks and Dependencies

| Dependency | Version | Role in system | Evidence |
|------------|---------|----------------|----------|
| `spring-boot-starter-web` | (via Spring Boot BOM 3.5.7) | Web MVC endpoints (controllers) | `pom.xml` |
| `spring-boot-starter-thymeleaf` | (via Spring Boot BOM 3.5.7) | Server-rendered UI templates | `pom.xml`, `src/main/resources/templates/` |
| `spring-boot-starter-security` | (via Spring Boot BOM 3.5.7) | Login + request authentication | `pom.xml`, `src/main/java/com/finsight/web/config/SecurityConfig.java` |
| `spring-boot-starter-actuator` | (via Spring Boot BOM 3.5.7) | Health/metrics endpoints | `pom.xml` |
| MyBatis-Plus Spring Boot starter | 3.5.7 | ORM-ish mapper integration | `pom.xml`, `src/main/resources/application.yml` |
| MySQL Connector/J | 8.0.33 | MySQL JDBC driver | `pom.xml` |
| Fastjson | 1.2.60 | JSON serialization (used in services) | `pom.xml`, `src/main/java/com/finsight/application/transaction/impl/TransactionServiceImpl.java` |
| EasyExcel | 3.3.2 | Excel/CSV style import/export utilities | `pom.xml` |
| Apache PDFBox | 2.0.31 | PDF parsing for statement import | `pom.xml` |
| Apache Lucene | 8.11.2 | Local text indexing/search components | `pom.xml` |
| jQuery EasyUI | vendored | UI widgets/components | `src/main/resources/static/plugins/jquery-easyui-1.11.4/` |
| ECharts | vendored | Charts for reports | `src/main/resources/static/plugins/echarts-6.0.0/` |

### 3) Development Toolchain

| Tool | Purpose | Evidence |
|------|---------|----------|
| Maven Compiler Plugin | Java compilation (source/target 21) | `pom.xml` |
| Spring Boot Maven Plugin | Run/package Spring Boot | `pom.xml` |
| Maven Checkstyle Plugin | Style checks (currently only `UnusedImports`) | `pom.xml`, `checkstyle.xml` |
| Spring Boot Test starter | Test dependency present (tests not detected in repo) | `pom.xml`, `docs/codebase/.codebase-scan.txt` |

### 4) Key Commands

```bash
# install
mvn -q -DskipTests package

# build
mvn clean package

# test
mvn test

# lint (checkstyle is configured as a plugin; exact invocation depends on lifecycle bindings)
mvn checkstyle:check
```

### 5) Environment and Config

- Config sources: `src/main/resources/application.yml`
- Required env vars:
  - `SPRING_DATASOURCE_URL`
  - `SPRING_DATASOURCE_USERNAME`
  - `SPRING_DATASOURCE_PASSWORD`
  - `ACCOUNT_DES_SIGN_KEY`
- Deployment/runtime constraints:
  - Requires a MySQL database reachable by the configured JDBC URL (defaults point to localhost).

### 6) Evidence

- `pom.xml`
- `src/main/resources/application.yml`
- `checkstyle.xml`
- `src/main/resources/templates/`
- `src/main/resources/static/plugins/`
- `docs/codebase/.codebase-scan.txt`

