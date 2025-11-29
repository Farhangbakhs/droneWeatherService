package cw1.service;

import cw1.DTO.External.CurrentWeatherResponse;
import cw1.DTO.External.ForecastItem;
import cw1.DTO.External.ForecastResponse;
import cw1.DTO.External.Precipitation;
import cw1.DTO.Internal.*;
import cw1.DTO.LngLat;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Service containing business logic for weather-based flight decision making
 *
 * This service provides:
 * 1. Weather data conversion (External DTOs → Internal DTOs)
 * 2. Flight safety assessment based on configurable thresholds
 * 3. Optimal delivery window calculation from forecasts
 */
@Service
public class WeatherService {

    // ==================== FLIGHT SAFETY THRESHOLDS ====================
    // These values are configurable and based on typical drone limitations

    private static final double MAX_SAFE_WIND_SPEED = 12.0;      // m/s (~43 km/h, ~27 mph)
    private static final double MAX_SAFE_WIND_GUST = 15.0;       // m/s (~54 km/h, ~34 mph)
    private static final int MIN_SAFE_VISIBILITY = 5000;         // meters (5 km)
    private static final double MAX_SAFE_PRECIPITATION = 5.0;    // mm/h (light rain)
    private static final double MIN_SAFE_TEMP = -10.0;           // Celsius (battery performance)
    private static final double MAX_SAFE_TEMP = 45.0;            // Celsius (electronics)
    private static final int DEFAULT_MIN_WINDOW_DURATION = 30;   // minutes

    private final WeatherRetrieveService weatherRetrieveService;

    public WeatherService(WeatherRetrieveService weatherRetrieveService) {
        this.weatherRetrieveService = weatherRetrieveService;
    }

    // ==================== PUBLIC API METHODS ====================

    /**
     * Get current weather summary for a location
     *
     * @param position LngLat coordinates
     * @return Simplified weather summary
     */
    public WeatherSummary getCurrentWeatherSummary(LngLat position) {
        CurrentWeatherResponse response = weatherRetrieveService.getCurrentWeather(position);

        return convertToWeatherSummary(
                response.getDt(),
                response.getMain().getTemp(),
                response.getWind(),
                response.getWeather().isEmpty() ? null : response.getWeather().get(0),
                response.getVisibility(),
                response.getMain().getHumidity(),
                response.getRain(),
                response.getSnow(),
                null,  // no precipitation probability for current weather
                response.getClouds() != null ? response.getClouds().getAll() : null
        );
    }

    /**
     * Get forecast summary for a location
     *
     * @param position LngLat coordinates
     * @return List of weather summaries (40 items, 3-hour intervals)
     */
    public List<WeatherSummary> getForecastSummary(LngLat position) {
        ForecastResponse response = weatherRetrieveService.getForecast(position);
        List<WeatherSummary> forecasts = new ArrayList<>();

        if (response.getList() != null) {
            for (ForecastItem item : response.getList()) {
                WeatherSummary summary = convertForecastItemToSummary(item);
                forecasts.add(summary);
            }
        }

        return forecasts;
    }

    /**
     * Check if current or forecasted weather conditions are safe for flight
     *
     * @param request Contains position and optional timestamp
     * @return Detailed safety assessment
     */
    public FlightConditionsResponse checkFlightConditions(FlightConditionsRequest request) {
        WeatherSummary conditions;

        if (request.getTimestamp() == null) {
            // Check current weather
            conditions = getCurrentWeatherSummary(request.getPosition());
        } else {
            // Check forecast for specific time
            conditions = getForecastForTimestamp(request.getPosition(), request.getTimestamp());
        }

        return assessFlightSafety(conditions);
    }

