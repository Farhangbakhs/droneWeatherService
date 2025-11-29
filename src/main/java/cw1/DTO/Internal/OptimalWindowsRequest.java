package cw1.DTO.Internal;

import cw1.DTO.LngLat;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request to find optimal delivery time windows based on weather
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OptimalWindowsRequest {
    @NotNull(message = "Position is required")
    private LngLat position;

    private Long startTime;  // Optional: Unix timestamp for search start (default: now)
    private Long endTime;    // Optional: Unix timestamp for search end (default: now + 5 days)
    private Integer minWindowDurationMinutes; // Optional: Minimum duration (default: 30)
}