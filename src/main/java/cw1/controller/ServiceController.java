// src/main/java/cw1/controller/ServiceController.java
package cw1.controller;

import cw1.DTO.External.Drone;
import cw1.DTO.*; // your request DTOs (DistanceRequest, NextPositionRequest, RegionRequest, LngLat, etc.)
import cw1.DTO.Internal.CalcDeliveryPathResult;
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

    @Autowired
    public ServiceController(CalculationService calculationService,
                             DroneService droneService) {
        this.calculationService = calculationService;
        this.droneService = droneService;
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

}



