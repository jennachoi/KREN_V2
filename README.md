# KREN v2

**Slow letters, corrected by a friend.** Write to a native speaker of the language you're learning; your letter takes hours to fly across the map, and they reply *and* correct it sentence by sentence. Correcting someone else's letter is how you earn the stamps to send your own.

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

## Credits

- Profile pictures: [Avataaars](https://avataaars.com/) by Pablo Stanley (free for personal and commercial use), rendered with [DiceBear](https://www.dicebear.com/).
- World map: [Natural Earth](https://www.naturalearthdata.com/) via [world-atlas](https://github.com/topojson/world-atlas).
