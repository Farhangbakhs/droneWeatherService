package cw1.service;

import cw1.DTO.External.CurrentWeatherResponse;
import cw1.DTO.External.ForecastResponse;
import cw1.DTO.LngLat;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Service for retrieving weather data from OpenWeatherMap API
 *
 * This service handles all external API communication and provides
 * a clean interface for the business logic layer.
 */
@Service
public class WeatherRetrieveService {

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final String apiKey;

    public WeatherRetrieveService(RestTemplate restTemplate,
                                  @Value("${weather.api.endpoint}") String baseUrl,
                                  @Value("${weather.api.key}") String apiKey) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
    }

    /**
     * Get current weather for a specific location
     *
     * API: GET /data/2.5/weather?lat={lat}&lon={lon}&appid={key}&units=metric
     *
     * @param position LngLat position
     * @return Current weather data
     */
    public CurrentWeatherResponse getCurrentWeather(LngLat position) {
        String url = UriComponentsBuilder
                .fromHttpUrl(baseUrl)
                .pathSegment("data", "2.5", "weather")
                .queryParam("lat", position.getLat())
                .queryParam("lon", position.getLng())
                .queryParam("appid", apiKey)
                .queryParam("units", "metric")  // Get temperature in Celsius
                .toUriString();

        return restTemplate.getForObject(url, CurrentWeatherResponse.class);
    }

    /**
     * Get 5-day weather forecast with 3-hour intervals for a specific location
     *
     * API: GET /data/2.5/forecast?lat={lat}&lon={lon}&appid={key}&units=metric
     * Returns 40 forecast items (5 days × 8 intervals per day)
     *
     * @param position LngLat position
     * @return Forecast data
     */
    public ForecastResponse getForecast(LngLat position) {
        String url = UriComponentsBuilder
                .fromHttpUrl(baseUrl)
                .pathSegment("data", "2.5", "forecast")
                .queryParam("lat", position.getLat())
                .queryParam("lon", position.getLng())
                .queryParam("appid", apiKey)
                .queryParam("units", "metric")  // Get temperature in Celsius
                .toUriString();

        return restTemplate.getForObject(url, ForecastResponse.class);
    }
}