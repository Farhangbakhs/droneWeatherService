package cw1.DTO.External;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 5-day weather forecast response from OpenWeatherMap API
 * Endpoint: /data/2.5/forecast
 * Contains forecast data with 3-hour step (40 items total)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ForecastResponse {
    private String cod;        // Internal parameter (response code)
    private Integer cnt;       // Number of forecast items
    private List<ForecastItem> list;  // List of forecast data points
}