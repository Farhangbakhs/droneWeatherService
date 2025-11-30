# Deployment Checklist - Weather-Aware Drone Delivery System

**Version: 3.0.0
**Last Updated**: November 30, 2025

This checklist ensures successful deployment of the Weather-Aware Drone Delivery System using the provided Docker image and source code.

---

##  Package Contents

Verify you have received:

- [ ] Source code directory (`ilp_submission_3/`)
- [ ] Docker image file (`.tar` or `.tar.gz`) OR Dockerfile for building
- [ ] This documentation (`README.md`, `DOCKER_GUIDE.md`, etc.)

---

##  System Requirements

### Minimum Requirements

- [ ] **Docker**: Version 20.10 or higher
- [ ] **Docker Compose**: Version 2.0 or higher
- [ ] **RAM**: 4GB minimum
- [ ] **Disk Space**: 10GB free space
- [ ] **Ports**: Port 8080 available

### Verify Installation

```bash
# Check Docker version
docker --version
# Expected: Docker version 20.10+

# Check Docker Compose version
docker-compose --version
# Expected: Docker Compose version 2.0+

# Check available port
netstat -an | findstr "8080"  # Windows
# OR
lsof -i :8080                  # Linux/Mac
# Expected: No output (port is free)
```

---

##  Prerequisites

### 1. OpenWeatherMap API Key

