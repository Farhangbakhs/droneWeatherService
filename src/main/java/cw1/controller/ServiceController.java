// src/main/java/cw1/controller/ServiceController.java
package cw1.controller;

import cw1.DTO.External.Drone;
import cw1.DTO.*; // your request DTOs (DistanceRequest, NextPositionRequest, RegionRequest, LngLat, etc.)
import cw1.DTO.Internal.CalcDeliveryPathResult;
import cw1.DTO.Internal.FlightConditionsRequest;
import cw1.DTO.Internal.FlightConditionsResponse;
import cw1.DTO.Internal.OptimalWindowsRequest;
import cw1.DTO.Internal.OptimalWindowsResponse;
import cw1.DTO.Internal.WeatherBlockedResponse;
import cw1.DTO.Internal.WeatherAwareDeliveryResult;
import cw1.DTO.Internal.WeatherAwareDeliveryRequest;
import cw1.DTO.Internal.WindImpactAnalysis;
import cw1.service.CalculationService;
import cw1.service.DroneService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import cw1.DTO.Internal.CalcDeliveryPathResult;
import cw1.DTO.Internal.GeoJsonLineString;
import cw1.DTO.MedDispatchRec;


import java.util.List;


@RestController
@RequestMapping("/api/v1")
public class ServiceController {

    private final CalculationService calculationService;
    private final DroneService droneService;
    private final cw1.service.WeatherService weatherService;
    private final ObjectMapper objectMapper;

    @Autowired
    public ServiceController(CalculationService calculationService,
                             DroneService droneService,
                             cw1.service.WeatherService weatherService,
                             ObjectMapper objectMapper) {
        this.calculationService = calculationService;
        this.droneService = droneService;
        this.weatherService = weatherService;
        this.objectMapper = objectMapper;
    }



    @GetMapping("/uid")
    public String uid() {
        return "s2324780";
    }

    @PostMapping("/isCloseTo")
    public ResponseEntity<Boolean> isCloseTo(@Valid @RequestBody DistanceRequest req) {
        return ResponseEntity.ok(
                calculationService.isCloseTo(req.getPosition1(), req.getPosition2())
        );
    }

    @PostMapping("/distanceTo")
    public ResponseEntity<Double> distanceTo(@Valid @RequestBody DistanceRequest req) {
        return ResponseEntity.ok(
                calculationService.calculateDistance(req.getPosition1(), req.getPosition2())
        );
    }

    @PostMapping("/nextPosition")
    public ResponseEntity<LngLat> nextPosition(@Valid @RequestBody NextPositionRequest req) {
        return ResponseEntity.ok(
                calculationService.nextPosition(req.getStart(), req.getAngle())
        );
    }

    @PostMapping("/isInRegion")
    public ResponseEntity<Boolean> isInRegion(@Valid @RequestBody RegionRequest req) {
        return ResponseEntity.ok(
                calculationService.isInRegion(req.getPosition(), req.getRegion().getVertices())
        );
    }



    @GetMapping("/dronesWithCooling/{state}")
    public List<String> hasCooling(@PathVariable boolean state) {
        return droneService.hasCooling(state);
    }


    @GetMapping("/droneDetails/{id}")
    public Drone getDetails(@PathVariable String id) {
        return droneService.getDetails(id);
    }

    @GetMapping("/queryAsPath/{attributeName}/{attributeValue}")
        public List<String> getQuery(@PathVariable String attributeName, @PathVariable String attributeValue) {
        return droneService.getQuery(attributeName, attributeValue);
    }

    @PostMapping("/query")
    public List<String> query(@Valid @RequestBody List<QueryAttribute> queryAttributes) {
        return droneService.query(queryAttributes);
    }

    @PostMapping("/queryAvailableDrones")
    public List<String> queryAvailableDrones( @RequestBody List<MedDispatchRec> medDispatchRecReq) {
        return droneService.queryAvailableDrones(medDispatchRecReq);
    }


    @PostMapping("/calcDeliveryPath")
    public CalcDeliveryPathResult calcDeliveryPath(@Valid @RequestBody List<MedDispatchRec> body) {
        return droneService.calcDeliveryPath(body);
    }

    @PostMapping("/calcDeliveryPathAsGeoJson")
    public GeoJsonLineString calcDeliveryPathAsGeoJson(@Valid @RequestBody List<MedDispatchRec> body) {
        return droneService.calcDeliveryPathAsGeoJson(body);
    }

