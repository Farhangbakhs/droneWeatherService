package cw1.DTO.External;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Single forecast item from OpenWeatherMap 5-day forecast
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ForecastItem {
    private Long dt;                    // Time of data forecasted (Unix timestamp)
    private MainData main;              // Weather parameters
    private List<WeatherData> weather;  // Weather conditions
    private Clouds clouds;              // Cloud coverage
    private Wind wind;                  // Wind data
    private Integer visibility;         // Visibility in meters

    @JsonProperty("pop")
    private Double probabilityOfPrecipitation;  // Probability (0-1)

    private Precipitation rain;         // Rain forecast (optional)
    private Precipitation snow;         // Snow forecast (optional)

    @JsonProperty("dt_txt")
    private String dtTxt;              // Time in ISO format (e.g., "2025-01-01 12:00:00")
}