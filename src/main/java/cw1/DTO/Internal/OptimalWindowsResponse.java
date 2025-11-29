package cw1.DTO.Internal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response containing optimal delivery time windows based on weather forecast
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OptimalWindowsResponse {
    private Integer totalWindows;
    private List<DeliveryWindow> windows;

    /**
     * A contiguous time period with safe weather conditions
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeliveryWindow {
        private Long startTime;        // Unix timestamp
        private Long endTime;          // Unix timestamp
        private String startDateTime;  // ISO format (human-readable)
        private String endDateTime;    // ISO format (human-readable)
        private Integer durationMinutes;
        private String safetyRating;   // EXCELLENT, GOOD, ACCEPTABLE
        private Double averageWindSpeed;
        private String averageCondition;
        private List<WeatherSummary> forecastDetails; // All forecasts in this window
    }
}