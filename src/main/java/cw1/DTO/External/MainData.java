package cw1.DTO.External;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Main weather parameters from OpenWeatherMap API
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class MainData {
    private Double temp;         // Temperature (will be in Celsius with units=metric)

    @JsonProperty("feels_like")
    private Double feelsLike;    // Perceived temperature

    @JsonProperty("temp_min")
    private Double tempMin;      // Minimum temperature

    @JsonProperty("temp_max")
    private Double tempMax;      // Maximum temperature

    private Integer pressure;    // Atmospheric pressure (hPa)
    private Integer humidity;    // Humidity percentage

    @JsonProperty("sea_level")
    private Integer seaLevel;    // Atmospheric pressure at sea level

    @JsonProperty("grnd_level")
    private Integer grndLevel;   // Atmospheric pressure at ground level
}