# Weather-Aware Drone Delivery System

**Tech Stack**: Java 21 | Spring Boot 3.4.3 | React 18.3.1 | Docker

**Version: 3.0.0 | **Status**: Production Ready

A sophisticated drone pathfinding and delivery optimization platform that integrates real-time weather intelligence to ensure safe and efficient drone operations.

---

##  Quick Start

### Prerequisites
- Docker 20.10+ and Docker Compose 2.0+
- OpenWeatherMap API key ([Get free key](https://openweathermap.org/api))

### 5-Minute Deployment

```bash
# 1. Navigate to project directory
cd ilp_submission_3

# 2. Set API key
echo "WEATHER_API_KEY=your_api_key_here" > .env

# 3. Start application
docker-compose up -d

# 4. Verify deployment
curl http://localhost:8080/actuator/health
```

 **Done!** Backend running at `http://localhost:8080`

---

##  Features

### Core Capabilities

 **Real-Time Weather Integration**
- OpenWeatherMap API integration
- Current weather and 5-day forecasts
- Automatic weather data caching

 **Wind Impact Analysis**
- Segment-by-segment wind analysis
- Headwind/Tailwind/Crosswind detection
- Dynamic cost adjustments (±30% based on conditions)

 **Safety Validation**
- Wind speed limits (max 12 m/s)
- Visibility requirements (min 5 km)
- Precipitation limits (max 5 mm/h)
- Temperature range checks (-10°C to 45°C)

 **Smart Delivery Recommendations**
- Alternative delivery windows when unsafe
- 5-day forecast analysis
- Safety ratings (EXCELLENT, GOOD, ACCEPTABLE)

 **Dual Operating Modes**
- **Normal Mode**: Address-based delivery
- **Testing Mode**: Coordinate-based testing (10-20x faster)

 **Interactive Visualizations**
- Flight path maps (Leaflet)
- Wind impact charts (Recharts)
- Real-time weather displays

---

##  Architecture

### Technology Stack

**Backend**
- Java 21
- Spring Boot 3.4.3
- Spring Web MVC
- RestTemplate (Weather API)
- Jackson (JSON)
- Lombok

**Frontend**
- React 18.3.1
- TypeScript 5.6
- Vite 6.0
- Leaflet 1.9 (Maps)
- Recharts 2.14 (Charts)
- Tailwind CSS 3.4

**Infrastructure**
- Docker & Docker Compose
- Multi-stage builds
- Health checks
- Log rotation

### API Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/v1/calcWeatherAwareDeliveryPath` | POST | Calculate weather-aware delivery with safety checks |
| `/api/v1/weather/flightConditions` | POST | Check flight safety at location/time |
| `/api/v1/weather/optimalWindows` | POST | Find safe delivery windows in forecast |
| `/actuator/health` | GET | Application health status |

---

##  Deployment

### Using Docker (Recommended)

#### Option 1: Build from Source

**Production Deployment**:
```bash
# 1. Set your OpenWeatherMap API key in .env file
echo "WEATHER_API_KEY=your_api_key_here" > .env

# 2. Build and run with docker-compose
docker-compose up -d

# 3. Verify deployment
curl http://localhost:8080/actuator/health

# 4. View logs
docker-compose logs -f backend

# 5. Stop containers
docker-compose down
```

#### Option 2: Load from Saved Image

**Quick Deployment Using Pre-built Image**:
```bash
# 1. Load the Docker image from tar file
docker load -i drone-delivery-3.0.0.tar

# 2. Verify image loaded
docker images | grep drone-delivery

# 3. Set your OpenWeatherMap API key
echo "WEATHER_API_KEY=your_api_key_here" > .env

# 4. Start with docker-compose
docker-compose up -d

# 5. Check container status
docker-compose ps

# 6. Test the application
curl http://localhost:8080/actuator/health
curl http://localhost:8080/api/v1/uid
```

**Save Docker Image** (for distribution):
```bash
# Save current image to tar file
docker save drone-delivery:3.0.0 -o drone-delivery-3.0.0.tar

# The tar file will be ~118MB and can be loaded on any Docker host
```

#### Development Deployment (Without Docker)

```bash
# Backend
./mvnw spring-boot:run

# Frontend (in separate terminal)
cd frontend && npm run dev
```

### Configuration

**Environment Variables**:
```bash
WEATHER_API_KEY=your_api_key          # Required
ILP_ENDPOINT=https://ilp-rest...      # Optional
JAVA_OPTS=-Xms512m -Xmx1024m          # Optional
```

### Docker Management Commands

```bash
# View container logs
docker-compose logs -f backend

# Restart after config changes
docker-compose restart backend

# Rebuild after code changes
docker-compose up -d --build

# Check container health
docker inspect --format='{{.State.Health.Status}}' drone-delivery-backend

# View resource usage
docker stats drone-delivery-backend

# Stop and remove all containers
docker-compose down
```

See [DOCKER_GUIDE.md](./DOCKER_GUIDE.md) for detailed deployment instructions.

---

##  Testing

### Run Tests

```bash
# Backend tests
./mvnw test

# Specific test class
./mvnw test -Dtest=WeatherServiceWindTests

# Frontend tests
cd frontend && npm test
```

### Test Coverage

- **WeatherService**: 30 wind impact tests
- **DroneService**: Pathfinding and validation tests
- **Integration Tests**: End-to-end API tests

---

##  Documentation

| Document | Description |
|----------|-------------|
| [CLAUDE.md](./CLAUDE.md) | **Complete feature documentation** (15,000+ words) |
| [DOCKER_GUIDE.md](./DOCKER_GUIDE.md) | **Docker deployment guide** (10,000+ words) |
| [DOCKER_QUICKSTART.md](./DOCKER_QUICKSTART.md) | **Quick deployment reference** |

---

##  Use Cases

### 1. Medical Supply Delivery
- Real-time safety assessment
- Alternative delivery windows
- Risk level evaluation

### 2. Emergency Response
- Rapid path calculation
- Weather impact analysis
- Optimal drone selection

### 3. Logistics Optimization
- Cost-adjusted routing
- Wind-aware path planning
- Multi-drone coordination

---

##  Development

### Project Structure

```
ilp_submission_3/
 src/main/java/cw1/
    controller/         # REST API endpoints
    service/            # Business logic
    DTO/                # Data transfer objects
    constants/          # Weather thresholds
 frontend/src/
    pages/              # React pages
    components/         # React components
    services/           # API clients
 Dockerfile              # Multi-stage build
 docker-compose.yml      # Orchestration
 README.md               # This file
```

### Build from Source

```bash
# Backend JAR
./mvnw clean package

# Frontend bundle
cd frontend && npm run build

# Docker image
docker build -t drone-delivery:latest .
```

---

##  API Examples

### Check Flight Conditions

```bash
curl -X POST http://localhost:8080/api/v1/weather/flightConditions \
  -H "Content-Type: application/json" \
  -d '{
    "location": {"lng": -3.1892, "lat": 55.9445}
  }'
```

**Response**:
```json
{
  "isSafe": true,
  "riskLevel": "LOW",
  "currentConditions": {
    "temperatureCelsius": 15.5,
    "windSpeedMps": 3.2,
    "visibility": 10000
  }
}
```

### Calculate Delivery Path

```bash
curl -X POST http://localhost:8080/api/v1/calcWeatherAwareDeliveryPath \
  -H "Content-Type: application/json" \
  -d '[{
    "requirements": {"capacity": 5.0},
    "delivery": {"lng": -3.1892, "lat": 55.9445}
  }]'
```

---

##  Security

-  Non-root Docker user
-  API keys via environment variables
-  CORS configuration
-  Input validation
-  Minimal Docker image (150MB)

---

##  Performance

| Metric | Value |
|--------|-------|
| **Docker Image Size** | 150MB (Alpine + JRE 21) |
| **Startup Time** | 15-20 seconds |
| **Response Time** | < 500ms (normal mode) |
| **Testing Mode** | 10-20x faster for international coordinates |
| **Memory Usage** | 256-512MB |

---

---

##  Acknowledgments

- **OpenWeatherMap** - Weather data API
- **Spring Boot** - Backend framework
- **React** - Frontend library
- **Leaflet** - Interactive maps
- **Recharts** - Data visualization

---

##  Support

For questions or issues:
-  Check [CLAUDE.md](./CLAUDE.md) for detailed documentation
-  See [DOCKER_GUIDE.md](./DOCKER_GUIDE.md) for deployment help
-  Review [DOCKER_QUICKSTART.md](./DOCKER_QUICKSTART.md) for quick troubleshooting

---

##  Roadmap

### Current Version: 3.0.0)
-  Weather intelligence integration
-  Wind impact analysis
-  Testing mode with service point bypass
-  Docker containerization

### Future Enhancements
- [ ] Kubernetes deployment
- [ ] Prometheus metrics
- [ ] Multi-region support
- [ ] Machine learning predictions
- [ ] Mobile app

---

**Made with  for safe and efficient drone deliveries**
