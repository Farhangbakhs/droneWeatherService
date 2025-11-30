# Docker Quick Start - 5 Minute Deployment

**Goal**: Get the Weather-Aware Drone Delivery System running in Docker in under 5 minutes.

---

## Prerequisites

- Docker installed ([Download](https://docs.docker.com/get-docker/))
- OpenWeatherMap API key ([Get free key](https://openweathermap.org/api))

---

## Step-by-Step Deployment

### 1. Navigate to Project Directory

```bash
cd ilp_submission_3
```

### 2. Set Your API Key

```bash
# Create .env file with your API key
echo "WEATHER_API_KEY=your_actual_api_key_here" > .env
```

### 3. Start the Application

```bash
docker-compose up -d
```

**Expected output**:
```
Creating network "drone-delivery-network" ... done
Creating drone-delivery-backend ... done
```

### 4. Verify It's Running

```bash
# Check container status
docker-compose ps

# Test health endpoint
curl http://localhost:8080/actuator/health
```

**Expected response**:
```json
{"status":"UP"}
```

 **Done!** Your application is now running at `http://localhost:8080`

---

## Common Commands

### View Logs
```bash
docker-compose logs -f backend
```

### Stop Application
```bash
docker-compose down
```

### Restart Application
```bash
docker-compose restart backend
```

### Rebuild After Code Changes
```bash
docker-compose up -d --build
```

---

## Test the API

### Check Flight Conditions
```bash
curl -X POST http://localhost:8080/api/v1/weather/flightConditions \
  -H "Content-Type: application/json" \
  -d '{
    "location": {"lng": -3.1892, "lat": 55.9445}
  }'
```

### Calculate Delivery Path (Normal Mode)
```bash
curl -X POST http://localhost:8080/api/v1/calcWeatherAwareDeliveryPath \
  -H "Content-Type: application/json" \
  -d '[{
    "requirements": {"capacity": 5.0},
    "delivery": {"lng": -3.1892, "lat": 55.9445}
  }]'
```

---

## Troubleshooting

### Container won't start?

```bash
# Check logs for errors
docker logs drone-delivery-backend

# Most common issue: Missing API key
cat .env
# Should show: WEATHER_API_KEY=your_key
```

### Port 8080 already in use?

```bash
# Option 1: Stop conflicting service
# Option 2: Change port in docker-compose.yml
#   ports:
#     - "8081:8080"  # Use 8081 instead
```

### Want to reset everything?

```bash
# Stop and remove all containers and volumes
docker-compose down -v

# Remove all images
docker rmi drone-delivery:3.0.0

# Start fresh
docker-compose up -d --build
```

---

## Next Steps

- **Full Documentation**: See [DOCKER_GUIDE.md](./DOCKER_GUIDE.md)
- **Application Guide**: See [CLAUDE.md](./CLAUDE.md)
- **Testing Mode**: Access testing UI at `http://localhost:3000/testing` (if frontend deployed)

---

## Quick Reference Card

| Task | Command |
|------|---------|
| **Start** | `docker-compose up -d` |
| **Stop** | `docker-compose down` |
| **Logs** | `docker-compose logs -f backend` |
| **Status** | `docker-compose ps` |
| **Rebuild** | `docker-compose up -d --build` |
| **Shell** | `docker exec -it drone-delivery-backend sh` |
| **Health** | `curl http://localhost:8080/actuator/health` |
| **Clean** | `docker-compose down -v && docker system prune -af` |

---

**Questions?** See [DOCKER_GUIDE.md](./DOCKER_GUIDE.md) for detailed documentation.
