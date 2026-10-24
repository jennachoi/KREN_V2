# KREN v2

A language-exchange penpal platform: find people who speak the language you're learning, and write each other letters.

A solo rebuild of [KREN](https://github.com/jennachoi/kren), a 2021 six-person bootcamp project (Java, Spring 5, JSP, Oracle).

> 🚧 Work in progress

## Tech stack

- **Backend:** Java 25, Spring Boot 4.1, Spring Security, Spring Data JPA, PostgreSQL 18, Flyway
- **Testing:** JUnit 5, Testcontainers
- **Infra:** Docker Compose, GitHub Actions

## Running locally

Requires JDK 25 and Docker.

```bash
cd backend
./mvnw spring-boot:run   # starts Postgres from ../compose.yaml automatically
./mvnw verify            # runs tests against a throwaway Postgres container
```
