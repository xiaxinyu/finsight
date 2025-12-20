# Technical Documentation
Language: English | [中文](TECHNICAL.zh-CN.md)

## 🏗 Project Structure

```text
finsight/
├── docs/                 # Documentation (Features, Branding, Manifesto)
├── src/
│   ├── main/
│   │   ├── java/com/finsight/
│   │   │   ├── application/    # Business Logic & Services
│   │   │   ├── domain/         # Core Domain Models
│   │   │   ├── infrastructure/ # Data Access (Mappers)
│   │   │   └── web/            # REST Controllers & Views
│   │   └── resources/
│   │       ├── mapper/         # MyBatis XML Mappers
│   │       ├── static/         # Frontend Assets (CSS, JS)
│   │       └── templates/      # Thymeleaf HTML Templates
├── pom.xml               # Maven Dependencies
└── README.md
```

## 🛠 Technology Stack

- **Backend**: Spring Boot 3, Java 21
- **Persistence**: MyBatis-Plus, MySQL 8.x
- **Frontend**: Thymeleaf, EasyUI, ECharts
- **Build Tool**: Maven 3.9+

## 🚀 Getting Started

### Prerequisites
- JDK `21`
- Maven `3.9+`
- MySQL `8.x` (Create database `finsight`)

### Installation & Setup

1. **Clone the repository**
   ```bash
   git clone https://github.com/yourname/finsight.git
   cd finsight
   ```

2. **Configure Database**
   Edit `src/main/resources/application.yml` and update the database connection details:
   ```yaml
   spring:
     datasource:
       url: jdbc:mysql://127.0.0.1:3306/finsight?...
       username: your_username
       password: your_password
   ```

3. **Build the project**
   ```bash
   mvn clean package -DskipTests
   ```

4. **Run the application**
   ```bash
   java -jar target/finsight-1.0.0.jar
   ```

### Usage
1. Open your browser and navigate to `http://localhost:8080/index.html`.
2. Upload your statement files (if applicable) or explore the demo data.

> 🔐 **Privacy Note**: Never commit real transaction data or configuration files with passwords to Git. Use `.gitignore`.

## 🤝 Contributing Guide

We welcome contributions! Please follow these steps:

1. **Open an issue**: Discuss your changes before implementing them.
2. **Code Style**: Follow the existing Java/Spring Boot coding conventions.
3. **Testing**: Add unit tests for new features if possible.
4. **Pull Request**: Submit a PR with a clear description of your changes.
