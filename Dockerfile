# ============================================================================
# Multi-stage Dockerfile for Roman Numeral Conversion Service
#
# Stage 1 (builder): Maven build with JDK — compiles source, runs tests
# Stage 2 (runtime): Alpine JRE only — minimal image, non-root user
#
# Final image: ~200MB (vs ~800MB with JDK)
# Security: runs as non-root 'appuser'
# Health: built-in HEALTHCHECK via actuator
# ============================================================================

# ---------- Stage 1: Build ----------
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /build

# Copy Maven wrapper and POM first (layer caching — deps don't change often)
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

# Copy source and build (skip tests — already run in CI)
COPY src/ src/
RUN ./mvnw package -DskipTests -B

# ---------- Stage 2: Runtime ----------
FROM eclipse-temurin:21-jre-alpine

# Security: non-root user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

# Copy the built JAR from builder stage
COPY --from=builder /build/target/*.jar app.jar

# Set ownership
RUN chown -R appuser:appgroup /app

# Switch to non-root user
USER appuser

# Expose application port
EXPOSE 8080

# Health check via actuator (no API key needed for actuator)
HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Run with Docker profile for structured JSON logging
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=docker"]
