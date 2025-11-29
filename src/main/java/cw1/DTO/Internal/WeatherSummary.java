package cw1.DTO.Internal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Simplified weather summary for drone operations
 * Used for both current weather and forecast items
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WeatherSummary {
    private Long timestamp;              // Unix timestamp
    private String dateTime;             // ISO format date-time (human-readable)
    private Double temperatureCelsius;   // Temperature in Celsius
    private Double windSpeedMps;         // Wind speed in m/s
    private Double windGustMps;          // Wind gust in m/s (optional)
    private Integer windDirectionDeg;    // Wind direction in degrees
    private String condition;            // Main condition (Clear, Rain, Snow, etc.)
    private String description;          // Detailed description
    private Integer visibility;          // Visibility in meters
    private Integer humidity;            // Humidity percentage
    private Double precipitationMm;      // Total precipitation in mm
    private Double probabilityOfPrecipitation; // 0-1, only for forecasts
    private Integer cloudCoverage;       // Cloud coverage percentage
}