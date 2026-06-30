---
title: Local development setup
---

# Local development setup

| | |
| :--- | :--- |
| **Language** | English · [简体中文](local-development.zh-cn.md) |

Run FinSight backend and optional frontend dev server on your machine.

## Prerequisites

- JDK **21**
- Maven **3.9+**
- MySQL **8.x** with a database created for the app (for example `finsight`)

## Steps

1. Configure database connection via environment variables.

   Set at least:

   - `SPRING_DATASOURCE_URL`
   - `SPRING_DATASOURCE_USERNAME`
   - `SPRING_DATASOURCE_PASSWORD`

2. Start the application.

   ```bash
   mvn spring-boot:run
   ```

## Verification

Confirm the app starts successfully and serves the UI.

- Look for a Spring Boot startup log indicating the server is listening (for example `Tomcat started on port(s): ...`).
- Open the application in your browser (based on your configured server port).

## Cleanup (optional)

Stop the running process.