    /**
     * Find optimal delivery windows based on weather forecast
     *
     * Groups consecutive safe forecast periods into delivery windows.
     * Filters out windows shorter than minimum duration.
     *
     * @param request Contains position, time range, and minimum duration
     * @return List of optimal time windows with safety ratings
     */
    public OptimalWindowsResponse findOptimalDeliveryWindows(OptimalWindowsRequest request) {
        List<WeatherSummary> forecasts = getForecastSummary(request.getPosition());

        // Set defaults
        long startTime = request.getStartTime() != null ? request.getStartTime() : Instant.now().getEpochSecond();
        long endTime = request.getEndTime() != null ? request.getEndTime() : startTime + (5 * 24 * 60 * 60); // 5 days
        int minDuration = request.getMinWindowDurationMinutes() != null ?
                request.getMinWindowDurationMinutes() : DEFAULT_MIN_WINDOW_DURATION;

        // Filter forecasts within time range
        List<WeatherSummary> relevantForecasts = forecasts.stream()
                .filter(f -> f.getTimestamp() >= startTime && f.getTimestamp() <= endTime)
                .toList();

        // Group consecutive safe periods into windows
        List<OptimalWindowsResponse.DeliveryWindow> windows = new ArrayList<>();
        List<WeatherSummary> currentWindow = new ArrayList<>();
        String currentSafetyRating = null;

        for (WeatherSummary forecast : relevantForecasts) {
            FlightConditionsResponse safety = assessFlightSafety(forecast);

            if (safety.getIsSafe() && !safety.getRiskLevel().equals("CRITICAL")) {
                // Safe period - add to current window
                currentWindow.add(forecast);
                if (currentSafetyRating == null) {
                    currentSafetyRating = mapRiskLevelToSafetyRating(safety.getRiskLevel());
                }
            } else {
                // Unsafe period - finalize current window if it meets minimum duration
                if (!currentWindow.isEmpty()) {
                    OptimalWindowsResponse.DeliveryWindow window =
                            createDeliveryWindow(currentWindow, currentSafetyRating, minDuration);
                    if (window != null) {
                        windows.add(window);
                    }
                    currentWindow = new ArrayList<>();
                    currentSafetyRating = null;
                }
            }
        }

        // Don't forget the last window
        if (!currentWindow.isEmpty()) {
            OptimalWindowsResponse.DeliveryWindow window =
                    createDeliveryWindow(currentWindow, currentSafetyRating, minDuration);
            if (window != null) {
                windows.add(window);
            }
        }

        return new OptimalWindowsResponse(windows.size(), windows);
    }

    // ==================== PRIVATE HELPER METHODS ====================

    /**
     * Convert ForecastItem to WeatherSummary
     */
    private WeatherSummary convertForecastItemToSummary(ForecastItem item) {
        return convertToWeatherSummary(
                item.getDt(),
                item.getMain().getTemp(),
                item.getWind(),
                item.getWeather().isEmpty() ? null : item.getWeather().get(0),
                item.getVisibility(),
                item.getMain().getHumidity(),
                item.getRain(),
                item.getSnow(),
                item.getProbabilityOfPrecipitation(),
                item.getClouds() != null ? item.getClouds().getAll() : null
        );
    }

    /**
     * Core conversion method: External weather data → Internal WeatherSummary
     *
     * This method handles both current weather and forecast items.
     */
    private WeatherSummary convertToWeatherSummary(Long timestamp, Double temp,
                                                   cw1.DTO.External.Wind wind,
                                                   cw1.DTO.External.WeatherData weatherData,
                                                   Integer visibility, Integer humidity,
                                                   Precipitation rain, Precipitation snow,
                                                   Double pop, Integer cloudCoverage) {
        WeatherSummary summary = new WeatherSummary();

        // Time
        summary.setTimestamp(timestamp);
        summary.setDateTime(formatTimestamp(timestamp));

        // Temperature
        summary.setTemperatureCelsius(temp);

        // Wind
        summary.setWindSpeedMps(wind != null ? wind.getSpeed() : 0.0);
        summary.setWindGustMps(wind != null ? wind.getGust() : null);
        summary.setWindDirectionDeg(wind != null && wind.getDeg() != null ?
                wind.getDeg().intValue() : null);

        // Conditions
        summary.setCondition(weatherData != null ? weatherData.getMain() : "Unknown");
        summary.setDescription(weatherData != null ? weatherData.getDescription() : "Unknown");

        // Visibility & Humidity
        summary.setVisibility(visibility);
        summary.setHumidity(humidity);

        // Calculate total precipitation (rain + snow)
        double precipitation = 0.0;
        if (rain != null) {
            precipitation += (rain.getThreeHour() != null ? rain.getThreeHour() :
                    (rain.getOneHour() != null ? rain.getOneHour() : 0.0));
        }
        if (snow != null) {
            precipitation += (snow.getThreeHour() != null ? snow.getThreeHour() :
                    (snow.getOneHour() != null ? snow.getOneHour() : 0.0));
        }
        summary.setPrecipitationMm(precipitation);

        // Probability and clouds
        summary.setProbabilityOfPrecipitation(pop);
        summary.setCloudCoverage(cloudCoverage);

        return summary;
    }