    /**
     * Weather-aware delivery path calculation endpoint (CW3 EXTENSION)
     *
     * This is a modular extension built ON TOP of the original CW2 calcDeliveryPath logic.
     * It enhances the delivery system with real-time weather intelligence.
     *
     * Key Features:
     * - If date/time are provided in MedDispatchRec, uses FORECAST weather data
     * - If date/time are null, uses CURRENT weather data
     * - Falls back gracefully to standard calcDeliveryPath if weather service fails
     * - Preserves all CW2 drone selection and pathfinding logic
     * - TESTING MODE: Supports explicit service point to bypass slow ILP queries
     *
     * Workflow:
     * 1. Check weather safety at delivery location (current or forecasted)
     * 2. If unsafe → Return alternative safe delivery windows
     * 3. If safe → Calculate path using original CW2 logic + add weather enrichment
     *
     * POST /api/v1/calcWeatherAwareDeliveryPath
     *
     * Request body: WeatherAwareDeliveryRequest OR List<MedDispatchRec> (backward compatible)
     * - dispatches: List of delivery dispatches
     * - explicitServicePoint (OPTIONAL): For testing mode - bypasses service point lookup
     * - skipServicePointSelection (OPTIONAL): Set to true to enable testing mode
     *
     * Testing Mode Benefits:
     * - Dramatically faster for international coordinates (30-60s → 2-3s)
     * - Skips ILP service point endpoint query
     * - Uses explicit coordinates directly
     *
     * Response:
     * - If unsafe: WeatherBlockedResponse with risk info and optimal windows
     * - If safe: WeatherAwareDeliveryResult with delivery path and weather conditions
     *
     * @param request WeatherAwareDeliveryRequest or List<MedDispatchRec> (polymorphic)
     * @return ResponseEntity containing either WeatherBlockedResponse or WeatherAwareDeliveryResult
     */
    @PostMapping("/calcWeatherAwareDeliveryPath")
    public ResponseEntity<?> calcWeatherAwareDeliveryPath(@RequestBody Object request) {
        // Step 1: Parse and deserialize the polymorphic request
        WeatherAwareDeliveryRequest deliveryRequest = parseDeliveryRequest(request);
        if (deliveryRequest == null) {
            return createErrorResponse("Invalid Request Format", "Failed to parse request");
        }

        // Step 2: Validate request structure and data
        ResponseEntity<?> validationError = validateDeliveryRequest(deliveryRequest);
        if (validationError != null) {
            return validationError;
        }

        // Step 3: Extract request data
        List<MedDispatchRec> dispatches = deliveryRequest.getDispatches();
        LngLat location = dispatches.get(0).getDelivery();
        java.time.LocalDate date = dispatches.get(0).getDate();
        java.time.LocalTime time = dispatches.get(0).getTime();

        // Step 4: Log operating mode for debugging
        logOperatingMode(deliveryRequest);

        // Step 5: Check weather safety conditions
        FlightConditionsResponse conditions = weatherService.checkFlightConditionsAtDateTime(
                location, date, time);

        // Step 6: If weather is unsafe, return alternative delivery windows
        if (!conditions.getIsSafe()) {
            return handleUnsafeWeather(conditions, location);
        }

        // Step 7: Weather is safe - calculate path and analyze wind impact
        return calculateSafeDeliveryPath(deliveryRequest, conditions, location, date, time);
    }

    /**
     * Parse polymorphic request (List or Map) into WeatherAwareDeliveryRequest.
     *
     * <p>Handles backward compatibility by accepting both:
     * <ul>
     *   <li>Legacy format: {@code List<MedDispatchRec>}</li>
     *   <li>New format: {@code WeatherAwareDeliveryRequest} (arrives as Map from JSON)</li>
     * </ul>
     *
     * @param request Raw request object from @RequestBody
     * @return Parsed WeatherAwareDeliveryRequest, or null if parsing fails
     */
    private WeatherAwareDeliveryRequest parseDeliveryRequest(Object request) {
        try {
            if (request instanceof List<?>) {
                // Legacy format: List<MedDispatchRec>
                List<MedDispatchRec> dispatches = objectMapper.convertValue(
                        request,
                        new TypeReference<List<MedDispatchRec>>() {}
                );
                return new WeatherAwareDeliveryRequest(dispatches);
            } else if (request instanceof java.util.Map<?, ?>) {
                // New format: WeatherAwareDeliveryRequest
                return objectMapper.convertValue(request, WeatherAwareDeliveryRequest.class);
            }
        } catch (Exception e) {
            System.err.println("Failed to parse request: " + e.getMessage());
        }
        return null;
    }

