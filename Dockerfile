# syntax=docker/dockerfile:1

# 1) Build the React app. Vite writes it into backend/src/main/resources/static.
FROM node:24-alpine AS web
WORKDIR /app/frontend
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

# 2) Build the Spring Boot jar with the frontend bundled in. Tests run in CI (they need Docker for
#    Testcontainers), so they're skipped here.
FROM eclipse-temurin:25-jdk AS jar
WORKDIR /app/backend
COPY backend/.mvn .mvn
COPY backend/mvnw backend/pom.xml ./
RUN ./mvnw -B -q dependency:go-offline
COPY backend/src src
COPY --from=web /app/backend/src/main/resources/static src/main/resources/static
RUN ./mvnw -B -q package -DskipTests && cp target/kren-*.jar app.jar

# 3) Small runtime image: JRE only, non-root user.
FROM eclipse-temurin:25-jre
RUN useradd --system --uid 1001 kren
WORKDIR /app
COPY --from=jar /app/backend/app.jar app.jar
USER kren
ENV SPRING_PROFILES_ACTIVE=prod
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
