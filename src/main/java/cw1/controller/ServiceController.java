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
import cw1.DTO.Internal.WindImpactAnalysis;
import cw1.service.CalculationService;
import cw1.service.DroneService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
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

    @Autowired
    public ServiceController(CalculationService calculationService,
                             DroneService droneService,
                             cw1.service.WeatherService weatherService) {
        this.calculationService = calculationService;
        this.droneService = droneService;
        this.weatherService = weatherService;
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
     * Weather-aware delivery path calculation endpoint
     *
     * Checks weather conditions before calculating delivery path.
     * If conditions are unsafe, returns alternative delivery windows.
     * If safe, calculates path and enriches with weather data.
     *
     * POST /api/v1/calcWeatherAwareDeliveryPath
     *
     * Request body: List of MedDispatchRec (same as calcDeliveryPath)
     *
     * Response:
     * - If unsafe: WeatherBlockedResponse with risk info and optimal windows
     * - If safe: WeatherAwareDeliveryResult with delivery path and weather conditions
     *
     * @param dispatches List of delivery dispatches
     * @return ResponseEntity containing either WeatherBlockedResponse or WeatherAwareDeliveryResult
     */
    @PostMapping("/calcWeatherAwareDeliveryPath")
    public ResponseEntity<?> calcWeatherAwareDeliveryPath(
            @Valid @RequestBody List<MedDispatchRec> dispatches) {

        // Validate input
        if (dispatches == null || dispatches.isEmpty()) {
            throw new IllegalArgumentException("Dispatches list cannot be empty");
        }

        // 1. Get delivery location from first dispatch
        LngLat location = dispatches.get(0).getDelivery();
        if (location == null) {
            throw new IllegalArgumentException("Delivery location cannot be null");
        }

        // 2. Check if safe to fly at this location (current weather)
        FlightConditionsRequest weatherReq = new FlightConditionsRequest(location, null);
        FlightConditionsResponse conditions = weatherService.checkFlightConditions(weatherReq);

        // 3. If unsafe, return weather info + optimal windows instead of calculating path
        if (!conditions.getIsSafe()) {
            // Find optimal delivery windows (default 60 minutes minimum window)
            OptimalWindowsRequest windowReq = new OptimalWindowsRequest(location, null, null, 60);
            OptimalWindowsResponse windows = weatherService.findOptimalDeliveryWindows(windowReq);

            return ResponseEntity.ok(new WeatherBlockedResponse(
                conditions.getRiskLevel(),
                conditions.getWarnings(),
                windows.getWindows()
            ));
        }

        // 4. Weather is safe - calculate delivery path normally
        CalcDeliveryPathResult pathResult = droneService.calcDeliveryPath(dispatches);

        // 5. Analyze wind impact on the delivery path for cost adjustments
        WindImpactAnalysis windImpact = weatherService.analyzeWindImpact(pathResult, location);

        // 6. Return enriched result with weather data and wind impact analysis
        return ResponseEntity.ok(new WeatherAwareDeliveryResult(pathResult, conditions, windImpact));
    }

}