    /**
     * CORE LOGIC: Assess flight safety based on weather conditions
     *
     * Checks:
     * 1. Wind speed and gusts
     * 2. Visibility
     * 3. Precipitation rate
     * 4. Temperature
     * 5. Severe weather conditions (thunderstorms, etc.)
     *
     * @param conditions Weather summary to assess
     * @return Detailed safety assessment with warnings and metrics
     */
    private FlightConditionsResponse assessFlightSafety(WeatherSummary conditions) {
        FlightConditionsResponse response = new FlightConditionsResponse();
        FlightConditionsResponse.FlightSafetyMetrics metrics =
                new FlightConditionsResponse.FlightSafetyMetrics();
        List<String> warnings = new ArrayList<>();

        // Check 1: Wind
        boolean windSafe = true;
        if (conditions.getWindSpeedMps() > MAX_SAFE_WIND_SPEED) {
            windSafe = false;
            warnings.add(String.format("Wind speed %.1f m/s exceeds safe limit of %.1f m/s",
                    conditions.getWindSpeedMps(), MAX_SAFE_WIND_SPEED));
        }
        if (conditions.getWindGustMps() != null && conditions.getWindGustMps() > MAX_SAFE_WIND_GUST) {
            windSafe = false;
            warnings.add(String.format("Wind gusts %.1f m/s exceed safe limit of %.1f m/s",
                    conditions.getWindGustMps(), MAX_SAFE_WIND_GUST));
        }

        // Check 2: Visibility
        boolean visibilitySafe = true;
        if (conditions.getVisibility() != null && conditions.getVisibility() < MIN_SAFE_VISIBILITY) {
            visibilitySafe = false;
            warnings.add(String.format("Visibility %d m below safe minimum of %d m",
                    conditions.getVisibility(), MIN_SAFE_VISIBILITY));
        }

        // Check 3: Precipitation (convert 3h to hourly rate)
        boolean precipitationSafe = true;
        double precipitationRate = conditions.getPrecipitationMm() / 3.0;  // 3-hour → hourly
        if (precipitationRate > MAX_SAFE_PRECIPITATION) {
            precipitationSafe = false;
            warnings.add(String.format("Precipitation rate %.1f mm/h exceeds safe limit of %.1f mm/h",
                    precipitationRate, MAX_SAFE_PRECIPITATION));
        }

        // Check 4: Temperature
        boolean temperatureSafe = true;
        if (conditions.getTemperatureCelsius() < MIN_SAFE_TEMP) {
            temperatureSafe = false;
            warnings.add(String.format("Temperature %.1f°C below safe minimum of %.1f°C",
                    conditions.getTemperatureCelsius(), MIN_SAFE_TEMP));
        }
        if (conditions.getTemperatureCelsius() > MAX_SAFE_TEMP) {
            temperatureSafe = false;
            warnings.add(String.format("Temperature %.1f°C above safe maximum of %.1f°C",
                    conditions.getTemperatureCelsius(), MAX_SAFE_TEMP));
        }

        // Check 5: Severe weather conditions
        String condition = conditions.getCondition();
        if (condition.equals("Thunderstorm")) {
            warnings.add("Thunderstorm detected - flight operations prohibited");
            precipitationSafe = false;
        } else if (condition.equals("Snow") && precipitationRate > 2.0) {
            warnings.add("Heavy snow detected - reduced visibility and flight control");
        } else if (condition.equals("Fog") || condition.equals("Mist")) {
            if (conditions.getVisibility() != null && conditions.getVisibility() < MIN_SAFE_VISIBILITY) {
                warnings.add("Fog/Mist reducing visibility below safe limits");
            }
        }

        // Set metrics
        metrics.setWindSafe(windSafe);
        metrics.setVisibilitySafe(visibilitySafe);
        metrics.setPrecipitationSafe(precipitationSafe);
        metrics.setTemperatureSafe(temperatureSafe);
        metrics.setMaxSafeWindSpeed(MAX_SAFE_WIND_SPEED);
        metrics.setMinSafeVisibility(MIN_SAFE_VISIBILITY);
        metrics.setMaxSafePrecipitation(MAX_SAFE_PRECIPITATION);
        metrics.setMinSafeTemp(MIN_SAFE_TEMP);
        metrics.setMaxSafeTemp(MAX_SAFE_TEMP);

        // Overall assessment
        boolean isSafe = windSafe && visibilitySafe && precipitationSafe && temperatureSafe;
        String riskLevel = calculateRiskLevel(windSafe, visibilitySafe,
                precipitationSafe, temperatureSafe,
                warnings.size());

        response.setIsSafe(isSafe);
        response.setRiskLevel(riskLevel);
        response.setWarnings(warnings);
        response.setCurrentConditions(conditions);
        response.setMetrics(metrics);

        return response;
    }

