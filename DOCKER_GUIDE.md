# Docker Deployment Guide - Weather-Aware Drone Delivery System

**Version: 3.0.0
**Last Updated**: November 30, 2025
**Status**: Production Ready

---

## Table of Contents

1. [Overview](#overview)
2. [Prerequisites](#prerequisites)
3. [Quick Start](#quick-start)
4. [Dockerfile Architecture](#dockerfile-architecture)
5. [Docker Compose Deployment](#docker-compose-deployment)
6. [Environment Configuration](#environment-configuration)
7. [Production Deployment](#production-deployment)
8. [Monitoring and Health Checks](#monitoring-and-health-checks)
9. [Troubleshooting](#troubleshooting)
10. [Advanced Topics](#advanced-topics)

---

## Overview

This project uses a **multi-stage Docker build** to create a production-ready container for the Weather-Aware Drone Delivery System. The containerization approach provides:

 **Reproducible builds** - Same environment everywhere
 **Minimal image size** - Uses Alpine Linux (~150MB final image)
 **Security hardening** - Non-root user, minimal attack surface
 **Health monitoring** - Built-in health checks
 **Easy deployment** - Single command to deploy

---

## Prerequisites

### Required

- **Docker**: 20.10+ ([Install Docker](https://docs.docker.com/get-docker/))
- **Docker Compose**: 2.0+ (included with Docker Desktop)
- **OpenWeatherMap API Key**: [Get free key](https://openweathermap.org/api)

### Recommended

- **Linux/macOS/WSL2**: For best performance
- **4GB RAM minimum**: For building and running
- **10GB disk space**: For Docker images and cache

---

## Quick Start

### Step 1: Navigate to Project

```bash
cd ilp_submission_3
```

### Step 2: Set API Key

```bash
# Create .env file from example
cp .env.example .env

# Edit .env and add your API key
echo "WEATHER_API_KEY=your_actual_api_key_here" > .env
```

### Step 3: Build and Run

```bash
# Using Docker Compose (recommended)
docker-compose up -d

# OR using Docker directly
docker build -t drone-delivery:latest .
docker run -d -p 8080:8080 \
  -e WEATHER_API_KEY=your_api_key_here \
  --name drone-delivery \
  drone-delivery:latest
```

### Step 4: Verify Deployment

```bash
# Check container is running
docker ps

# Check health status
curl http://localhost:8080/actuator/health

# View logs
docker logs -f drone-delivery
```

**Expected Response**:
```json
{
  "status": "UP",
  "components": {
    "diskSpace": { "status": "UP" },
    "ping": { "status": "UP" }
  }
}
```

---

## Dockerfile Architecture

### Multi-Stage Build Strategy

The Dockerfile uses a **two-stage build**:

#### Stage 1: Builder (eclipse-temurin:21-jdk-alpine)
```dockerfile
FROM eclipse-temurin:21-jdk-alpine AS builder
```

**Purpose**: Compile the application
**Size**: ~450MB (discarded after build)
**Tasks**:
- Downloads Maven dependencies
- Compiles Java source code
- Runs Maven package (creates JAR)
- Uses layer caching for faster rebuilds

#### Stage 2: Runtime (eclipse-temurin:21-jre-alpine)
```dockerfile
FROM eclipse-temurin:21-jre-alpine AS runtime
```

**Purpose**: Run the application
**Final Size**: ~150MB
**Tasks**:
- Copies only the JAR from builder
- Creates non-root user
- Configures health checks
- Sets up environment variables

### Security Features

1. **Non-Root User**
   ```dockerfile
   RUN addgroup -g 1001 -S appgroup && \
       adduser -u 1001 -S appuser -G appgroup
   USER appuser
   ```
   - Prevents privilege escalation
   - Follows least privilege principle

2. **Minimal Base Image**
   - Alpine Linux (5MB base)
   - Only JRE, not full JDK
   - Reduces attack surface

3. **No Secrets in Image**
   - API keys via environment variables
   - Configuration at runtime

### Layer Caching Strategy

```dockerfile
# Cached if pom.xml unchanged
COPY pom.xml .
RUN ./mvnw dependency:go-offline

# Only rebuilds if source changes
COPY src/ ./src/
RUN ./mvnw clean package
```

**Benefit**: Rebuilds in 10-20 seconds after code changes (vs 2-3 minutes cold build)

---

## Docker Compose Deployment

### Basic Configuration

```yaml
services:
  backend:
    build: .
    ports:
      - "8080:8080"
    environment:
      - WEATHER_API_KEY=${WEATHER_API_KEY}
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
    networks:
      - drone-network
```

### Common Commands

| Task | Command |
|------|---------|
| Start services | `docker-compose up -d` |
| Stop services | `docker-compose down` |
| View logs | `docker-compose logs -f backend` |
| Rebuild | `docker-compose up -d --build` |
| Restart service | `docker-compose restart backend` |
| Remove all data | `docker-compose down -v` |
| Scale instances | `docker-compose up -d --scale backend=3` |

### Service Status

```bash
# Check all services
docker-compose ps

# Expected output:
# NAME                       STATUS    PORTS
# drone-delivery-backend     Up        0.0.0.0:8080->8080/tcp
```

---

## Environment Configuration

### Required Variables

```bash
WEATHER_API_KEY=your_api_key_here  # REQUIRED
```

### Optional Variables

```bash
# Service Endpoints
ILP_ENDPOINT=https://ilp-rest-2025-bvh6e9hschfagrgy.ukwest-01.azurewebsites.net/
WEATHER_API_ENDPOINT=https://api.openweathermap.org/

# JVM Tuning
JAVA_OPTS=-Xms512m -Xmx1024m -XX:+UseG1GC

# Spring Boot Profile
SPRING_PROFILES_ACTIVE=production

# Server Port (if not 8080)
SERVER_PORT=8080

# Timezone
TZ=UTC
```

### Setting Variables

**Method 1: .env File** (Recommended)
```bash
# Create .env file
cat > .env << EOF
WEATHER_API_KEY=abc123xyz
JAVA_OPTS=-Xms512m -Xmx1024m
EOF

# Docker Compose reads .env automatically
docker-compose up -d
```

**Method 2: Command Line**
```bash
docker run -d \
  -e WEATHER_API_KEY=abc123xyz \
  -e JAVA_OPTS="-Xms512m -Xmx1024m" \
  -p 8080:8080 \
  drone-delivery:latest
```

**Method 3: Environment File**
```bash
# Create env.production
cat > env.production << EOF
WEATHER_API_KEY=abc123xyz
JAVA_OPTS=-Xms512m -Xmx1024m
EOF

# Use with Docker Compose
docker-compose --env-file env.production up -d
```

---

## Production Deployment

### Pre-Deployment Checklist

- [ ] API key configured in .env (not in Dockerfile!)
- [ ] Firewall rules configured (port 8080)
- [ ] SSL/TLS certificate ready (if using HTTPS)
- [ ] Log aggregation configured
- [ ] Monitoring/alerting set up
- [ ] Backup strategy defined
- [ ] Resource limits configured

### Resource Limits

```yaml
services:
  backend:
    deploy:
      resources:
        limits:
          cpus: '2'
          memory: 2G
        reservations:
          cpus: '1'
          memory: 1G
```

### Production Docker Compose

```yaml
version: '3.8'

services:
  backend:
    build: .
    image: drone-delivery:3.0.0
    container_name: drone-delivery-backend
    restart: always  # Auto-restart on failure
    ports:
      - "8080:8080"
    environment:
      - WEATHER_API_KEY=${WEATHER_API_KEY}
      - JAVA_OPTS=-Xms1g -Xmx2g -XX:+UseG1GC -XX:MaxGCPauseMillis=200
      - SPRING_PROFILES_ACTIVE=production
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 5s
      retries: 3
      start_period: 60s
    deploy:
      resources:
        limits:
          cpus: '2'
          memory: 2G
    logging:
      driver: "json-file"
      options:
        max-size: "50m"
        max-file: "5"
    networks:
      - drone-network

networks:
  drone-network:
    driver: bridge
```

### Nginx Reverse Proxy (Optional)

```nginx
# nginx.conf
upstream backend {
    server backend:8080;
}

server {
    listen 80;
    server_name your-domain.com;

    location / {
        proxy_pass http://backend;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

### Cloud Deployment

#### AWS ECS

```bash
# Build and tag
docker build -t drone-delivery:3.0.0 .
docker tag drone-delivery:3.0.0 \
  123456789012.dkr.ecr.us-east-1.amazonaws.com/drone-delivery:3.0.0

# Push to ECR
aws ecr get-login-password --region us-east-1 | \
  docker login --username AWS --password-stdin \
  123456789012.dkr.ecr.us-east-1.amazonaws.com

docker push 123456789012.dkr.ecr.us-east-1.amazonaws.com/drone-delivery:3.0.0
```

#### Google Cloud Run

```bash
# Build and push
gcloud builds submit --tag gcr.io/PROJECT-ID/drone-delivery:3.0.0

# Deploy
gcloud run deploy drone-delivery \
  --image gcr.io/PROJECT-ID/drone-delivery:3.0.0 \
  --platform managed \
  --region us-central1 \
  --allow-unauthenticated \
  --set-env-vars WEATHER_API_KEY=your_key
```

#### Azure Container Instances

```bash
# Build and push to ACR
az acr build --registry myregistry \
  --image drone-delivery:3.0.0 .

# Deploy
az container create \
  --resource-group myResourceGroup \
  --name drone-delivery \
  --image myregistry.azurecr.io/drone-delivery:3.0.0 \
  --dns-name-label drone-delivery \
  --ports 8080 \
  --environment-variables WEATHER_API_KEY=your_key
```

---

## Monitoring and Health Checks

### Built-in Health Check

The Dockerfile includes a health check:

```dockerfile
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1
```

**Parameters**:
- `interval`: Check every 30 seconds
- `timeout`: Fail if no response in 3 seconds
- `start-period`: Wait 40 seconds before first check (startup time)
- `retries`: Mark unhealthy after 3 consecutive failures

### Check Container Health

```bash
# View health status
docker inspect --format='{{.State.Health.Status}}' drone-delivery

# View health log
docker inspect --format='{{json .State.Health}}' drone-delivery | jq
```

**Health States**:
- `starting`: Initial startup (first 40 seconds)
- `healthy`: All checks passing
- `unhealthy`: 3 consecutive check failures

### Spring Boot Actuator Endpoints

| Endpoint | Purpose |
|----------|---------|
| `/actuator/health` | Overall health status |
| `/actuator/info` | Application information |

**Example Health Response**:
```json
{
  "status": "UP",
  "components": {
    "diskSpace": {
      "status": "UP",
      "details": {
        "total": 62671097856,
        "free": 31835549696,
        "threshold": 10485760
      }
    },
    "ping": {
      "status": "UP"
    }
  }
}
```

### Prometheus Metrics (Optional)

Add to `docker-compose.yml`:

```yaml
services:
  prometheus:
    image: prom/prometheus:latest
    ports:
      - "9090:9090"
    volumes:
      - ./prometheus.yml:/etc/prometheus/prometheus.yml:ro
    networks:
      - drone-network
```

Create `prometheus.yml`:

```yaml
scrape_configs:
  - job_name: 'spring-boot'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['backend:8080']
```

---

## Troubleshooting

### Container Won't Start

**Symptom**: `docker ps` shows no running container

```bash
# Check exit code and error
docker logs drone-delivery

# Common issues:
# 1. Port already in use
sudo lsof -i :8080
# Solution: Change port or stop conflicting service

# 2. Missing API key
# Solution: Check .env file exists and has WEATHER_API_KEY

# 3. Out of memory
docker stats
# Solution: Increase memory limits
```

### Health Check Failing

**Symptom**: Container marked as `unhealthy`

```bash
# Check health log
docker inspect --format='{{json .State.Health}}' drone-delivery | jq

# Test health endpoint manually
docker exec drone-delivery curl -f http://localhost:8080/actuator/health

# Check application logs
docker logs -f drone-delivery

# Common causes:
# - Application crashed (check logs)
# - Slow startup (increase start_period)
# - Database connection failed
```

### Image Build Fails

**Symptom**: `docker build` errors

```bash
# Clear build cache
docker builder prune -af

# Build with verbose output
docker build --progress=plain --no-cache -t drone-delivery:latest .

# Common issues:
# 1. Maven build failure
#    - Check Java version (must be 21)
#    - Check internet connection (Maven downloads dependencies)
#
# 2. Test failures
#    - Tests are skipped in Dockerfile (OK)
#
# 3. JAR not found
#    - Check pom.xml artifact name matches Dockerfile COPY
```

### High Memory Usage

**Symptom**: Container using excessive memory

```bash
# Check current usage
docker stats drone-delivery

# Tune JVM heap
docker-compose down
# Edit docker-compose.yml:
# JAVA_OPTS=-Xms256m -Xmx512m
docker-compose up -d

# Monitor garbage collection
docker exec drone-delivery jstat -gc 1 1000 10
```

### Cannot Connect to API

**Symptom**: HTTP 502/503 errors

```bash
# Check if container is running
docker ps

# Check if port is exposed
docker port drone-delivery

# Test from inside container
docker exec drone-delivery curl -f http://localhost:8080/actuator/health

# Test from host
curl http://localhost:8080/actuator/health

# Check firewall rules (Linux)
sudo ufw status
sudo ufw allow 8080/tcp
```

---

## Advanced Topics

### Multi-Architecture Builds

Build for ARM64 (Apple Silicon, Raspberry Pi):

```bash
# Enable BuildKit
export DOCKER_BUILDKIT=1

# Build for multiple platforms
docker buildx build \
  --platform linux/amd64,linux/arm64 \
  -t drone-delivery:3.0.0 \
  --push \
  .
```

### Layer Caching Optimization

```bash
# Build with cache
docker build -t drone-delivery:latest .

# Rebuild only changed layers (fast)
docker build -t drone-delivery:latest .

# Force rebuild all layers (slow)
docker build --no-cache -t drone-delivery:latest .
```

### Debugging Inside Container

```bash
# Start shell in running container
docker exec -it drone-delivery sh

# Inspect Java process
docker exec drone-delivery ps aux | grep java

# Check environment variables
docker exec drone-delivery env

# View file system
docker exec drone-delivery ls -la /app

# Check disk usage
docker exec drone-delivery df -h
```

### Custom JVM Flags

```bash
# Enable flight recorder
JAVA_OPTS="-XX:StartFlightRecording=dumponexit=true,filename=/app/recording.jfr"

# Enable remote debugging
JAVA_OPTS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"

# Heap dump on OOM
JAVA_OPTS="-XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/app/heapdump.hprof"
```

### Secrets Management

**Using Docker Secrets** (Swarm mode):

```yaml
services:
  backend:
    secrets:
      - weather_api_key
    environment:
      - WEATHER_API_KEY_FILE=/run/secrets/weather_api_key

secrets:
  weather_api_key:
    external: true
```

**Using HashiCorp Vault**:

```bash
# Fetch secret at runtime
export WEATHER_API_KEY=$(vault kv get -field=api_key secret/weather)
docker run -e WEATHER_API_KEY=$WEATHER_API_KEY drone-delivery:latest
```

---

## Summary

 **Multi-stage build** reduces image size by 66%
 **Non-root user** improves security
 **Health checks** enable automatic recovery
 **Docker Compose** simplifies deployment
 **Environment variables** separate config from code
 **Production-ready** with monitoring and logging

For additional help, see:
- [Main Documentation](./CLAUDE.md)
- [Quick Start Guide](./QUICK_START.md)
- [Docker Official Docs](https://docs.docker.com/)
- [Spring Boot Docker Guide](https://spring.io/guides/topicals/spring-boot-docker/)
