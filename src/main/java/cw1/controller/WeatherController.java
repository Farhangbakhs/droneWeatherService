package cw1.controller;

import cw1.DTO.Internal.FlightConditionsRequest;
import cw1.DTO.Internal.FlightConditionsResponse;
import cw1.DTO.Internal.OptimalWindowsRequest;
import cw1.DTO.Internal.OptimalWindowsResponse;
import cw1.DTO.Internal.WeatherSummary;
import cw1.DTO.LngLat;
import cw1.service.WeatherService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for weather intelligence endpoints
 * Provides weather data and flight condition analysis for drone operations
 *
 * Base path: /api/v1/weather
 */
@RestController
@RequestMapping("/api/v1/weather")
public class WeatherController {

    private final WeatherService weatherService;

    @Autowired
    public WeatherController(WeatherService weatherService) {
        this.weatherService = weatherService;
    }

    /**
     * Endpoint 1: Get current weather at a specific location
     *
     * GET /api/v1/weather/current/{lng}/{lat}
     *
     * Example: GET /api/v1/weather/current/-3.186/55.944
     *
     * @param lng Longitude (-180 to 180)
     * @param lat Latitude (-90 to 90)
     * @return Current weather summary
     */
    @GetMapping("/current/{lng}/{lat}")
    public ResponseEntity<WeatherSummary> getCurrentWeather(
            @PathVariable double lng,
            @PathVariable double lat) {

        LngLat position = new LngLat(lng, lat);
        WeatherSummary weather = weatherService.getCurrentWeatherSummary(position);
        return ResponseEntity.ok(weather);
    }

    /**
     * Endpoint 2: Get 5-day weather forecast with 3-hour intervals
     *
     * GET /api/v1/weather/forecast/{lng}/{lat}
     *
     * Example: GET /api/v1/weather/forecast/-3.186/55.944
     *
     * Returns 40 forecast items (5 days × 8 intervals per day)
     *
     * @param lng Longitude
     * @param lat Latitude
     * @return List of weather forecasts
     */
    @GetMapping("/forecast/{lng}/{lat}")
    public ResponseEntity<List<WeatherSummary>> getForecast(
            @PathVariable double lng,
            @PathVariable double lat) {

        LngLat position = new LngLat(lng, lat);
        List<WeatherSummary> forecasts = weatherService.getForecastSummary(position);
        return ResponseEntity.ok(forecasts);
    }

    /**
     * Endpoint 3: Check if weather conditions are safe for drone flight
     *
     * POST /api/v1/weather/checkFlightConditions
     *
     * Request body example:
     * {
     *   "position": {"lng": -3.186, "lat": 55.944},
     *   "timestamp": 1735689600  // Optional: Unix timestamp for forecast check
     * }
     *
     * Response includes:
     * - isSafe: Boolean overall assessment
     * - riskLevel: LOW, MEDIUM, HIGH, or CRITICAL
     * - warnings: List of specific issues
     * - metrics: Detailed safety checks
     *
     * @param request Flight conditions check request
     * @return Flight safety assessment
     */
    @PostMapping("/checkFlightConditions")
    public ResponseEntity<FlightConditionsResponse> checkFlightConditions(
            @Valid @RequestBody FlightConditionsRequest request) {

        FlightConditionsResponse response = weatherService.checkFlightConditions(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint 4: Find optimal delivery time windows based on weather forecast
     *
     * POST /api/v1/weather/optimalDeliveryWindows
     *
     * Request body example:
     * {
     *   "position": {"lng": -3.186, "lat": 55.944},
     *   "startTime": 1735689600,        // Optional: Unix timestamp (default: now)
     *   "endTime": 1736121600,          // Optional: Unix timestamp (default: now + 5 days)
     *   "minWindowDurationMinutes": 30  // Optional: Minimum duration (default: 30)
     * }
     *
     * Response includes:
     * - totalWindows: Number of safe windows found
     * - windows: List of delivery windows with:
     *   - Time range (Unix + ISO format)
     *   - Duration in minutes
     *   - Safety rating (EXCELLENT, GOOD, ACCEPTABLE)
     *   - Average wind speed and condition
     *   - Detailed forecast data
     *
     * @param request Optimal windows request
     * @return List of optimal delivery windows
     */
    @PostMapping("/optimalDeliveryWindows")
    public ResponseEntity<OptimalWindowsResponse> findOptimalDeliveryWindows(
            @Valid @RequestBody OptimalWindowsRequest request) {

        OptimalWindowsResponse response = weatherService.findOptimalDeliveryWindows(request);
        return ResponseEntity.ok(response);
    }
}