    /**
     * Calculate risk level based on number of failed criteria
     */
    private String calculateRiskLevel(boolean windSafe, boolean visibilitySafe,
                                      boolean precipitationSafe, boolean temperatureSafe,
                                      int warningCount) {
        if (!windSafe || !visibilitySafe || !precipitationSafe || !temperatureSafe) {
            if (warningCount >= 3) {
                return "CRITICAL";
            } else if (warningCount >= 2) {
                return "HIGH";
            } else {
                return "MEDIUM";
            }
        }
        return "LOW";
    }

    /**
     * Map risk level to safety rating for delivery windows
     */
    private String mapRiskLevelToSafetyRating(String riskLevel) {
        return switch (riskLevel) {
            case "LOW" -> "EXCELLENT";
            case "MEDIUM" -> "GOOD";
            default -> "ACCEPTABLE";
        };
    }

    /**
     * Get forecast closest to requested timestamp
     */
    private WeatherSummary getForecastForTimestamp(LngLat position, long timestamp) {
        List<WeatherSummary> forecasts = getForecastSummary(position);

        // Find closest forecast to requested timestamp
        return forecasts.stream()
                .min((f1, f2) -> Long.compare(
                        Math.abs(f1.getTimestamp() - timestamp),
                        Math.abs(f2.getTimestamp() - timestamp)
                ))
                .orElse(null);
    }

    /**
     * Create delivery window from list of consecutive safe forecasts
     *
     * @return DeliveryWindow if duration meets minimum, null otherwise
     */
    private OptimalWindowsResponse.DeliveryWindow createDeliveryWindow(
            List<WeatherSummary> forecasts,
            String safetyRating,
            int minDurationMinutes) {

        if (forecasts.isEmpty()) {
            return null;
        }

        long startTime = forecasts.get(0).getTimestamp();
        long endTime = forecasts.get(forecasts.size() - 1).getTimestamp() + (3 * 60 * 60); // +3h
        int durationMinutes = (int) ((endTime - startTime) / 60);

        // Check minimum duration
        if (durationMinutes < minDurationMinutes) {
            return null;
        }

        // Calculate average wind speed
        double avgWindSpeed = forecasts.stream()
                .mapToDouble(WeatherSummary::getWindSpeedMps)
                .average()
                .orElse(0.0);

        // Get most common condition (simplified: just take first)
        String avgCondition = forecasts.stream()
                .map(WeatherSummary::getCondition)
                .findFirst()
                .orElse("Unknown");

        OptimalWindowsResponse.DeliveryWindow window = new OptimalWindowsResponse.DeliveryWindow();
        window.setStartTime(startTime);
        window.setEndTime(endTime);
        window.setStartDateTime(formatTimestamp(startTime));
        window.setEndDateTime(formatTimestamp(endTime));
        window.setDurationMinutes(durationMinutes);
        window.setSafetyRating(safetyRating);
        window.setAverageWindSpeed(avgWindSpeed);
        window.setAverageCondition(avgCondition);
        window.setForecastDetails(forecasts);

        return window;
    }

    /**
     * Format Unix timestamp to ISO-8601 date-time string
     */
    private String formatTimestamp(Long timestamp) {
        return Instant.ofEpochSecond(timestamp)
                .atZone(ZoneOffset.UTC)
                .format(DateTimeFormatter.ISO_DATE_TIME);
    }

