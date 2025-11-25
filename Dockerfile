# ============================
# 1. Build Stage
# ============================
FROM maven:3.9.5-eclipse-temurin-21 AS build

WORKDIR /app

# Copy pom first (enables dependency caching)
COPY pom.xml .

# Download dependencies without building
RUN mvn -B dependency:go-offline

# Copy source
COPY src ./src

# Build the application
#RUN mvn -B -DskipTests package
RUN mvn -B -DskipTests package spring-boot:repackage

# ============================
# 2. Runtime Stage (Slim JRE)
# ============================
FROM eclipse-temurin:21-jre-alpine AS runtime

WORKDIR /app

# Add unprivileged user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

#COPY --from=build /app/target/*.jar app.jar
COPY --from=build /app/target/wso2-cymmetri-connector-0.1.0.jar app.jar

USER appuser

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
