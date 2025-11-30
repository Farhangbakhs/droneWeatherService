package cw1.service;

import cw1.DTO.External.CurrentWeatherResponse;
import cw1.DTO.External.ForecastResponse;
import cw1.DTO.LngLat;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
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
     * Get current weather for a specific location.
     *
     * <p>API: GET /data/2.5/weather?lat={lat}&lon={lon}&appid={key}&units=metric
     *
     * @param position Geographic coordinates (longitude, latitude)
     * @return Current weather data including temperature, wind, precipitation
     * @throws ResponseStatusException with 503 status if weather service is unavailable
     * @throws IllegalArgumentException if position is null
     */
    public CurrentWeatherResponse getCurrentWeather(LngLat position) {
        if (position == null || position.getLat() == null || position.getLng() == null) {
            throw new IllegalArgumentException("Position coordinates cannot be null");
        }

        try {
            String url = UriComponentsBuilder
                    .fromHttpUrl(baseUrl)
                    .pathSegment("data", "2.5", "weather")
                    .queryParam("lat", position.getLat())
                    .queryParam("lon", position.getLng())
                    .queryParam("appid", apiKey)
                    .queryParam("units", "metric")  // Get temperature in Celsius
                    .toUriString();

            CurrentWeatherResponse response = restTemplate.getForObject(url, CurrentWeatherResponse.class);

            if (response == null) {
                throw new ResponseStatusException(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "Weather service returned empty response"
                );
            }

            return response;

        } catch (RestClientException e) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Weather service unavailable: " + e.getMessage(),
                    e
            );
        }
    }

    /**
     * Get 5-day weather forecast with 3-hour intervals for a specific location.
     *
     * <p>API: GET /data/2.5/forecast?lat={lat}&lon={lon}&appid={key}&units=metric
     *
     * <p>Returns 40 forecast items (5 days × 8 intervals per day). Each interval
     * provides weather conditions at 3-hour steps (00:00, 03:00, 06:00, ...).
     *
     * @param position Geographic coordinates (longitude, latitude)
     * @return Forecast data with 40 time-stamped weather predictions
     * @throws ResponseStatusException with 503 status if weather service is unavailable
     * @throws IllegalArgumentException if position is null
     */
    public ForecastResponse getForecast(LngLat position) {
        if (position == null || position.getLat() == null || position.getLng() == null) {
            throw new IllegalArgumentException("Position coordinates cannot be null");
        }

        try {
            String url = UriComponentsBuilder
                    .fromHttpUrl(baseUrl)
                    .pathSegment("data", "2.5", "forecast")
                    .queryParam("lat", position.getLat())
                    .queryParam("lon", position.getLng())
                    .queryParam("appid", apiKey)
                    .queryParam("units", "metric")  // Get temperature in Celsius
                    .toUriString();

            ForecastResponse response = restTemplate.getForObject(url, ForecastResponse.class);

            if (response == null || response.getList() == null || response.getList().isEmpty()) {
                throw new ResponseStatusException(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "Weather service returned empty or invalid forecast response"
                );
            }

            return response;

        } catch (RestClientException e) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Weather forecast service unavailable: " + e.getMessage(),
                    e
            );
        }
    }
}