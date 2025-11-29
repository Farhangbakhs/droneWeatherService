package cw1.DTO.Internal;

import cw1.DTO.LngLat;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request to check if weather conditions are safe for drone flight
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FlightConditionsRequest {
    @NotNull(message = "Position is required")
    private LngLat position;

    private Long timestamp;  // Optional: Unix timestamp for forecast check
    // If null, checks current weather
}