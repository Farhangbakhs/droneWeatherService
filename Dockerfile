# ============================================
# Multi-Stage Dockerfile for Weather-Aware Drone Delivery System
# ============================================
# Stage 1: Build the application
# Stage 2: Run the application with minimal footprint
# ============================================

# ============================================
# Stage 1: Build Stage
# ============================================
FROM eclipse-temurin:21-jdk-alpine AS builder

# Set metadata
LABEL stage=builder
LABEL maintainer="Weather-Aware Drone Delivery System"
LABEL description="Build stage for Spring Boot application"

# Set working directory
WORKDIR /build

# Copy Maven wrapper and pom.xml first for dependency caching
# This layer will only rebuild if pom.xml changes
COPY .mvn/ .mvn/
COPY mvnw .
COPY pom.xml .

# Download dependencies (cached layer if pom.xml hasn't changed)
RUN ./mvnw dependency:go-offline -B

# Copy source code
COPY src/ ./src/

# Build the application
# Skip tests in Docker build (tests should be run in CI/CD pipeline)
# Use -DskipTests instead of -Dmaven.test.skip to ensure test classes are compiled
RUN ./mvnw clean package -DskipTests -B

# Verify JAR was created
RUN ls -lh /build/target/*.jar

# ============================================
# Stage 2: Runtime Stage
# ============================================
FROM eclipse-temurin:21-jre-alpine AS runtime

# Set metadata
LABEL maintainer="Weather-Aware Drone Delivery System"
LABEL version="3.0.0"
LABEL description="Weather-Aware Drone Delivery System - Production Runtime"
LABEL org.opencontainers.image.title="Drone Delivery System"
LABEL org.opencontainers.image.description="Drone pathfinding with real-time weather intelligence"
LABEL org.opencontainers.image.version="3.0.0"

# Install curl for healthcheck
RUN apk add --no-cache curl

# Create non-root user for security
# Running as non-root is a security best practice
RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup

# Set working directory
WORKDIR /app

# Copy JAR from builder stage
COPY --from=builder /build/target/ilp_submission_3-3.0.0.jar app.jar

# Change ownership to non-root user
RUN chown -R appuser:appgroup /app

# Switch to non-root user
USER appuser

# Expose application port
EXPOSE 8080

# Add health check
# This checks if the Spring Boot Actuator health endpoint responds
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# Environment variables with defaults
# These can be overridden at runtime using -e flag or docker-compose
ENV JAVA_OPTS="-Xms256m -Xmx512m" \
    SERVER_PORT=8080 \
    ILP_ENDPOINT="https://ilp-rest-2025-bvh6e9hschfagrgy.ukwest-01.azurewebsites.net/" \
    WEATHER_API_ENDPOINT="https://api.openweathermap.org/" \
    WEATHER_API_KEY=""

# Use shell form to allow variable expansion
# Spring Boot will read environment variables automatically
ENTRYPOINT sh -c "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar app.jar"

# ============================================
# Build Instructions:
# ============================================
# Build image:
#   docker build -t drone-delivery:latest .
#
# Run container:
#   docker run -d -p 8080:8080 \
#     -e WEATHER_API_KEY=your_api_key_here \
#     --name drone-delivery \
#     drone-delivery:latest
#
# Build with custom JAR name:
#   docker build -t drone-delivery:2.0.0 .
#
# View logs:
#   docker logs -f drone-delivery
#
# Check health:
#   docker inspect --format='{{.State.Health.Status}}' drone-delivery
# ============================================