- [ ] Obtain API key from [OpenWeatherMap](https://openweathermap.org/api)
- [ ] Free tier is sufficient for testing/development
- [ ] Keep API key secure (never commit to version control)

**Note**: The free tier allows 60 calls/minute, 1,000,000 calls/month - more than sufficient.

---

##  Deployment Steps

### Option A: Using Pre-Built Docker Image

If you received a `.tar` or `.tar.gz` Docker image file:

```bash
# 1. Navigate to project directory
cd ilp_submission_3

# 2. Load Docker image
docker load < drone-delivery-image.tar
# OR for compressed
docker load < drone-delivery-image.tar.gz

# 3. Verify image loaded
docker images | grep drone-delivery

# 4. Create .env file
echo "WEATHER_API_KEY=your_api_key_here" > .env

# 5. Update docker-compose.yml to use loaded image
# Edit docker-compose.yml and comment out the 'build' section
# Ensure 'image: drone-delivery:3.0.0' line is present

# 6. Start application
docker-compose up -d

# 7. Verify deployment
curl http://localhost:8080/actuator/health
# Expected: {"status":"UP"}
```

### Option B: Building from Source

If building from source code:

```bash
# 1. Navigate to project directory
cd ilp_submission_3

# 2. Create .env file
echo "WEATHER_API_KEY=your_api_key_here" > .env

# 3. Build and start application
docker-compose up -d --build

# 4. Monitor build progress
docker-compose logs -f backend

# 5. Verify deployment (wait 30-60 seconds for startup)
curl http://localhost:8080/actuator/health
# Expected: {"status":"UP"}
```

**Build Time**: First build takes 2-3 minutes, subsequent builds 10-20 seconds.

---

##  Post-Deployment Verification

### 1. Check Container Status

```bash
docker-compose ps
```

**Expected Output**:
```
NAME                       STATUS    PORTS
drone-delivery-backend     Up        0.0.0.0:8080->8080/tcp
```

### 2. Check Application Health

```bash
curl http://localhost:8080/actuator/health
```

**Expected Response**:
```json
{
  "status": "UP",
  "components": {
    "diskSpace": {"status": "UP"},
    "ping": {"status": "UP"}
  }
}
```

### 3. Test Weather API Integration

```bash
curl -X POST http://localhost:8080/api/v1/weather/flightConditions \
  -H "Content-Type: application/json" \
  -d '{
    "location": {"lng": -3.1892, "lat": 55.9445}
  }'
```

**Expected**: JSON response with weather data (not an error about API key).

### 4. Test Delivery Path Calculation

```bash
curl -X POST http://localhost:8080/api/v1/calcWeatherAwareDeliveryPath \
  -H "Content-Type: application/json" \
  -d '[{
    "requirements": {"capacity": 5.0},
    "delivery": {"lng": -3.1892, "lat": 55.9445}
  }]'
```

**Expected**: JSON response with delivery path or weather blocked message.

### 5. Check Logs

```bash
docker-compose logs -f backend
```

**Look for**:
-  No error messages
-  "Started Cw1Application" message
-  "Tomcat started on port(s): 8080"

---

##  Access Points

After successful deployment:

| Service | URL | Purpose |
|---------|-----|---------|
| **Backend API** | `http://localhost:8080` | REST API endpoints |
| **Health Check** | `http://localhost:8080/actuator/health` | System health |
| **Frontend** (if deployed) | `http://localhost:3000` | Web interface |

---

##  Troubleshooting

### Issue: Container Won't Start

**Check**:
```bash
# View container logs
docker logs drone-delivery-backend

# Check if port is in use
netstat -ano | findstr "8080"  # Windows
lsof -i :8080                  # Linux/Mac
```

**Solutions**:
- Kill process using port 8080
- Change port in `docker-compose.yml` (e.g., `8081:8080`)
- Check if Docker daemon is running

### Issue: Health Check Failing

**Check**:
```bash
# Check if container is running
docker ps

# View detailed logs
docker-compose logs -f backend

# Check inside container
docker exec -it drone-delivery-backend sh
curl localhost:8080/actuator/health
exit
```

**Solutions**:
- Wait 30-60 seconds for startup
- Increase memory limits in `docker-compose.yml`
- Check logs for Java errors

### Issue: Weather API Errors

**Check**:
```bash
# Test API key manually
curl "https://api.openweathermap.org/data/2.5/weather?lat=55.9445&lon=-3.1892&appid=YOUR_API_KEY"
```

**Solutions**:
- Verify API key is correct in `.env` file
- Ensure `.env` file is in project root
- Check API key quotas on OpenWeatherMap dashboard
- Restart container after updating `.env`: `docker-compose restart backend`

### Issue: "No drone available" Errors

**Possible Causes**:
- ILP endpoint unreachable
- Capacity requirement too high
- Delivery location out of range

**Check**:
```bash
# Test ILP endpoint
curl https://ilp-rest-2025-bvh6e9hschfagrgy.ukwest-01.azurewebsites.net/drones

# View backend logs
docker-compose logs backend | grep -i "drone"
```

---

##  Stopping the Application

```bash
# Stop containers (keeps data)
docker-compose stop

# Stop and remove containers (clean shutdown)
docker-compose down

# Stop and remove everything including volumes
docker-compose down -v
```

---

##  Updating the Application

```bash
# Pull latest changes (if rebuilding)
# Update source code as needed

# Rebuild and restart
docker-compose down
docker-compose up -d --build

# Or if using pre-built image
docker load < new-drone-delivery-image.tar
docker-compose up -d --force-recreate
```

---

##  Performance Benchmarks

Expected performance metrics:

| Metric | Value |
|--------|-------|
| Startup Time | 15-20 seconds |
| Memory Usage | 256-512 MB |
| Response Time | < 500ms (API calls) |
| Image Size | ~150 MB |
| Build Time (first) | 2-3 minutes |
| Build Time (cached) | 10-20 seconds |

---

##  Security Notes

-  API key stored in `.env` (not in code)
-  Container runs as non-root user (UID 1001)
-  Minimal Alpine Linux base image
-  No secrets in Docker image
-  CORS enabled for frontend integration

**Important**: Never commit `.env` file to version control!

---

##  Configuration Options

### Environment Variables

Edit `.env` file or set in `docker-compose.yml`:

```bash
# Required
WEATHER_API_KEY=your_key_here

# Optional - JVM tuning
JAVA_OPTS=-Xms512m -Xmx1024m

# Optional - Change port
SERVER_PORT=8080

# Optional - Override endpoints
ILP_ENDPOINT=https://your-custom-endpoint/
WEATHER_API_ENDPOINT=https://api.openweathermap.org/
```

### Resource Limits

Edit `docker-compose.yml` to adjust resources:

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

---

##  Support Resources

If you encounter issues:

1. **Check Documentation**:
   - [DOCKER_QUICKSTART.md](./DOCKER_QUICKSTART.md) - Quick troubleshooting
   - [DOCKER_GUIDE.md](./DOCKER_GUIDE.md) - Comprehensive deployment guide
   - [CLAUDE.md](./CLAUDE.md) - Full feature documentation

2. **Check Logs**:
   ```bash
   docker-compose logs -f backend
   ```

3. **Verify System Requirements**:
   - Docker version 20.10+
   - Sufficient memory (4GB+)
   - Port 8080 available

4. **Test Components**:
   - Health endpoint: `curl http://localhost:8080/actuator/health`
   - Weather API: Test key at OpenWeatherMap dashboard
   - ILP endpoint: Check if ILP service is accessible

---

##  Final Checklist

Before considering deployment complete:

- [ ] Container is running (`docker ps` shows "Up")
- [ ] Health check passes (`/actuator/health` returns "UP")
- [ ] Weather API integration works (test with API call)
- [ ] Backend responds to delivery path requests
- [ ] Logs show no errors
- [ ] Performance is acceptable (< 500ms response times)
- [ ] `.env` file contains valid API key
- [ ] Frontend can connect to backend (if applicable)

---

##  Success Criteria

Deployment is successful when:

1.  Container health check status is "healthy"
2.  `/actuator/health` returns `{"status":"UP"}`
3.  API calls return valid responses (not 500 errors)
4.  Weather data is being fetched successfully
5.  Delivery path calculations complete successfully

---

**Deployment Time**: Typically 5-10 minutes from start to verified deployment.

**Questions?** Refer to the comprehensive documentation in `DOCKER_GUIDE.md` or `CLAUDE.md`.