    /**
     * Analyze wind impact on delivery path and calculate adjusted costs
     *
     * Wind impact calculation:
     * - Headwind (0-45°): +20% cost increase (fighting against wind)
     * - Crosswind (45-135°): +5% cost increase (stability issues)
     * - Tailwind (135-180°): -10% cost decrease (wind assistance)
     *
     * @param deliveryPath The calculated delivery path
     * @param position Location to get current wind conditions
     * @return Wind impact analysis with adjusted costs
     */
    public WindImpactAnalysis analyzeWindImpact(CalcDeliveryPathResult deliveryPath, LngLat position) {
        // Get current wind conditions at delivery location
        WeatherSummary weather = getCurrentWeatherSummary(position);
        double windSpeed = weather.getWindSpeedMps();
        Integer windDirection = weather.getWindDirectionDeg();

        // If no wind data available, return neutral analysis
        if (windDirection == null || windSpeed < 0.5) {
            return new WindImpactAnalysis(
                    deliveryPath.getTotalCost(),
                    deliveryPath.getTotalCost(),
                    0.0,
                    windSpeed,
                    windDirection,
                    List.of()
            );
        }

        List<WindImpactAnalysis.PathSegmentWind> segmentAnalysis = new ArrayList<>();
        double totalCostMultiplier = 0.0;
        int segmentCount = 0;

        // Analyze each drone path
        for (DronePathDTO dronePath : deliveryPath.getDronePaths()) {
            for (DeliveryPathDTO delivery : dronePath.getDeliveries()) {
                List<LngLat> flightPath = delivery.getFlightPath();
                if (flightPath == null || flightPath.size() < 2) {
                    continue;
                }

                // Analyze each segment in the flight path
                for (int i = 0; i < flightPath.size() - 1; i++) {
                    LngLat start = flightPath.get(i);
                    LngLat end = flightPath.get(i + 1);

                    // Calculate flight direction (bearing from start to end)
                    double flightDirection = calculateBearing(start, end);

                    // Calculate relative wind angle (difference between wind and flight direction)
                    double relativeWindAngle = calculateRelativeAngle(windDirection, flightDirection);

                    // Determine wind impact type and cost multiplier
                    String impactType;
                    double costMultiplier;

                    if (relativeWindAngle <= 45) {
                        // Headwind - flying into the wind
                        impactType = "HEADWIND";
                        costMultiplier = 1.0 + (0.20 * (windSpeed / MAX_SAFE_WIND_SPEED));
                    } else if (relativeWindAngle >= 135) {
                        // Tailwind - wind pushing from behind
                        impactType = "TAILWIND";
                        costMultiplier = 1.0 - (0.10 * (windSpeed / MAX_SAFE_WIND_SPEED));
                    } else {
                        // Crosswind - wind from the side
                        impactType = "CROSSWIND";
                        costMultiplier = 1.0 + (0.05 * (windSpeed / MAX_SAFE_WIND_SPEED));
                    }

                    segmentAnalysis.add(new WindImpactAnalysis.PathSegmentWind(
                            segmentCount,
                            flightDirection,
                            windDirection.doubleValue(),
                            windSpeed,
                            impactType,
                            relativeWindAngle,
                            costMultiplier
                    ));

                    totalCostMultiplier += costMultiplier;
                    segmentCount++;
                }
            }
        }

        // Calculate average cost multiplier across all segments
        double avgCostMultiplier = segmentCount > 0 ? totalCostMultiplier / segmentCount : 1.0;

        // Apply multiplier to total cost
        double baseCost = deliveryPath.getTotalCost();
        double windAdjustedCost = baseCost * avgCostMultiplier;
        double costAdjustmentPercent = ((windAdjustedCost - baseCost) / baseCost) * 100;

        return new WindImpactAnalysis(
                baseCost,
                windAdjustedCost,
                costAdjustmentPercent,
                windSpeed,
                windDirection,
                segmentAnalysis
        );
    }

    /**
     * Calculate bearing (direction) from one point to another
     * Returns angle in degrees (0-360) where 0/360 is North
     */
    private double calculateBearing(LngLat from, LngLat to) {
        double lat1 = Math.toRadians(from.getLat());
        double lat2 = Math.toRadians(to.getLat());
        double dLng = Math.toRadians(to.getLng() - from.getLng());

        double y = Math.sin(dLng) * Math.cos(lat2);
        double x = Math.cos(lat1) * Math.sin(lat2) -
                Math.sin(lat1) * Math.cos(lat2) * Math.cos(dLng);

        double bearing = Math.toDegrees(Math.atan2(y, x));

        // Normalize to 0-360 range
        return (bearing + 360) % 360;
    }

    /**
     * Calculate relative angle between wind direction and flight direction
     * Returns the smallest angle (0-180 degrees)
     */
    private double calculateRelativeAngle(double windDirection, double flightDirection) {
        double diff = Math.abs(windDirection - flightDirection);

        // Normalize to 0-180 range (we only care about relative angle, not which side)
        if (diff > 180) {
            diff = 360 - diff;
        }

        return diff;
    }
}