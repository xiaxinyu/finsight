# Technical Documentation

| | |
| :--- | :--- |
| **Language** | English · [简体中文](technical.zh-cn.md) |

This document is for **operators and developers** who deploy, configure, or extend FinSight.

- Product positioning and feature narrative: [`docs/user/concepts/product-guide.md`](../../user/concepts/product-guide.md)
- Strategy and roadmap: [`docs/user/concepts/overview.md`](../../user/concepts/overview.md)

---

## Repository layout

```text
finsight/
├── docs/                 # Product, strategy, engineering docs
├── src/main/java/com/finsight/
│   ├── application/      # Use cases, services, importers, classification
│   ├── domain/           # Domain models
│   ├── infrastructure/   # MyBatis mappers, adapters
│   └── web/              # Controllers, REST models, Thymeleaf views
├── src/main/resources/
│   ├── mapper/           # MyBatis XML
│   ├── static/           # CSS, JS
│   └── templates/      # Thymeleaf HTML
├── pom.xml
└── README.md
```

---

## Stack

| Layer | Technology |
| :--- | :--- |
| Runtime | Java **21**, Spring Boot **3.x** |
| Data | MyBatis-Plus, **MySQL 8.x** |
| UI | Thymeleaf, jQuery EasyUI, ECharts |
| Build | Maven **3.9+** |

---

## Prerequisites

- JDK **21**
- Maven **3.9+**
- MySQL **8.x** with a database (e.g. `finsight`) created for the app

---

## Configuration

`src/main/resources/application.yml` supports **environment-variable overrides** for sensitive and environment-specific values. For production, set at least:

| Variable | Purpose |
| :--- | :--- |
| `SPRING_DATASOURCE_URL` | JDBC URL |
| `SPRING_DATASOURCE_USERNAME` | DB user |
| `SPRING_DATASOURCE_PASSWORD` | DB password |
| `ACCOUNT_DES_SIGN_KEY` | Signing key for account-related crypto (replace default) |

Defaults in the file are for **local development only**. Do not commit real credentials.

---

## Build and run

```bash
git clone <your-repository-url>
cd finsight
mvn clean package -DskipTests
java -jar target/finsight-1.6.0.jar
```

Alternatively:

```bash
mvn spring-boot:run
```

Then open `http://localhost:8080/index.html` (adjust host/port if configured).

---

## Security and data hygiene

- Keep secrets out of Git; use env vars or your platform’s secret store.
- Do not commit exports of real transaction data.

---

## Contributing

1. Discuss non-trivial changes via an issue or short design note.
2. Match existing Java/Spring style; keep changes focused.
3. Add or update tests when behavior changes.
4. Open a PR with a clear summary and risk notes.
