
package cw1.service;

import cw1.DTO.External.Drone;

import cw1.DTO.Internal.CapabilityDTO;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

@Service
public class DroneService {

    private final RetrieveService retrieveService;

    public DroneService(RetrieveService retrieveService) {
        this.retrieveService = retrieveService;
    }



    public List<Integer> hasCooling(boolean state) {
        List<Drone> drones = retrieveService.getAllDrones();
        return drones.stream()
                .filter(d -> d.getCapability() != null && d.getCapability().getCooling() != null)
                .filter(d -> d.getCapability().getCooling() == state)
                .map(Drone::getId)
                .filter(Objects::nonNull)
                .collect(toList());
    }

    public Drone getDetails(Integer id) {
        return retrieveService.getAllDrones().stream()
                .filter(d -> id.equals(d.getId()))
                .findFirst()
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "Drone not found: " + id));
    }

    //create whitelist for validating against attribute name
    private static final Set<String> ALLOWED_ATTRIBUTES = Set.of("cooling", "heating", "capacity", "maxMoves", "costPerMove", "costInitial", "costFinal");


    public List<Integer> getQuery(String attributeName, String attributeValue) {
        if (!ALLOWED_ATTRIBUTES.contains(attributeName)) //Validating name is withing the allowed whitelist attributes
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Attribute not allowed"); //throwing the error if invalid
        List<Drone> drones = retrieveService.getAllDrones();

        //use to stream to filter , and convert values to corresponding attribute types
        return drones.stream()
                .filter(drone -> {
                            switch (attributeName) {
                                case "cooling" -> {
                                    return drone.getCapability().getCooling() == Boolean.parseBoolean(attributeValue);
                                }
                                case "heating" -> {
                                    return drone.getCapability().getHeating() == Boolean.parseBoolean(attributeValue);
                                }
                                case "capacity" -> {
                                    return drone.getCapability().getCapacity() == Double.parseDouble(attributeValue);
                                }
                                case "maxMoves" -> {
                                    return drone.getCapability().getMaxMoves() == Integer.parseInt(attributeValue);
                                }
                                case "costPerMove" -> {
                                    return drone.getCapability().getCostPerMove() == Double.parseDouble(attributeValue);
                                }
                                case "costInitial" -> {
                                    return drone.getCapability().getCostInitial() == Double.parseDouble(attributeValue);
                                }
                                case "costFinal" -> {
                                    return drone.getCapability().getCostFinal() == Double.parseDouble(attributeValue);
                                }

                                default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Value not allowed");

                            }
                            }
                        )
                //map results to corresponding drone IDs
                .map(Drone::getId)
                //collect them into a list
                .toList();


    }




    /* @GetMapping(queryAsPath/{queryAttribute}/{queryValue})
    public List<Integer> queryAsPath(@PathVariable String attributeName, @PathVariable String attributeValue){
    return droneService.getQuery(attributeName, attributeValue);





    public List<Integer> getQuery(String queryAttribute, String queryValue) {
        List<Drone> drones = retrieveService.getAllDrones()
        return drones.stream()
            .filter(d

     */






}