    /**
     * Validate delivery request structure and data integrity.
     *
     * @param deliveryRequest Request to validate
     * @return ResponseEntity with error if validation fails, null if valid
     */
    private ResponseEntity<?> validateDeliveryRequest(WeatherAwareDeliveryRequest deliveryRequest) {
        // Validate request structure
        try {
            deliveryRequest.validate();
        } catch (IllegalArgumentException e) {
            return createErrorResponse("Invalid Request", e.getMessage());
        }

        // Validate delivery location exists
        if (deliveryRequest.getDispatches().get(0).getDelivery() == null) {
            return createErrorResponse("Invalid Request", "Delivery location cannot be null");
        }

        return null; // Valid
    }

    /**
     * Log the operating mode (normal or testing) for debugging.
     *
     * @param deliveryRequest Request containing mode information
     */
    private void logOperatingMode(WeatherAwareDeliveryRequest deliveryRequest) {
        if (deliveryRequest.isTestingMode()) {
            System.out.println(" TESTING MODE ACTIVATED");
            System.out.println("   Explicit Service Point: " + deliveryRequest.getExplicitServicePoint());
            System.out.println("   Skipping ILP service point endpoint query");
        } else {
            System.out.println(" NORMAL MODE: Auto-selecting nearest service point from ILP data");
        }
    }

    /**
     * Handle unsafe weather conditions by returning alternative delivery windows.
     *
     * @param conditions Flight safety assessment
     * @param location Delivery location
     * @return ResponseEntity with WeatherBlockedResponse
     */
    private ResponseEntity<?> handleUnsafeWeather(FlightConditionsResponse conditions, LngLat location) {
        // Find optimal delivery windows (default 60 minutes minimum window)
        OptimalWindowsRequest windowReq = new OptimalWindowsRequest(location, null, null, 60);
        OptimalWindowsResponse windows = weatherService.findOptimalDeliveryWindows(windowReq);

        return ResponseEntity.ok(new WeatherBlockedResponse(
                conditions.getRiskLevel(),
                conditions.getWarnings(),
                windows.getWindows()
        ));
    }

    /**
     * Calculate delivery path with safe weather and analyze wind impact.
     *
     * @param deliveryRequest Full request with mode information
     * @param conditions Weather safety assessment
     * @param location Delivery location
     * @param date Optional delivery date for forecast
     * @param time Optional delivery time for forecast
     * @return ResponseEntity with WeatherAwareDeliveryResult or error
     */
    private ResponseEntity<?> calculateSafeDeliveryPath(
            WeatherAwareDeliveryRequest deliveryRequest,
            FlightConditionsResponse conditions,
            LngLat location,
            java.time.LocalDate date,
            java.time.LocalTime time) {

        try {
            List<MedDispatchRec> dispatches = deliveryRequest.getDispatches();

            // Calculate path based on mode (testing vs normal)
            CalcDeliveryPathResult pathResult;
            if (deliveryRequest.isTestingMode()) {
                // TESTING MODE: Use explicit service point to bypass slow ILP query
                pathResult = droneService.calcDeliveryPathWithExplicitServicePoint(
                        dispatches,
                        deliveryRequest.getExplicitServicePoint()
                );
            } else {
                // NORMAL MODE: Auto-select nearest service point
                pathResult = droneService.calcDeliveryPath(dispatches);
            }

            // Analyze wind impact on the calculated path
            WindImpactAnalysis windImpact = weatherService.analyzeWindImpactAtDateTime(
                    pathResult, location, date, time);

            // Return enriched result with weather data and wind impact
            return ResponseEntity.ok(new WeatherAwareDeliveryResult(pathResult, conditions, windImpact));

        } catch (IllegalArgumentException e) {
            // No drone can fulfill requirements (capacity exceeded, max moves exceeded, etc.)
            return createErrorResponse("Invalid Dispatch Request", e.getMessage());
        }
    }

    /**
     * Create a standardized error response.
     *
     * @param error Error type/category
     * @param message Detailed error message
     * @return ResponseEntity with 400 Bad Request status
     */
    private ResponseEntity<?> createErrorResponse(String error, String message) {
        return ResponseEntity.badRequest().body(java.util.Map.of(
                "error", error,
                "message", message
        ));
    }

}



