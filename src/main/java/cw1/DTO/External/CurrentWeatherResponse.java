package cw1.DTO.External;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Current weather response from OpenWeatherMap API
 * Endpoint: /data/2.5/weather
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CurrentWeatherResponse {
    private List<WeatherData> weather;  // Weather conditions (array, usually 1 item)
    private MainData main;              // Main weather parameters
    private Integer visibility;         // Visibility in meters
    private Wind wind;                  // Wind data
    private Clouds clouds;              // Cloud coverage
    private Precipitation rain;         // Rain data (optional)
    private Precipitation snow;         // Snow data (optional)
    private Long dt;                    // Time of data calculation (Unix timestamp)
    private String name;                // City name
    private Integer timezone;           // Shift in seconds from UTC
}