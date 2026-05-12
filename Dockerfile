# ============================================================
# 律法通 Spring Boot 多阶段构建
# ============================================================

# Stage 1: Build
FROM maven:3.9-eclipse-temurin-17-alpine AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn package -DskipTests -B

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

RUN addgroup -S lvatong && adduser -S lvatong -G lvatong

COPY --from=build /app/target/*.jar app.jar

RUN mkdir -p /app/uploads/contracts && chown -R lvatong:lvatong /app

RUN apk add --no-cache curl

USER lvatong

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=10s --retries=3 \
  CMD curl -sf http://localhost:8080/v3/api-docs || exit 1

ENTRYPOINT ["java", "-XX:+UseG1GC", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
