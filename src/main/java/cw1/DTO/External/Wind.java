package cw1.DTO.External;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Wind data from OpenWeatherMap API
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Wind {
    private Double speed;  // Wind speed in m/s (with units=metric)
    private Double deg;    // Wind direction in degrees (meteorological)
    private Double gust;   // Wind gust speed in m/s (optional)
}