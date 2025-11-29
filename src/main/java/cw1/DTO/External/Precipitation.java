package cw1.DTO.External;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Precipitation data (rain/snow) from OpenWeatherMap API
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Precipitation {
    @JsonProperty("1h")
    private Double oneHour;   // Precipitation volume for last 1 hour (mm)

    @JsonProperty("3h")
    private Double threeHour; // Precipitation volume for last 3 hours (mm)
}