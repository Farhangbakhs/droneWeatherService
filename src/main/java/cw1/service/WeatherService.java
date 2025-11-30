package cw1.service;

import cw1.DTO.External.CurrentWeatherResponse;
import cw1.DTO.External.ForecastItem;
import cw1.DTO.External.ForecastResponse;
import cw1.DTO.External.Precipitation;
import cw1.DTO.Internal.*;
import cw1.DTO.LngLat;
import cw1.constants.WeatherConstants;
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
                request.getMinWindowDurationMinutes() : WeatherConstants.DEFAULT_MIN_WINDOW_DURATION_MINUTES;

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
        if (conditions.getWindSpeedMps() > WeatherConstants.MAX_SAFE_WIND_SPEED) {
            windSafe = false;
            warnings.add(String.format("Wind speed %.1f m/s exceeds safe limit of %.1f m/s",
                    conditions.getWindSpeedMps(), WeatherConstants.MAX_SAFE_WIND_SPEED));
        }
        if (conditions.getWindGustMps() != null && conditions.getWindGustMps() > WeatherConstants.MAX_SAFE_WIND_GUST) {
            windSafe = false;
            warnings.add(String.format("Wind gusts %.1f m/s exceed safe limit of %.1f m/s",
                    conditions.getWindGustMps(), WeatherConstants.MAX_SAFE_WIND_GUST));
        }

        // Check 2: Visibility
        boolean visibilitySafe = true;
        if (conditions.getVisibility() != null && conditions.getVisibility() < WeatherConstants.MIN_SAFE_VISIBILITY) {
            visibilitySafe = false;
            warnings.add(String.format("Visibility %d m below safe minimum of %d m",
                    conditions.getVisibility(), WeatherConstants.MIN_SAFE_VISIBILITY));
        }

        // Check 3: Precipitation (convert 3h to hourly rate)
        boolean precipitationSafe = true;
        double precipitationRate = conditions.getPrecipitationMm() / 3.0;  // 3-hour → hourly
        if (precipitationRate > WeatherConstants.MAX_SAFE_PRECIPITATION) {
            precipitationSafe = false;
            warnings.add(String.format("Precipitation rate %.1f mm/h exceeds safe limit of %.1f mm/h",
                    precipitationRate, WeatherConstants.MAX_SAFE_PRECIPITATION));
        }

        // Check 4: Temperature
        boolean temperatureSafe = true;
        if (conditions.getTemperatureCelsius() < WeatherConstants.MIN_SAFE_TEMP) {
            temperatureSafe = false;
            warnings.add(String.format("Temperature %.1f°C below safe minimum of %.1f°C",
                    conditions.getTemperatureCelsius(), WeatherConstants.MIN_SAFE_TEMP));
        }
        if (conditions.getTemperatureCelsius() > WeatherConstants.MAX_SAFE_TEMP) {
            temperatureSafe = false;
            warnings.add(String.format("Temperature %.1f°C above safe maximum of %.1f°C",
                    conditions.getTemperatureCelsius(), WeatherConstants.MAX_SAFE_TEMP));
        }

        // Check 5: Severe weather conditions
        String condition = conditions.getCondition();
        if (condition.equals("Thunderstorm")) {
            warnings.add("Thunderstorm detected - flight operations prohibited");
            precipitationSafe = false;
        } else if (condition.equals("Snow") && precipitationRate > 2.0) {
            warnings.add("Heavy snow detected - reduced visibility and flight control");
        } else if (condition.equals("Fog") || condition.equals("Mist")) {
            if (conditions.getVisibility() != null && conditions.getVisibility() < WeatherConstants.MIN_SAFE_VISIBILITY) {
                warnings.add("Fog/Mist reducing visibility below safe limits");
            }
        }

        // Set metrics
        metrics.setWindSafe(windSafe);
        metrics.setVisibilitySafe(visibilitySafe);
        metrics.setPrecipitationSafe(precipitationSafe);
        metrics.setTemperatureSafe(temperatureSafe);
        metrics.setMaxSafeWindSpeed(WeatherConstants.MAX_SAFE_WIND_SPEED);
        metrics.setMinSafeVisibility(WeatherConstants.MIN_SAFE_VISIBILITY);
        metrics.setMaxSafePrecipitation(WeatherConstants.MAX_SAFE_PRECIPITATION);
        metrics.setMinSafeTemp(WeatherConstants.MIN_SAFE_TEMP);
        metrics.setMaxSafeTemp(WeatherConstants.MAX_SAFE_TEMP);

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
            if (warningCount >= WeatherConstants.CRITICAL_RISK_WARNING_COUNT) {
                return "CRITICAL";
            } else if (warningCount >= WeatherConstants.HIGH_RISK_WARNING_COUNT) {
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
        long endTime = forecasts.get(forecasts.size() - 1).getTimestamp() + WeatherConstants.FORECAST_INTERVAL_SECONDS;
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
        if (windDirection == null || windSpeed < WeatherConstants.MIN_WIND_SPEED_FOR_ANALYSIS) {
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

        // Analyze each drone path (a delivery may involve multiple drones/dispatches)
        for (DronePathDTO dronePath : deliveryPath.getDronePaths()) {
            for (DeliveryPathDTO delivery : dronePath.getDeliveries()) {
                List<LngLat> flightPath = delivery.getFlightPath();

                // Skip invalid paths (need at least 2 points to form a segment)
                if (flightPath == null || flightPath.size() < 2) {
                    continue;
                }

                // Analyze each segment in the flight path
                // A segment is the straight-line movement from one waypoint to the next
                for (int i = 0; i < flightPath.size() - 1; i++) {
                    LngLat start = flightPath.get(i);
                    LngLat end = flightPath.get(i + 1);

                    // Step 1: Calculate flight direction (bearing from start to end)
                    // Returns angle in degrees where 0° = North, 90° = East, etc.
                    double flightDirection = calculateBearing(start, end);

                    // Step 2: Calculate relative wind angle (difference between wind and flight direction)
                    // Normalizes to 0-180° range (we only care about smallest angle, not which side)
                    double relativeWindAngle = calculateRelativeAngle(windDirection, flightDirection);

                    // Step 3: Determine wind impact type and cost multiplier based on relative angle
                    // - 0-45°: Headwind (flying into wind) → increased cost
                    // - 135-180°: Tailwind (wind from behind) → decreased cost
                    // - 45-135°: Crosswind (perpendicular wind) → slight cost increase (stability)
                    String impactType;
                    double costMultiplier;

                    if (relativeWindAngle <= WeatherConstants.HEADWIND_ANGLE_THRESHOLD) {
                        // Headwind - flying into the wind
                        // Scale impact based on wind speed: 0-30% penalty for 0-12 m/s winds
                        impactType = "HEADWIND";
                        double headwindPenalty = Math.min(
                            windSpeed * WeatherConstants.HEADWIND_PENALTY_PER_MS,
                            WeatherConstants.MAX_HEADWIND_PENALTY
                        );
                        costMultiplier = 1.0 + headwindPenalty;

                        if (i == 0) { // Log first segment details
                            System.out.printf("  WIND ANALYSIS: WindSpeed=%.2f m/s, Penalty=%.2f%%, Multiplier=%.4f%n",
                                    windSpeed, headwindPenalty * 100, costMultiplier);
                        }
                    } else if (relativeWindAngle >= WeatherConstants.TAILWIND_ANGLE_THRESHOLD) {
                        // Tailwind - wind pushing from behind
                        // Scale benefit based on wind speed: 0-15% discount for 0-12 m/s winds
                        impactType = "TAILWIND";
                        double tailwindBenefit = Math.min(
                            windSpeed * WeatherConstants.TAILWIND_BENEFIT_PER_MS,
                            WeatherConstants.MAX_TAILWIND_BENEFIT
                        );
                        costMultiplier = 1.0 - tailwindBenefit;

                        if (i == 0) {
                            System.out.printf("  WIND ANALYSIS: WindSpeed=%.2f m/s, Benefit=%.2f%%, Multiplier=%.4f%n",
                                    windSpeed, tailwindBenefit * 100, costMultiplier);
                        }
                    } else {
                        // Crosswind - wind from the side
                        // Scale impact based on wind speed: 0-12% penalty for 0-12 m/s winds
                        impactType = "CROSSWIND";
                        double crosswindPenalty = Math.min(
                            windSpeed * WeatherConstants.CROSSWIND_PENALTY_PER_MS,
                            WeatherConstants.MAX_CROSSWIND_PENALTY
                        );
                        costMultiplier = 1.0 + crosswindPenalty;

                        if (i == 0) {
                            System.out.printf("  WIND ANALYSIS: WindSpeed=%.2f m/s, Penalty=%.2f%%, Multiplier=%.4f%n",
                                    windSpeed, crosswindPenalty * 100, costMultiplier);
                        }
                    }

                    // Store segment analysis for detailed reporting
                    segmentAnalysis.add(new WindImpactAnalysis.PathSegmentWind(
                            segmentCount,
                            flightDirection,
                            windDirection.doubleValue(),
                            windSpeed,
                            impactType,
                            relativeWindAngle,
                            costMultiplier
                    ));

                    // Accumulate multipliers for averaging
                    totalCostMultiplier += costMultiplier;
                    segmentCount++;
                }
            }
        }

        // Step 4: Calculate average cost multiplier across all segments
        // This smooths out the impact across the entire path
        // Example: If 5 segments have multipliers [1.1, 1.2, 0.9, 1.0, 1.15], avg = 1.07
        double avgCostMultiplier = segmentCount > 0 ? totalCostMultiplier / segmentCount : 1.0;

        // Step 5: Apply multiplier to total cost
        // baseCost = number of moves (each move costs 1 unit)
        // windAdjustedCost = baseCost × avgMultiplier (accounts for wind resistance/assistance)
        double baseCost = deliveryPath.getTotalCost();
        double windAdjustedCost = baseCost * avgCostMultiplier;

        // Calculate percentage change for reporting
        // Positive % = cost increase (headwind/crosswind dominant)
        // Negative % = cost decrease (tailwind dominant)
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
     * Calculate bearing (direction) from one point to another using spherical geometry.
     *
     * <p><b>Formula:</b> Uses the atan2 function with adjusted coordinates:
     * <pre>
     * y = sin(Δlng) × cos(lat₂)
     * x = cos(lat₁) × sin(lat₂) - sin(lat₁) × cos(lat₂) × cos(Δlng)
     * bearing = atan2(y, x)
     * </pre>
     *
     * <p><b>Bearing Convention:</b>
     * <ul>
     *   <li>0° / 360° = North</li>
     *   <li>90° = East</li>
     *   <li>180° = South</li>
     *   <li>270° = West</li>
     * </ul>
     *
     * <p><b>Note:</b> This uses the forward azimuth formula from spherical
     * trigonometry, which is accurate for any distance on Earth.
     *
     * @param from Starting point
     * @param to Ending point
     * @return Bearing angle in degrees (0-360), where 0° is North
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
        return (bearing + WeatherConstants.FULL_CIRCLE_DEGREES) % WeatherConstants.FULL_CIRCLE_DEGREES;
    }

    /**
     * Calculate relative angle between wind direction and flight direction.
     *
     * <p><b>Purpose:</b> Determines the angular difference to classify wind impact type
     * (headwind, tailwind, or crosswind).
     *
     * <p><b>Algorithm:</b>
     * <ol>
     *   <li>Calculate absolute difference: |windDir - flightDir|</li>
     *   <li>Normalize to 0-180° range (we only care about smallest angle, not direction)</li>
     *   <li>If difference > 180°, use 360° - difference (shortest angular path)</li>
     * </ol>
     *
     * <p><b>Examples:</b>
     * <ul>
     *   <li>Wind: 0°, Flight: 0° → Relative: 0° (headwind)</li>
     *   <li>Wind: 0°, Flight: 180° → Relative: 180° (tailwind)</li>
     *   <li>Wind: 350°, Flight: 10° → Relative: 20° (headwind, not 340°)</li>
     * </ul>
     *
     * @param windDirection Wind direction in degrees (0-360)
     * @param flightDirection Flight bearing in degrees (0-360)
     * @return Smallest angular difference in degrees (0-180)
     */
    private double calculateRelativeAngle(double windDirection, double flightDirection) {
        double diff = Math.abs(windDirection - flightDirection);

        // Normalize to 0-180 range (we only care about relative angle, not which side)
        if (diff > WeatherConstants.HALF_CIRCLE_DEGREES) {
            diff = WeatherConstants.FULL_CIRCLE_DEGREES - diff;
        }

        return diff;
    }

    // ==================== DATE/TIME BASED METHODS (CW3 EXTENSION) ====================

    /**
     * Convert LocalDate and LocalTime to epoch timestamp for forecast lookup
     *
     * @param date LocalDate for the delivery
     * @param time LocalTime for the delivery
     * @return Epoch timestamp in seconds
     */
    private long convertToTimestamp(java.time.LocalDate date, java.time.LocalTime time) {
        if (date == null || time == null) {
            return Instant.now().getEpochSecond();
        }

        // Combine date and time, assume UTC for weather API consistency
        return date.atTime(time).toEpochSecond(ZoneOffset.UTC);
    }

    /**
     * Check flight conditions at a specific date and time using forecast data
     * This is the modular weather-aware extension - if date/time are null, falls back to current weather
     *
     * OpenWeatherMap forecast API covers ONLY the next 5 days. If date/time is outside this range,
     * falls back to current weather.
     *
     * @param position Delivery location
     * @param date Optional delivery date (if null, uses current weather)
     * @param time Optional delivery time (if null, uses current weather)
     * @return Flight safety assessment
     */
    public FlightConditionsResponse checkFlightConditionsAtDateTime(
            LngLat position, java.time.LocalDate date, java.time.LocalTime time) {

        if (date == null || time == null) {
            // Fallback to current weather
            FlightConditionsResponse response = checkFlightConditions(new FlightConditionsRequest(position, null));
            response.setWeatherDataSource("Using current weather");
            return response;
        }

        // Validate date range: OpenWeatherMap forecast API only covers next 5 days
        java.time.LocalDateTime requestedDateTime = java.time.LocalDateTime.of(date, time);
        java.time.LocalDateTime now = java.time.LocalDateTime.now(ZoneOffset.UTC);
        java.time.LocalDateTime fiveDaysFromNow = now.plusDays(5);

        // If requested time is in the past or more than 5 days in the future, use current weather
        if (requestedDateTime.isBefore(now) || requestedDateTime.isAfter(fiveDaysFromNow)) {
            // Outside forecast range - fallback to current weather
            FlightConditionsResponse response = checkFlightConditions(new FlightConditionsRequest(position, null));
            response.setWeatherDataSource("Not in forecast range, using current weather");
            return response;
        }

        // Use forecast for specific date/time (within 5-day range)
        long timestamp = convertToTimestamp(date, time);
        FlightConditionsResponse response = checkFlightConditions(new FlightConditionsRequest(position, timestamp));
        response.setWeatherDataSource("Using forecast data");
        return response;
    }

    /**
     * Analyze wind impact at a specific date and time using forecast data
     * This is the modular weather-aware extension - if date/time are null, falls back to current weather
     *
     * OpenWeatherMap forecast API covers ONLY the next 5 days. If date/time is outside this range,
     * falls back to current weather.
     *
     * @param deliveryPath The calculated delivery path
     * @param position Delivery location
     * @param date Optional delivery date (if null, uses current weather)
     * @param time Optional delivery time (if null, uses current weather)
     * @return Wind impact analysis with adjusted costs
     */
    public WindImpactAnalysis analyzeWindImpactAtDateTime(
            CalcDeliveryPathResult deliveryPath, LngLat position,
            java.time.LocalDate date, java.time.LocalTime time) {

        WeatherSummary weather;

        if (date == null || time == null) {
            // Fallback to current weather
            weather = getCurrentWeatherSummary(position);
        } else {
            // Validate date range: OpenWeatherMap forecast API only covers next 5 days
            java.time.LocalDateTime requestedDateTime = java.time.LocalDateTime.of(date, time);
            java.time.LocalDateTime now = java.time.LocalDateTime.now(ZoneOffset.UTC);
            java.time.LocalDateTime fiveDaysFromNow = now.plusDays(5);

            // If requested time is in the past or more than 5 days in the future, use current weather
            if (requestedDateTime.isBefore(now) || requestedDateTime.isAfter(fiveDaysFromNow)) {
                // Outside forecast range - fallback to current weather
                weather = getCurrentWeatherSummary(position);
            } else {
                // Use forecast for specific date/time (within 5-day range)
                long timestamp = convertToTimestamp(date, time);
                weather = getForecastForTimestamp(position, timestamp);
            }
        }

        double windSpeed = weather.getWindSpeedMps();
        Integer windDirection = weather.getWindDirectionDeg();

        // If no wind data available, return neutral analysis
        if (windDirection == null || windSpeed < WeatherConstants.MIN_WIND_SPEED_FOR_ANALYSIS) {
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

        // Analyze each drone path (a delivery may involve multiple drones/dispatches)
        for (DronePathDTO dronePath : deliveryPath.getDronePaths()) {
            for (DeliveryPathDTO delivery : dronePath.getDeliveries()) {
                List<LngLat> flightPath = delivery.getFlightPath();

                // Skip invalid paths (need at least 2 points to form a segment)
                if (flightPath == null || flightPath.size() < 2) {
                    continue;
                }

                // Analyze each segment in the flight path
                // A segment is the straight-line movement from one waypoint to the next
                for (int i = 0; i < flightPath.size() - 1; i++) {
                    LngLat start = flightPath.get(i);
                    LngLat end = flightPath.get(i + 1);

                    // Step 1: Calculate flight direction (bearing from start to end)
                    // Returns angle in degrees where 0° = North, 90° = East, etc.
                    double flightDirection = calculateBearing(start, end);

                    // Step 2: Calculate relative wind angle (difference between wind and flight direction)
                    // Normalizes to 0-180° range (we only care about smallest angle, not which side)
                    double relativeWindAngle = calculateRelativeAngle(windDirection, flightDirection);

                    // Step 3: Determine wind impact type and cost multiplier based on relative angle
                    // - 0-45°: Headwind (flying into wind) → increased cost
                    // - 135-180°: Tailwind (wind from behind) → decreased cost
                    // - 45-135°: Crosswind (perpendicular wind) → slight cost increase (stability)
                    String impactType;
                    double costMultiplier;

                    if (relativeWindAngle <= WeatherConstants.HEADWIND_ANGLE_THRESHOLD) {
                        // Headwind - flying into the wind
                        // Scale impact based on wind speed: 0-30% penalty for 0-12 m/s winds
                        impactType = "HEADWIND";
                        double headwindPenalty = Math.min(
                            windSpeed * WeatherConstants.HEADWIND_PENALTY_PER_MS,
                            WeatherConstants.MAX_HEADWIND_PENALTY
                        );
                        costMultiplier = 1.0 + headwindPenalty;

                        if (i == 0) { // Log first segment details
                            System.out.printf("  WIND ANALYSIS: WindSpeed=%.2f m/s, Penalty=%.2f%%, Multiplier=%.4f%n",
                                    windSpeed, headwindPenalty * 100, costMultiplier);
                        }
                    } else if (relativeWindAngle >= WeatherConstants.TAILWIND_ANGLE_THRESHOLD) {
                        // Tailwind - wind pushing from behind
                        // Scale benefit based on wind speed: 0-15% discount for 0-12 m/s winds
                        impactType = "TAILWIND";
                        double tailwindBenefit = Math.min(
                            windSpeed * WeatherConstants.TAILWIND_BENEFIT_PER_MS,
                            WeatherConstants.MAX_TAILWIND_BENEFIT
                        );
                        costMultiplier = 1.0 - tailwindBenefit;

                        if (i == 0) {
                            System.out.printf("  WIND ANALYSIS: WindSpeed=%.2f m/s, Benefit=%.2f%%, Multiplier=%.4f%n",
                                    windSpeed, tailwindBenefit * 100, costMultiplier);
                        }
                    } else {
                        // Crosswind - wind from the side
                        // Scale impact based on wind speed: 0-12% penalty for 0-12 m/s winds
                        impactType = "CROSSWIND";
                        double crosswindPenalty = Math.min(
                            windSpeed * WeatherConstants.CROSSWIND_PENALTY_PER_MS,
                            WeatherConstants.MAX_CROSSWIND_PENALTY
                        );
                        costMultiplier = 1.0 + crosswindPenalty;

                        if (i == 0) {
                            System.out.printf("  WIND ANALYSIS: WindSpeed=%.2f m/s, Penalty=%.2f%%, Multiplier=%.4f%n",
                                    windSpeed, crosswindPenalty * 100, costMultiplier);
                        }
                    }

                    // Store segment analysis for detailed reporting
                    segmentAnalysis.add(new WindImpactAnalysis.PathSegmentWind(
                            segmentCount,
                            flightDirection,
                            windDirection.doubleValue(),
                            windSpeed,
                            impactType,
                            relativeWindAngle,
                            costMultiplier
                    ));

                    // Accumulate multipliers for averaging
                    totalCostMultiplier += costMultiplier;
                    segmentCount++;
                }
            }
        }

        // Step 4: Calculate average cost multiplier across all segments
        // This smooths out the impact across the entire path
        // Example: If 5 segments have multipliers [1.1, 1.2, 0.9, 1.0, 1.15], avg = 1.07
        double avgCostMultiplier = segmentCount > 0 ? totalCostMultiplier / segmentCount : 1.0;

        // Step 5: Apply multiplier to total cost
        // baseCost = number of moves (each move costs 1 unit)
        // windAdjustedCost = baseCost × avgMultiplier (accounts for wind resistance/assistance)
        double baseCost = deliveryPath.getTotalCost();
        double windAdjustedCost = baseCost * avgCostMultiplier;

        // Calculate percentage change for reporting
        // Positive % = cost increase (headwind/crosswind dominant)
        // Negative % = cost decrease (tailwind dominant)
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
}