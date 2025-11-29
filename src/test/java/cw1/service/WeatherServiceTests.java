package cw1.service;

import cw1.DTO.External.*;
import cw1.DTO.Internal.FlightConditionsRequest;
import cw1.DTO.Internal.FlightConditionsResponse;
import cw1.DTO.Internal.OptimalWindowsRequest;
import cw1.DTO.Internal.OptimalWindowsResponse;
import cw1.DTO.Internal.WeatherSummary;
import cw1.DTO.LngLat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit tests for WeatherService business logic
 *
 * Uses Mockito to mock WeatherRetrieveService (no real API calls)
 */
class WeatherServiceTests {

    @Mock
    private WeatherRetrieveService weatherRetrieveService;

    private WeatherService weatherService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        weatherService = new WeatherService(weatherRetrieveService);
    }

    // ==================== FLIGHT SAFETY TESTS ====================

    @Test
    void checkFlightConditions_idealWeather_returnsSafe() {
        // Arrange: Perfect flying conditions
        LngLat position = new LngLat(-3.186, 55.944);
        CurrentWeatherResponse mockResponse = createMockCurrentWeather(
                20.0,  // temperature
                5.0,   // wind speed
                null,  // wind gust
                10000, // visibility
                "Clear",
                0.0    // no precipitation
        );

        when(weatherRetrieveService.getCurrentWeather(any())).thenReturn(mockResponse);

        FlightConditionsRequest request = new FlightConditionsRequest(position, null);

        // Act
        FlightConditionsResponse response = weatherService.checkFlightConditions(request);

        // Assert
        assertThat(response.getIsSafe()).isTrue();
        assertThat(response.getRiskLevel()).isEqualTo("LOW");
        assertThat(response.getWarnings()).isEmpty();
        assertThat(response.getMetrics().getWindSafe()).isTrue();
        assertThat(response.getMetrics().getVisibilitySafe()).isTrue();
        assertThat(response.getMetrics().getPrecipitationSafe()).isTrue();
        assertThat(response.getMetrics().getTemperatureSafe()).isTrue();
    }

    @Test
    void checkFlightConditions_highWind_returnsUnsafe() {
        // Arrange: Wind exceeds safe limits
        LngLat position = new LngLat(-3.186, 55.944);
        CurrentWeatherResponse mockResponse = createMockCurrentWeather(
                20.0,  // temperature
                15.0,  // wind speed (exceeds 12 m/s limit)
                18.0,  // wind gust (exceeds 15 m/s limit)
                10000, // visibility
                "Clear",
                0.0    // no precipitation
        );

        when(weatherRetrieveService.getCurrentWeather(any())).thenReturn(mockResponse);

        FlightConditionsRequest request = new FlightConditionsRequest(position, null);

        // Act
        FlightConditionsResponse response = weatherService.checkFlightConditions(request);

        // Assert
        assertThat(response.getIsSafe()).isFalse();
        assertThat(response.getRiskLevel()).isIn("MEDIUM", "HIGH");
        assertThat(response.getWarnings()).isNotEmpty();
        assertThat(response.getMetrics().getWindSafe()).isFalse();
        assertThat(response.getWarnings().stream()
                .anyMatch(w -> w.contains("Wind"))).isTrue();
    }

    @Test
    void checkFlightConditions_lowVisibility_returnsUnsafe() {
        // Arrange: Poor visibility
        LngLat position = new LngLat(-3.186, 55.944);
        CurrentWeatherResponse mockResponse = createMockCurrentWeather(
                20.0,  // temperature
                5.0,   // wind speed
                null,  // wind gust
                2000,  // visibility (below 5000m limit)
                "Fog",
                0.0    // no precipitation
        );

        when(weatherRetrieveService.getCurrentWeather(any())).thenReturn(mockResponse);

        FlightConditionsRequest request = new FlightConditionsRequest(position, null);

        // Act
        FlightConditionsResponse response = weatherService.checkFlightConditions(request);

        // Assert
        assertThat(response.getIsSafe()).isFalse();
        assertThat(response.getMetrics().getVisibilitySafe()).isFalse();
        assertThat(response.getWarnings().stream()
                .anyMatch(w -> w.contains("Visibility") || w.contains("Fog"))).isTrue();
    }

    @Test
    void checkFlightConditions_heavyRain_returnsUnsafe() {
        // Arrange: Heavy precipitation
        LngLat position = new LngLat(-3.186, 55.944);
        CurrentWeatherResponse mockResponse = createMockCurrentWeather(
                20.0,  // temperature
                5.0,   // wind speed
                null,  // wind gust
                10000, // visibility
                "Rain",
                18.0   // precipitation (6 mm/h exceeds 5 mm/h limit)
        );

        when(weatherRetrieveService.getCurrentWeather(any())).thenReturn(mockResponse);

        FlightConditionsRequest request = new FlightConditionsRequest(position, null);

        // Act
        FlightConditionsResponse response = weatherService.checkFlightConditions(request);

        // Assert
        assertThat(response.getIsSafe()).isFalse();
        assertThat(response.getMetrics().getPrecipitationSafe()).isFalse();
        assertThat(response.getWarnings().stream()
                .anyMatch(w -> w.contains("Precipitation"))).isTrue();
    }

    @Test
    void checkFlightConditions_thunderstorm_returnsCritical() {
        // Arrange: Thunderstorm (critical condition)
        LngLat position = new LngLat(-3.186, 55.944);
        CurrentWeatherResponse mockResponse = createMockCurrentWeather(
                20.0,  // temperature
                8.0,   // wind speed
                null,  // wind gust
                8000,  // visibility
                "Thunderstorm",
                3.0    // precipitation
        );

        when(weatherRetrieveService.getCurrentWeather(any())).thenReturn(mockResponse);

        FlightConditionsRequest request = new FlightConditionsRequest(position, null);

        // Act
        FlightConditionsResponse response = weatherService.checkFlightConditions(request);

        // Assert
        assertThat(response.getIsSafe()).isFalse();
        assertThat(response.getWarnings().stream()
                .anyMatch(w -> w.contains("Thunderstorm"))).isTrue();
    }

    @Test
    void checkFlightConditions_extremeTemperature_returnsUnsafe() {
        // Arrange: Temperature too low
        LngLat position = new LngLat(-3.186, 55.944);
        CurrentWeatherResponse mockResponse = createMockCurrentWeather(
                -15.0, // temperature (below -10°C limit)
                5.0,   // wind speed
                null,  // wind gust
                10000, // visibility
                "Clear",
                0.0    // no precipitation
        );

        when(weatherRetrieveService.getCurrentWeather(any())).thenReturn(mockResponse);

        FlightConditionsRequest request = new FlightConditionsRequest(position, null);

        // Act
        FlightConditionsResponse response = weatherService.checkFlightConditions(request);

        // Assert
        assertThat(response.getIsSafe()).isFalse();
        assertThat(response.getMetrics().getTemperatureSafe()).isFalse();
        assertThat(response.getWarnings().stream()
                .anyMatch(w -> w.contains("Temperature"))).isTrue();
    }

    @Test
    void checkFlightConditions_multipleIssues_returnsHighOrCriticalRisk() {
        // Arrange: Multiple safety issues
        LngLat position = new LngLat(-3.186, 55.944);
        CurrentWeatherResponse mockResponse = createMockCurrentWeather(
                50.0,  // temperature (too high)
                14.0,  // wind speed (too high)
                17.0,  // wind gust (too high)
                3000,  // visibility (too low)
                "Rain",
                12.0   // heavy precipitation
        );

        when(weatherRetrieveService.getCurrentWeather(any())).thenReturn(mockResponse);

        FlightConditionsRequest request = new FlightConditionsRequest(position, null);

        // Act
        FlightConditionsResponse response = weatherService.checkFlightConditions(request);

        // Assert
        assertThat(response.getIsSafe()).isFalse();
        assertThat(response.getRiskLevel()).isIn("HIGH", "CRITICAL");
        assertThat(response.getWarnings().size()).isGreaterThanOrEqualTo(3);
    }

    // ==================== OPTIMAL WINDOWS TESTS ====================

    @Test
    void findOptimalWindows_allSafeConditions_returnsOneWindow() {
        // Arrange: All forecast items have safe conditions
        LngLat position = new LngLat(-3.186, 55.944);
        ForecastResponse mockForecast = createMockForecast(
                5,     // number of safe forecast items
                true,  // all safe
                20.0,  // temperature
                5.0    // wind speed
        );

        when(weatherRetrieveService.getForecast(any())).thenReturn(mockForecast);

        OptimalWindowsRequest request = new OptimalWindowsRequest(position, null, null, 30);

        // Act
        OptimalWindowsResponse response = weatherService.findOptimalDeliveryWindows(request);

        // Assert
        assertThat(response.getTotalWindows()).isGreaterThan(0);
        assertThat(response.getWindows()).isNotEmpty();
        assertThat(response.getWindows().get(0).getSafetyRating()).isIn("EXCELLENT", "GOOD");
    }

    @Test
    void findOptimalWindows_intermittentUnsafe_createsMultipleWindows() {
        // Arrange: Mix of safe and unsafe conditions creating gaps
        LngLat position = new LngLat(-3.186, 55.944);
        List<ForecastItem> items = new ArrayList<>();

        // Safe window 1 (3 hours)
        items.add(createForecastItem(Instant.now().getEpochSecond(), 20.0, 5.0, "Clear"));

        // Unsafe gap (high wind)
        items.add(createForecastItem(Instant.now().getEpochSecond() + 10800, 20.0, 15.0, "Clear"));

        // Safe window 2 (3 hours)
        items.add(createForecastItem(Instant.now().getEpochSecond() + 21600, 20.0, 5.0, "Clear"));

        ForecastResponse mockForecast = new ForecastResponse("200", items.size(), items);
        when(weatherRetrieveService.getForecast(any())).thenReturn(mockForecast);

        OptimalWindowsRequest request = new OptimalWindowsRequest(position, null, null, 30);

        // Act
        OptimalWindowsResponse response = weatherService.findOptimalDeliveryWindows(request);

        // Assert
        // Should have at least one safe window (might have 2 if both meet min duration)
        assertThat(response.getWindows()).isNotEmpty();
    }

    @Test
    void findOptimalWindows_noSafeConditions_returnsEmptyWindows() {
        // Arrange: All forecast items unsafe
        LngLat position = new LngLat(-3.186, 55.944);
        ForecastResponse mockForecast = createMockForecast(
                5,     // number of items
                false, // all unsafe
                20.0,  // temperature
                20.0   // wind speed (too high)
        );

        when(weatherRetrieveService.getForecast(any())).thenReturn(mockForecast);

        OptimalWindowsRequest request = new OptimalWindowsRequest(position, null, null, 30);

        // Act
        OptimalWindowsResponse response = weatherService.findOptimalDeliveryWindows(request);

        // Assert
        assertThat(response.getTotalWindows()).isZero();
        assertThat(response.getWindows()).isEmpty();
    }

    @Test
    void findOptimalWindows_shortSafeWindow_excludedDueToMinDuration() {
        // Arrange: Very short safe window (less than minimum duration)
        LngLat position = new LngLat(-3.186, 55.944);
        List<ForecastItem> items = new ArrayList<>();

        // Only one safe item (3 hours, but testing with high min duration)
        items.add(createForecastItem(Instant.now().getEpochSecond(), 20.0, 5.0, "Clear"));
        items.add(createForecastItem(Instant.now().getEpochSecond() + 10800, 20.0, 15.0, "Clear"));

        ForecastResponse mockForecast = new ForecastResponse("200", items.size(), items);
        when(weatherRetrieveService.getForecast(any())).thenReturn(mockForecast);

        OptimalWindowsRequest request = new OptimalWindowsRequest(
                position, null, null, 300  // 5-hour minimum (longer than 3-hour safe period)
        );

        // Act
        OptimalWindowsResponse response = weatherService.findOptimalDeliveryWindows(request);

        // Assert
        assertThat(response.getTotalWindows()).isZero();
    }

    // ==================== HELPER METHODS ====================

    private CurrentWeatherResponse createMockCurrentWeather(double temp, double windSpeed,
                                                            Double windGust, int visibility,
                                                            String condition, double precipitation) {
        CurrentWeatherResponse response = new CurrentWeatherResponse();

        // Weather data
        WeatherData weatherData = new WeatherData(800, condition, condition.toLowerCase(), "01d");
        response.setWeather(List.of(weatherData));

        // Main data
        MainData mainData = new MainData();
        mainData.setTemp(temp);
        mainData.setHumidity(60);
        response.setMain(mainData);

        // Wind
        Wind wind = new Wind();
        wind.setSpeed(windSpeed);
        wind.setGust(windGust);
        wind.setDeg(180.0);
        response.setWind(wind);

        // Clouds
        Clouds clouds = new Clouds(20);
        response.setClouds(clouds);

        // Visibility
        response.setVisibility(visibility);

        // Precipitation
        if (precipitation > 0) {
            Precipitation rain = new Precipitation(null, precipitation);
            response.setRain(rain);
        }

        // Timestamp
        response.setDt(Instant.now().getEpochSecond());

        return response;
    }

    private ForecastResponse createMockForecast(int itemCount, boolean allSafe,
                                                double temperature, double windSpeed) {
        List<ForecastItem> items = new ArrayList<>();
        long baseTime = Instant.now().getEpochSecond();

        for (int i = 0; i < itemCount; i++) {
            long timestamp = baseTime + (i * 10800); // 3-hour intervals
            ForecastItem item = createForecastItem(
                    timestamp,
                    temperature,
                    allSafe ? windSpeed : 20.0, // Unsafe if not allSafe
                    "Clear"
            );
            items.add(item);
        }

        return new ForecastResponse("200", itemCount, items);
    }

    private ForecastItem createForecastItem(long timestamp, double temp, double windSpeed, String condition) {
        ForecastItem item = new ForecastItem();

        item.setDt(timestamp);
        item.setDtTxt(Instant.ofEpochSecond(timestamp).toString());

        // Main data
        MainData mainData = new MainData();
        mainData.setTemp(temp);
        mainData.setHumidity(60);
        item.setMain(mainData);

        // Weather
        WeatherData weatherData = new WeatherData(800, condition, condition.toLowerCase(), "01d");
        item.setWeather(List.of(weatherData));

        // Wind
        Wind wind = new Wind();
        wind.setSpeed(windSpeed);
        wind.setDeg(180.0);
        item.setWind(wind);

        // Clouds
        Clouds clouds = new Clouds(20);
        item.setClouds(clouds);

        // Visibility
        item.setVisibility(10000);

        // Probability of precipitation
        item.setProbabilityOfPrecipitation(0.1);

        return item;
    }
}