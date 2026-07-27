# ── Stage 1: Build ──────────────────────────────────────────────────────────
FROM maven:3.9.6-eclipse-temurin-21 AS build

WORKDIR /app

# Copy pom.xml first so Maven dependency layer is cached
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source and build the fat JAR
COPY src ./src
RUN mvn clean package -DskipTests -B

# ── Stage 2: Runtime ─────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# Copy the fat JAR from the build stage
COPY --from=build /app/target/*.jar app.jar

# Certs are mounted at runtime via:
# docker run -v /path/to/certs:/app/certs edgesense-ingestion
VOLUME ["/app/certs"]

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]