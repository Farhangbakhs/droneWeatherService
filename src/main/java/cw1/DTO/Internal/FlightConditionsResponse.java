package cw1.DTO.Internal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response indicating if weather conditions are safe for drone flight
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FlightConditionsResponse {
    private Boolean isSafe;              // Overall safety assessment
    private String riskLevel;            // LOW, MEDIUM, HIGH, CRITICAL
    private List<String> warnings;       // Specific warnings (human-readable)
    private WeatherSummary currentConditions;
    private FlightSafetyMetrics metrics;
    private String weatherDataSource;    // "Using forecast data" or "Not in forecast range, using current weather"

    /**
     * Detailed metrics for each safety criterion
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FlightSafetyMetrics {
        private Boolean windSafe;        // Wind speed within safe limits
        private Boolean visibilitySafe;  // Visibility adequate
        private Boolean precipitationSafe; // No heavy rain/snow
        private Boolean temperatureSafe;   // Temperature within operational range

        // Threshold values (informational)
        private Double maxSafeWindSpeed;    // m/s
        private Integer minSafeVisibility;  // meters
        private Double maxSafePrecipitation; // mm/h
        private Double minSafeTemp;         // Celsius
        private Double maxSafeTemp;         // Celsius
    }
}