
package cw1.service;

import cw1.DTO.DispatchRec;
import cw1.DTO.External.Drone;

import cw1.DTO.External.DronesForService;
import cw1.DTO.External.ServicePoint;
import cw1.DTO.Internal.CapabilityDTO;
import cw1.DTO.Internal.LocationDTO;
import cw1.DTO.LngLat;
import cw1.DTO.MedDispatchRec;
import cw1.DTO.QueryAttribute;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

@Service
public class DroneService {

    private final RetrieveService retrieveService;
    private final CalculationService calculationService;

    public DroneService(RetrieveService retrieveService, CalculationService calculationService) {
        this.retrieveService = retrieveService;
        this.calculationService = calculationService;
    }



    public List<String> hasCooling(boolean state) {
        List<Drone> drones = retrieveService.getAllDrones();
        return drones.stream()
                .filter(d -> d.getCapability() != null && d.getCapability().getCooling() != null)
                .filter(d -> d.getCapability().getCooling() == state)
                .map(Drone::getId)
                .filter(Objects::nonNull)
                .collect(toList());
    }

    public Drone getDetails(String id) {
        return retrieveService.getAllDrones().stream()
                .filter(d -> id.equals(d.getId()))
                .findFirst()
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "Drone not found: " + id));
    }

    //create whitelist for validating against attribute name
    private static final Set<String> ALLOWED_ATTRIBUTES = Set.of("cooling", "heating", "capacity", "maxMoves", "costPerMove", "costInitial", "costFinal");


    public List<String> getQuery(String attributeName, String attributeValue) {
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
                                    return drone.getCapability().getCapacity() == Integer.parseInt(attributeValue);
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

    //Method to return list of drone ids that can satisy medDispatchRec
    public List<String> queryAvailableDrones(List<MedDispatchRec> medDispatchRecs){
        //Handles special case if medDispatch is empty
        if (medDispatchRecs == null || medDispatchRecs.isEmpty()) {
            return retrieveService.getAllDrones().stream()
                    .map(Drone::getId)
                    .filter(Objects::nonNull)
                    .collect((toList()));
        }

        //fetching data from external rest service on every call
        List<Drone> drones  = retrieveService.getAllDrones();
        List<DronesForService> dronesForServices = retrieveService.getAllDronesForService();
        List<ServicePoint> servicePoints = retrieveService.getAllServicePoints();


        //filters drones and keeps only those that can handle all dispatches
        return drones.stream()
                .filter(drone -> singleDroneFilter(drone, medDispatchRecs, dronesForServices, servicePoints))
                .map(Drone::getId)
                .filter(Objects::nonNull)
                .collect(toList());
    }


    //Checks whether one specific drone can handle all the necessary requirements uses && logic
    private boolean singleDroneFilter(Drone drone,List<MedDispatchRec> medDispatchRecs , List<DronesForService> dronesForServices, List<ServicePoint> servicePoints ) {

        //sums capacity of all dispatch requirementts and checks if drone can handle the entire capacity
        int checkTotalCapacity = medDispatchRecs.stream()
                .map(MedDispatchRec::getRequirements)
                .map(DispatchRec::getCapacity)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();
        if(drone.getCapability().getCapacity() < checkTotalCapacity) {
            return false;
        }

        //for every dispatch if cooling is required , drone must have cooling
        boolean coolingOk = medDispatchRecs.stream()
                .allMatch(medDispatchRec -> {
                    DispatchRec rec = medDispatchRec.getRequirements();
                    Boolean coolingRequired = rec.getCooling();
                    //null or false then no requirement, if true then drone must have cooling
                    return coolingRequired == null || !coolingRequired || Boolean.TRUE.equals(drone.getCapability().getCooling());
                });

        if(!coolingOk) {
            return false; //fails cooling requirement for atleast one dispatch
        }

        //for every dispatch if heating is required , drone must have heating
        boolean heatingOK = medDispatchRecs.stream()
                .allMatch((medDispatchRec -> {
                    DispatchRec rec = medDispatchRec.getRequirements();
                    Boolean heatingRequired = rec.getHeating();
                    //null or false then no requirement, if true then drone must have heating
                    return heatingRequired == null || !heatingRequired || Boolean.TRUE.equals(drone.getCapability().getHeating());
                }));

        if(!heatingOK) {
            return false; // fails heating requirement for atleast one dispatch
        }

        //drone must be available for every dispatchs (date,time) at some service point
        boolean availibityOk = medDispatchRecs.stream()
                .allMatch(medDispatchRec ->
                    isDroneAvailableForDispatch(drone, medDispatchRec, dronesForServices)
                );

        if (!availibityOk) {
            return false;
        }

        if (!maxCostOk(drone, medDispatchRecs, dronesForServices, servicePoints)) {
            return false;
        }

    return true;
    }


    private boolean maxCostOk(Drone drone, List<MedDispatchRec> medDispatchRecs, List<DronesForService> dronesForServices, List<ServicePoint> servicePoints) {
        //checks if there is any maxCost at all
        boolean hasAnyMaxCost = medDispatchRecs.stream()
                .map(MedDispatchRec::getRequirements)
                .map(DispatchRec::getMaxCost)
                .anyMatch(Objects::nonNull);

        if(!hasAnyMaxCost){
            return true;
        }

        //set n to be the size of the dispatch list, number of dispatches needede
        int n = medDispatchRecs.size();
        CapabilityDTO capability = drone.getCapability();

        //check each dispatch that has a maxCost
        for (MedDispatchRec dispatchRec :medDispatchRecs){
            DispatchRec rec = dispatchRec.getRequirements();
            Double maxCost = dispatchRec.getRequirements().getMaxCost();

            //if maxCost is null this dispatch doesnt constrain cost
            if(maxCost == null) {
                continue;
            }

            //estimate how many moves are needed for this dispatch
            long estimatedMoves = estimatedMovesForDelivery(drone, dispatchRec, dronesForServices, servicePoints);
            //variable since depends on the moves
            double variableCost = estimatedMoves* capability.getCostPerMove();
            //"pack" cost for this drone and this dispatch
            double packCost = capability.getCostInitial() + capability.getCostFinal() + variableCost;
            //share per dispatch
            double costShareForDispatch = packCost/n;
            //enforce constraint
            if (costShareForDispatch > maxCost) {
                return false;
            }
        }
        //if none of maxCost constraints were violated then true
        return true;
    }

    //rough estimation of number of moves from suitable service point to delivery location
    private long estimatedMovesForDelivery(Drone drone, MedDispatchRec dispatchRec, List<DronesForService> dronesForServices, List<ServicePoint> servicePoints) {
        //assume 10000ish moves, to reject
        final long DEFAULT_MOVES = 1000L;

        //checks if no delivery location
        LngLat delivery = dispatchRec.getDelivery();
        if (delivery == null) {
            return DEFAULT_MOVES;
        }

        //finds all service point ids where this drone is registered/active
        List<Integer> servicePointIdsForDrone = dronesForServices.stream()
                .filter(sp -> sp.getDrones().stream()
                        .anyMatch(droneEntry -> droneEntry.getId().equals(drone.getId())))
                .map(DronesForService::getServicePointId)
                .collect(Collectors.toList());

        //checks if drone is assigned to any service points
        if (servicePointIdsForDrone.isEmpty()) {
            return DEFAULT_MOVES;
        }

        double minDistance = Double.POSITIVE_INFINITY;

        //checks all service points, find the closest to delivery loctation
        for(Integer spId: servicePointIdsForDrone) {
            ServicePoint sp = servicePoints.stream()
                    .filter(s->s.getId().equals(spId))
                    .findFirst()
                    .orElse(null);

            if(sp == null || sp.getLocation()==null){
                continue;
            }

            LocationDTO loc = sp.getLocation();
            LngLat spLocation = new LngLat(loc.getLng(), loc.getLat());

            double distance = calculationService.calculateDistance(spLocation, delivery);
            if(distance < minDistance) {
                minDistance = distance;
            }
        }


        //if we never found a valid distance , use defualt
        if(!Double.isFinite(minDistance)) {
            return DEFAULT_MOVES;
        }

        //convert distance to moves using global step size
        long moves = (long) Math.ceil(minDistance/CalculationService.STEP);
        //guard against zero moves
        return moves > 0 ? moves : 1L;
    }


    private boolean isDroneAvailableForDispatch(Drone drone, MedDispatchRec dispatchRec, List<DronesForService> dronesForServices){
        DayOfWeek dayOfWeek = dispatchRec.getDate().getDayOfWeek();

        return dronesForServices.stream()
                //Look at all drone entries across service points
                .flatMap(servicepoint -> servicepoint.getDrones().stream())
                //onlt this specific drone
                .filter(droneEntry -> droneEntry.getId().equals(drone.getId()))
                //flatten all availbility slots for this drone
                .flatMap(droneEntry -> droneEntry.getAvailability().stream())
                //keep only slots that match the dispatchs day of week
                .filter(availability -> availability.getDayOfWeek() == dayOfWeek)
                //is dispatch time inside any of the matching avaibility windows?
                .anyMatch(availability -> {
                    LocalTime from = availability.getFrom();
                    LocalTime until = availability.getUntil();
                    LocalTime dispatchTime = dispatchRec.getTime();
                    return !dispatchTime.isBefore(from) && !dispatchTime.isAfter(until);

                });
    }





    public List<String> query(List<QueryAttribute> queryAttributes) {
        //if no filters, return list of all drone ids
        if (queryAttributes == null || queryAttributes.isEmpty()){
            return retrieveService.getAllDrones().stream()
                    .map(Drone::getId)
                    .filter(Objects::nonNull)
                    .collect(toList());
        }
        List<Drone> drones = retrieveService.getAllDrones();

        //Filter drones therefore all conditions must match

        return drones.stream()
                .filter(drone -> matchesAllQueries(drone, queryAttributes))
                .map(Drone::getId)
                .filter(Objects::nonNull)
                .collect(toList());
    }

    private boolean matchesAllQueries(Drone drone, List<QueryAttribute> queries) {
        //ALL queries must match && {"outside" method that checks if all the matchesQuery results are ALL true
        for (QueryAttribute query : queries) {
            if(!matchesQuery(drone, query)) {
                return false;
            }
        }
        return true;
    }

    private boolean matchesQuery (Drone drone, QueryAttribute query) {
        //extracting attribute(which field to check)
        //extracting operator(how to compare)
        //extracting value(what to compare to)

        String attribute = query.getAttribute();
        String operator = query.getOperator();
        String value = query.getValue();

        try{
            return switch (attribute) {
                //Individual query checks
                //Using the compareType methods for corresponding  individual attributes "cooling" ">" "8" returns true if true
                case "name" -> compareStrings(drone.getName(), operator, value);
                case "id" -> compareStrings(drone.getId(), operator, value);
                case "cooling" -> compareBooleans(drone.getCapability().getCooling(), operator, value);
                case "heating" -> compareBooleans(drone.getCapability().getHeating(), operator, value);
                case "capacity" -> compareIntegers(drone.getCapability().getCapacity(), operator, value);
                case "maxMoves" -> compareIntegers(drone.getCapability().getMaxMoves(), operator, value);
                case "costPerMove" -> compareDoubles(drone.getCapability().getCostPerMove(), operator, value);
                case "costInitial" -> compareDoubles(drone.getCapability().getCostInitial(), operator, value);
                case "costFinal" -> compareDoubles(drone.getCapability().getCostFinal(), operator, value);
                default -> false;
            };

        } catch (Exception e) {
            return false; // parse error no match
        }
    }


    //methods for comparisons of each type
    //actual is type specific so we need 4 methods for each type
    //expected needs to be parsed

    private boolean compareStrings(String actual, String operator, String expected){
        if (actual==null) return false;

        return switch (operator) {
            case "=" -> actual.equals(expected);
            case "!=" -> !actual.equals(expected);
            default -> false; //Strings only support equals and not equals

        };
    }

    private boolean compareBooleans(Boolean actual, String operator, String expected) {
        if(actual==null) return false;

        boolean expectedBool = Boolean.parseBoolean(expected);

        return switch (operator) {
            case "=" -> actual == expectedBool;
            case "!=" -> actual != expectedBool;
            default -> false; //Boolean only supports equals and not equals
        };
    }

    private boolean compareIntegers(Integer actual, String operator, String expected) {
        if(actual==null) return false;

        int expectedInt = Integer.parseInt(expected);

        return switch (operator) {
            case "=" -> actual.equals(expectedInt);
            case "!=" -> !actual.equals(expectedInt);
            case "<" -> actual < expectedInt;
            case ">" -> actual > expectedInt;
            default -> false;
        };
    }

    private boolean compareDoubles(Double actual, String operator, String expected){
        if(actual==null) return false;

        double expectedDouble = Double.parseDouble(expected);

        return switch (operator) {
            case "=" -> actual.equals(expectedDouble);
            case "!=" -> !actual.equals(expectedDouble);
            case "<" -> actual < expectedDouble;
            case ">" -> actual > expectedDouble;
            default -> false;
        };
    }
}
