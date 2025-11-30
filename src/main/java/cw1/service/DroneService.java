package cw1.service;

import cw1.DTO.DispatchRec;
import cw1.DTO.External.Drone;

import cw1.DTO.External.DronesForService;
import cw1.DTO.External.ServicePoint;
import cw1.DTO.Internal.*;
        import cw1.DTO.LngLat;
import cw1.DTO.MedDispatchRec;
import cw1.DTO.QueryAttribute;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.math.*;
        import cw1.DTO.Internal.GeoJsonLineString;
import cw1.DTO.Internal.DronePathDTO;
import cw1.DTO.Internal.DeliveryPathDTO;
import java.util.PriorityQueue;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.*;

        import static java.util.stream.Collectors.toList;

@Service
public class DroneService {

    private final RetrieveService retrieveService;
    private final CalculationService calculationService;

    public DroneService(RetrieveService retrieveService, CalculationService calculationService) {
        this.retrieveService = retrieveService;
        this.calculationService = calculationService;
    }

    private static final double[] ALLOWED_ANGLES = {
            0, 22.5, 45, 67.5, 90, 112.5, 135, 157.5,
            180, 202.5, 225, 247.5, 270, 292.5, 315, 337.5
    };

    private record DroneAssignment(Drone drone, ServicePoint servicePoint) {}

    //Each node represents
        //  one grid point(LngLat)
        //How far we have traveled
        //f = g + h , h = straight line distance to target
        //Parent [Where we came from, so we can reconstruct the path backwards later]

    private static class Node {
        final LngLat point;
        double g;      // cost from start
        double f;      // g + heuristic
        Node parent;

        Node(LngLat point, double g, double f, Node parent) {
            this.point = point;
            this.g = g;
            this.f = f;
            this.parent = parent;
        }
    }


    // 1) /dronesWithCooling → hasCooling


    public List<String> hasCooling(boolean state) {
        List<Drone> drones = retrieveService.getAllDrones();
        return drones.stream()
                .filter(d -> d.getCapability() != null && d.getCapability().getCooling() != null)
                .filter(d -> d.getCapability().getCooling() == state)
                .map(Drone::getId)
                .filter(Objects::nonNull)
                .collect(toList());
    }


    // 2) /droneDetails/{id} → getDetails


    public Drone getDetails(String id) {
        return retrieveService.getAllDrones().stream()
                .filter(d -> id.equals(d.getId()))
                .findFirst()
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "Drone not found: " + id));
    }


    // 3) /queryAsPath/{attributeName}/{attributeValue} → getQuery


    //create whitelist for validating against attribute name
    private static final Set<String> ALLOWED_ATTRIBUTES = Set.of("name", "id","cooling", "heating", "capacity", "maxMoves", "costPerMove", "costInitial", "costFinal");

    public List<String> getQuery(String attributeName, String attributeValue) {
        if (!ALLOWED_ATTRIBUTES.contains(attributeName)) //Validating name is withing the allowed whitelist attributes
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Attribute not allowed"); //throwing the error if invalid
        List<Drone> drones = retrieveService.getAllDrones();

        //use to stream to filter , and convert values to corresponding attribute types
        return drones.stream()
                .filter(drone -> {
                            switch (attributeName) {
                                case "name" -> {
                                    return Objects.equals(drone.getName(), attributeValue);
                                }
                                case "id" -> {
                                    return Objects.equals(drone.getId(), attributeValue);
                                }
                                case "cooling" -> {
                                    return drone.getCapability().getCooling() == Boolean.parseBoolean(attributeValue);
                                }
                                case "heating" -> {
                                    return drone.getCapability().getHeating() == Boolean.parseBoolean(attributeValue);
                                }
                                case "capacity" -> {
                                    return Math.abs(drone.getCapability().getCapacity() - Double.parseDouble(attributeValue)) <0.0001;
                                }
                                case "maxMoves" -> {
                                    return drone.getCapability().getMaxMoves() == Integer.parseInt(attributeValue);
                                }
                                case "costPerMove" -> {
                                    return Math.abs(drone.getCapability().getCostPerMove() - Double.parseDouble(attributeValue)) < 0.0001;
                                }
                                case "costInitial" -> {
                                    return Math.abs(drone.getCapability().getCostInitial() - Double.parseDouble(attributeValue)) < 0.0001;
                                }
                                case "costFinal" -> {
                                    return Math.abs(drone.getCapability().getCostFinal() - Double.parseDouble(attributeValue)) <0.0001;
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


    // 4) /query → query


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
                case "capacity" -> compareDoubles(drone.getCapability().getCapacity(), operator, value);
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
            case "=" -> Math.abs(actual - expectedDouble) < 0.0001;
            case "!=" -> Math.abs(actual - expectedDouble) >= 0.0001;
            case "<" -> actual < expectedDouble;
            case ">" -> actual > expectedDouble;
            default -> false;
        };
    }


    // 5) /queryAvailableDrones → queryAvailableDrones


    //Method to return list of drone ids that can satisfy medDispatchRec
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
    private boolean singleDroneFilter(Drone drone,
                                      List<MedDispatchRec> medDispatchRecs,
                                      List<DronesForService> dronesForServices,
                                      List<ServicePoint> servicePoints) {

        // 1) Same drone must be able to handle EACH dispatch individually
        boolean allDispatchesFeasible = medDispatchRecs.stream()
                .allMatch(rec -> canSingleDroneHandleDispatch(drone, rec, dronesForServices));

        if (!allDispatchesFeasible) {
            return false;
        }

        // 2) Check maxCost constraints across the whole “pack”
        return maxCostOk(drone, medDispatchRecs, dronesForServices, servicePoints);
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

        //set n to be the size of the dispatch list, number of dispatches needed
        int n = medDispatchRecs.size();
        CapabilityDTO capability = drone.getCapability();

        // 2) First pass: estimate totalMoves across ALL dispatches
        long totalMoves = 0L;
        for (MedDispatchRec dispatchRec : medDispatchRecs) {
            long moves = estimatedMovesForDelivery(
                    drone,
                    dispatchRec,
                    dronesForServices,
                    servicePoints
            );
            totalMoves += moves;
        }

        // 3) Total cost for the whole “flight pack”
        double totalCost = capability.getCostInitial()
                + capability.getCostFinal()
                + (totalMoves * capability.getCostPerMove());

        // 4) Equal pro-rata share per dispatch (as per spec)
        double costSharePerDispatch = totalCost / n;

        // 5) Second pass: enforce each maxCost constraint
        for (MedDispatchRec dispatchRec : medDispatchRecs) {
            Double maxCost = dispatchRec.getRequirements().getMaxCost();
            if (maxCost == null) {
                continue;   // this dispatch doesn’t constrain cost
            }

            if (costSharePerDispatch > maxCost) {
                return false;   // this drone is too expensive for that dispatch
            }
        }

        // All maxCost constraints satisfied
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
                .toList();

        //checks if drone is assigned to any service points
        if (servicePointIdsForDrone.isEmpty()) {
            return DEFAULT_MOVES;
        }

        double minDistance = Double.POSITIVE_INFINITY;

        //checks all service points, find the closest to delivery location
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

        //if we never found a valid distance , use default
        if(!Double.isFinite(minDistance)) {
            return DEFAULT_MOVES;
        }

        //convert distance to moves using global step size
        long moves = (long) Math.ceil(minDistance/CalculationService.STEP);
        //guard against zero moves
        return moves > 0 ? moves : 1L;
    }

    private boolean isDroneAvailableForDispatch(Drone drone, MedDispatchRec dispatchRec, List<DronesForService> dronesForServices){
        // If date/time are not provided, skip availability check (weather-aware queries)
        if (dispatchRec.getDate() == null || dispatchRec.getTime() == null) {
            return true;
        }

        DayOfWeek dayOfWeek = dispatchRec.getDate().getDayOfWeek();

        return dronesForServices.stream()
                //Look at all drone entries across service points
                .flatMap(forService -> forService.getDrones().stream())
                //only this specific drone
                .filter(droneEntry -> droneEntry.getId().equals(drone.getId()))
                //flatten all availability slots for this drone
                .flatMap(droneEntry -> droneEntry.getAvailability().stream())
                //keep only slots that match the dispatches day of week
                .filter(availability -> availability.getDayOfWeek() == dayOfWeek)
                //is dispatch time inside any of the matching availability windows?
                .anyMatch(availability -> {
                    LocalTime from = availability.getFrom();
                    LocalTime until = availability.getUntil();
                    LocalTime dispatchTime = dispatchRec.getTime();
                    return !dispatchTime.isBefore(from) && !dispatchTime.isAfter(until);

                });
    }


    // 6) /calcDeliveryPath → calcDeliveryPath


    public CalcDeliveryPathResult calcDeliveryPath(List<MedDispatchRec> medDispatchRecs) {
        //Fetch data from external api


        List<Drone> allDrones = retrieveService.getAllDrones();
        List<DronesForService> dfsList = retrieveService.getAllDronesForService();
        List<ServicePoint> servicePoints = retrieveService.getAllServicePoints();

        //fetch restricted areas and convert to polygons of LngLat
        List<List<LngLat>> restrictedPolygons = retrieveService.getAllRestrictedAreas().stream()
                .map(area -> area.getVertices().stream()
                        .map(v-> new LngLat(v.getLng(), v.getLat()))
                        .toList())
                .toList();


        //deliveries you haven't made yet
        List<MedDispatchRec> remaining = new ArrayList<>(medDispatchRecs);
        //final list of flight paths (one per drone flight)
        List<DronePathDTO> dronePaths = new ArrayList<>();

        long totalMoves = 0L;
        double totalCost = 0.0;

        //GREEDY - { iteratively scan remaining deliveries, greedily pick the first that fits all necessary attributes, keep going until nothing fits}
        // while there are deliveries left
        while (!remaining.isEmpty()) {
            MedDispatchRec seed = remaining.get(0); //take first delivery as a seed(starting job) for a new flight

            DroneAssignment assignment = pickDroneForDispatch(seed, allDrones, dfsList, servicePoints, restrictedPolygons
            );

            if (assignment == null) {
                // No drone can fulfill this delivery's requirements
                throw new IllegalArgumentException(
                        "No available drone can fulfill the delivery requirements. " +
                        "Possible reasons: maxCost too low, required capabilities (cooling/heating) not available, " +
                        "capacity constraints, or availability conflicts."
                );
            }

            Drone drone = assignment.drone();
            ServicePoint servicePoint = assignment.servicePoint();

            List<DeliveryPathDTO> deliveriesForDrone = new ArrayList<>(); //all segments this drone flies in one flight path, each with its own path
            LngLat serviceLoc = toLngLat(servicePoint.getLocation()); //starting point
            LngLat currentPos = serviceLoc; //where drone currently is e.g starts at service point

            long movesForThisDrone = 0L; //number of moves used in this flight
            double capacityUsed = 0.0; //total payload capacity used in this flight
            int maxMoves = drone.getCapability().getMaxMoves(); //move constraint for this drone
            double droneCapacity = drone.getCapability().getCapacity(); //capacity constraint for this drone

            //try to pack as many deliveries as possible into one flight

            boolean addedAny;
            do {
                addedAny = false;

                for(Iterator<MedDispatchRec> it = remaining.iterator(); it.hasNext(); ) {
                    MedDispatchRec candidate = it.next();

                    //capacity constraint per flight
                    Double capNeeded = candidate.getRequirements().getCapacity();
                    double neededCap = capNeeded == null ? 0.0 : capNeeded;
                    if(capacityUsed + neededCap > droneCapacity) { //if candidates capacity is larger than the drone's then skip
                        continue;
                    }

                    //feasibility check(per dispatch per drone) e.g is this drone even able to handle dispatch with its assets.
                    //checks if
                        // this drone has enough capacity for this dispatch
                        //cooling/heating requirements
                        //availability requirements
                    if(!canSingleDroneHandleDispatch(drone, candidate, dfsList)) {
                        continue;
                    }

                    LngLat deliveryLoc = candidate.getDelivery();


                    //Path out: from current position to delivery location, avoiding the restricted areas
                    List<LngLat> pathOut = findPathAvoidingRestricted(currentPos, deliveryLoc, restrictedPolygons);
                    int movesOut = Math.max(0, pathOut.size() -1 );


                    //Path back: from delivery location back to the service point, avoiding restricted areas
                    List<LngLat> pathBack = findPathAvoidingRestricted(deliveryLoc, serviceLoc, restrictedPolygons);
                    int movesBack = Math.max(0, pathBack.size() -1);


                    //"If I add delivery now, then go back to service point will I exceed maxMoves")
                    //projected = (Moves already used + going from currentPos to delivery + hover move + going back from delivery to service point)
                    long projected = movesForThisDrone + movesOut + 1 + movesBack;
                    if (projected > maxMoves) {
                        //cant add delivery without breaking max moves

                        continue;
                    }


                    //If all constraints are OK then we can add delivery
                    //deliveryPath is built by copying pathOut
                    List<LngLat> deliveryFlightPath = new ArrayList<>(pathOut);
                    //gets last pont in the path last=[Pk], appends it to deliveryFLightPath -> this adds the hover point
                    if (!deliveryFlightPath.isEmpty()) {
                        LngLat last = deliveryFlightPath.get(deliveryFlightPath.size() - 1);
                        deliveryFlightPath.add(last);
                    }

                    //add this flight path with id to the whole dispatch flight
                    deliveriesForDrone.add(
                            new DeliveryPathDTO(candidate.getId(), deliveryFlightPath)
                    );

                    //Accumulate/Update the variables
                    movesForThisDrone += movesOut + 1;
                    capacityUsed += neededCap;
                    LngLat hoverPoint = deliveryFlightPath.get(deliveryFlightPath.size() - 1);
                    currentPos = hoverPoint;

                    //remove this dispatch from remaining
                    it.remove();
                    addedAny = true;
                    break;
                }
            } while (addedAny && !remaining.isEmpty());//Once you cant add anymore deliveries to this flight

            //compute a final return path from wherever you ended up (CurrentPos) back to service location
            List<LngLat> pathReturn = findPathAvoidingRestricted(currentPos, serviceLoc, restrictedPolygons);
            int movesBack = Math.max(0, pathReturn.size() - 1);
            movesForThisDrone += movesBack; // add those moves to movesFor This Drone

            deliveriesForDrone.add(
                    new DeliveryPathDTO(null, pathReturn)
            );  //"return leg"

            //All segments for this flight, linked to one drone
            DronePathDTO dronePath = new DronePathDTO(drone.getId(), deliveriesForDrone);
            dronePaths.add(dronePath);

            //Cost formula for that flight: initial + final + perMove*Moves used
            double costForDrone = drone.getCapability().getCostInitial() + drone.getCapability().getCostFinal() + movesForThisDrone*drone.getCapability().getCostPerMove();

            //Accumalate into global totals
            totalMoves += movesForThisDrone;
            totalCost += costForDrone;
        }


        //Returning total cost across all flights, totalMoves across all flights and, detailed paths
        return new CalcDeliveryPathResult(totalCost, totalMoves, dronePaths);
    }

    /**
     * TESTING MODE: Calculate delivery path with explicit service point coordinates
     *
     * This method bypasses the slow ILP service point endpoint query by using
     * the explicitly provided service point coordinates. This dramatically speeds
     * up path calculation for international testing scenarios (30-60s → 2-3s).
     *
     * Key Differences from calcDeliveryPath():
     * - Does NOT call retrieveService.getAllServicePoints() (slow ILP query)
     * - Creates a synthetic ServicePoint from provided coordinates
     * - Still fetches drones and restricted areas (required for pathfinding)
     * - Uses same pathfinding logic as normal mode
     *
     * @param medDispatchRecs List of delivery dispatches
     * @param explicitServicePoint Coordinates to use as the service point
     * @return CalcDeliveryPathResult with path, cost, and moves
     */
    public CalcDeliveryPathResult calcDeliveryPathWithExplicitServicePoint(
            List<MedDispatchRec> medDispatchRecs,
            LngLat explicitServicePoint) {

        System.out.println(" FAST PATH: Using explicit service point, skipping ILP query");
        System.out.println("   Service Point: " + explicitServicePoint);

        // Fetch data from external API (but skip slow service points query)
        List<Drone> allDrones = retrieveService.getAllDrones();
        List<DronesForService> dfsList = retrieveService.getAllDronesForService();

        // Create a synthetic ServicePoint from the explicit coordinates
        // This allows us to reuse existing pathfinding logic
        ServicePoint syntheticServicePoint = new ServicePoint();
        syntheticServicePoint.setId(999);  // Synthetic ID
        syntheticServicePoint.setName("Testing Mode Service Point");

        // Set location using LocationDTO format
        LocationDTO location = new LocationDTO();
        location.setLng(explicitServicePoint.getLng());
        location.setLat(explicitServicePoint.getLat());
        syntheticServicePoint.setLocation(location);

        // Create a single-element service points list
        List<ServicePoint> servicePoints = List.of(syntheticServicePoint);

        System.out.println("   Created synthetic service point at (" +
            explicitServicePoint.getLat() + ", " + explicitServicePoint.getLng() + ")");

        // CRITICAL FIX: Create synthetic drone-service associations
        // Map ALL drones to our synthetic service point so they can be selected
        // Structure: DronesForService contains servicePointId and a list of DroneAvailable
        List<DroneAvailable> availableDrones = new ArrayList<>();
        for (Drone drone : allDrones) {
            // Create a DroneAvailable with default 24/7 availability
            DroneAvailable available = new DroneAvailable();
            available.setId(drone.getId());
            // We'll skip availability check in testing mode by making drones always available
            available.setAvailability(List.of());  // Empty list = no restrictions
            availableDrones.add(available);
        }

        DronesForService syntheticDfs = new DronesForService();
        syntheticDfs.setServicePointId(999);  // Our synthetic service point ID
        syntheticDfs.setDrones(availableDrones);

        List<DronesForService> syntheticDfsList = List.of(syntheticDfs);

        System.out.println("   Created synthetic drone-service association with " +
            availableDrones.size() + " drones");

        // Fetch restricted areas (still needed for pathfinding)
        List<List<LngLat>> restrictedPolygons = retrieveService.getAllRestrictedAreas().stream()
                .map(area -> area.getVertices().stream()
                        .map(v -> new LngLat(v.getLng(), v.getLat()))
                        .toList())
                .toList();

        // Deliveries you haven't made yet
        List<MedDispatchRec> remaining = new ArrayList<>(medDispatchRecs);
        // Final list of flight paths (one per drone flight)
        List<DronePathDTO> dronePaths = new ArrayList<>();

        long totalMoves = 0L;
        double totalCost = 0.0;

        // GREEDY: Iteratively scan remaining deliveries, greedily pick the first that fits
        while (!remaining.isEmpty()) {
            MedDispatchRec seed = remaining.get(0);

            // Pick a (drone, service point) pair - will use our synthetic service point
            // Use syntheticDfsList instead of dfsList so drones can be associated with our synthetic service point
            DroneAssignment assignment = pickDroneForDispatch(seed, allDrones, syntheticDfsList, servicePoints, restrictedPolygons);

            if (assignment == null) {
                throw new IllegalArgumentException(
                    "No drone available to fulfill dispatch requirements for delivery to: " +
                    seed.getDelivery()
                );
            }

            Drone drone = assignment.drone();
            ServicePoint servicePoint = assignment.servicePoint();

            System.out.println("\n Selected drone: " + drone.getId() +
                " for delivery to (" + seed.getDelivery().getLat() + ", " + seed.getDelivery().getLng() + ")");

            // Starting point is the service point
            LngLat serviceLoc = toLngLat(servicePoint.getLocation());
            LngLat currentPos = serviceLoc;

            List<MedDispatchRec> batchDeliveries = new ArrayList<>();
            batchDeliveries.add(seed);
            remaining.remove(seed);

            List<LngLat> flightPath = new ArrayList<>();
            flightPath.add(currentPos);

            long movesForThisDrone = 0L;
            double costForThisDrone = 0.0;

            // Calculate path for each delivery in the batch
            for (MedDispatchRec delivery : batchDeliveries) {
                LngLat deliveryLocation = delivery.getDelivery();

                // Path to delivery location
                List<LngLat> pathToDelivery = findPathAvoidingRestricted(currentPos, deliveryLocation, restrictedPolygons);

                if (pathToDelivery.isEmpty()) {
                    throw new IllegalArgumentException(
                        "No valid path found from service point to delivery location: " + deliveryLocation
                    );
                }

                flightPath.addAll(pathToDelivery);
                movesForThisDrone += pathToDelivery.size() - 1;  // Don't count the starting position

                // Hover move
                flightPath.add(deliveryLocation);
                movesForThisDrone++;

                currentPos = deliveryLocation;
            }

            // Path back to service point
            List<LngLat> pathBack = findPathAvoidingRestricted(currentPos, serviceLoc, restrictedPolygons);

            if (pathBack.isEmpty()) {
                throw new IllegalArgumentException(
                    "No valid path found from delivery location back to service point"
                );
            }

            flightPath.addAll(pathBack);
            movesForThisDrone += Math.max(0, pathBack.size() - 1);  // Don't count the starting position

            // Check max moves constraint
            if (movesForThisDrone > drone.getCapability().getMaxMoves()) {
                throw new IllegalArgumentException(
                    "Drone " + drone.getId() + " exceeds max moves: " +
                    movesForThisDrone + " > " + drone.getCapability().getMaxMoves()
                );
            }

            // Create delivery path DTO
            List<DeliveryPathDTO> deliveryPaths = batchDeliveries.stream()
                .map(d -> new DeliveryPathDTO(d.getId(), flightPath))
                .toList();

            DronePathDTO dronePath = new DronePathDTO(drone.getId(), deliveryPaths);
            dronePaths.add(dronePath);

            // Calculate cost
            costForThisDrone = drone.getCapability().getCostInitial() +
                drone.getCapability().getCostFinal() +
                movesForThisDrone * drone.getCapability().getCostPerMove();

            totalMoves += movesForThisDrone;
            totalCost += costForThisDrone;

            System.out.println("   Moves: " + movesForThisDrone + ", Cost: " + costForThisDrone);
        }

        System.out.println("\n FAST PATH COMPLETE");
        System.out.println("   Total Moves: " + totalMoves);
        System.out.println("   Total Cost: " + totalCost);

        return new CalcDeliveryPathResult(totalCost, totalMoves, dronePaths);
    }

    //pick a pair of (drone, service point) that can handle this one dispatch as a whole flight(out,hover, back) without breaking constraints
    private  DroneAssignment pickDroneForDispatch(MedDispatchRec dispatchRec, List<Drone> drones, List<DronesForService> dfsList, List<ServicePoint> servicePoints, List<List<LngLat>> restrictedPolygons) {

        // Get delivery location for distance calculations
        LngLat deliveryLoc = dispatchRec.getDelivery();

        System.out.println("");
        System.out.println(" DRONE SELECTION - Delivery Location: (" + deliveryLoc.getLng() + ", " + deliveryLoc.getLat() + ")");
        System.out.println("");

        // Sort dfsList by distance from service point to delivery location (closest first)
        // This ensures we prioritize the nearest service point
        List<DronesForService> sortedDfsList = dfsList.stream()
                .sorted((dfs1, dfs2) -> {
                    ServicePoint sp1 = servicePoints.stream()
                            .filter(s -> s.getId().equals(dfs1.getServicePointId()))
                            .findFirst()
                            .orElse(null);
                    ServicePoint sp2 = servicePoints.stream()
                            .filter(s -> s.getId().equals(dfs2.getServicePointId()))
                            .findFirst()
                            .orElse(null);

                    if (sp1 == null) return 1;  // sp1 null -> sort it to end
                    if (sp2 == null) return -1; // sp2 null -> sort it to end

                    double dist1 = calculationService.calculateDistance(toLngLat(sp1.getLocation()), deliveryLoc);
                    double dist2 = calculationService.calculateDistance(toLngLat(sp2.getLocation()), deliveryLoc);

                    return Double.compare(dist1, dist2);
                })
                .toList();

        // Log sorted service points with distances
        System.out.println("\n Service Points sorted by distance:");
        for (DronesForService dfs : sortedDfsList) {
            ServicePoint sp = servicePoints.stream()
                    .filter(s -> s.getId().equals(dfs.getServicePointId()))
                    .findFirst()
                    .orElse(null);
            if (sp != null) {
                double distance = calculationService.calculateDistance(toLngLat(sp.getLocation()), deliveryLoc);
                System.out.printf("   • SP-%d (%s) at (%.4f, %.4f) - Distance: %.4f km | Drones: %d%n",
                        sp.getId(), sp.getName(),
                        sp.getLocation().getLng(), sp.getLocation().getLat(),
                        distance, dfs.getDrones().size());
            }
        }

        //extracting service point Id from drones for service list, finding corresponding service Point
        for (DronesForService dfs : sortedDfsList) {
            Integer spId = dfs.getServicePointId();
            ServicePoint sp = servicePoints.stream()
                    .filter(s->s.getId().equals(spId))
                    .findFirst()
                    .orElse(null);
            if (sp==null) continue;

            System.out.println("\n Checking Service Point: " + sp.getName() + " (ID: " + sp.getId() + ")");

            //Iterating for drones at corresponding service point
            for(cw1.DTO.Internal.DroneAvailable da: dfs.getDrones()) {
                Drone drone = drones.stream()
                        //find matching drone in the full drones list by ID
                        .filter(d->d.getId().equals(da.getId()))
                        .findFirst()
                        .orElse(null);
                if (drone == null) continue;

                System.out.println("     Testing Drone: " + drone.getName() + " (ID: " + drone.getId() + ")");

                //per-dispatch feasibility
                //cheap non pathfinding check.
                if(!canSingleDroneHandleDispatch(drone,dispatchRec,dfsList)) {
                    System.out.println("       Failed: Capacity or availability check");
                    continue;
                }

                //path out and back from this service point
                //converting the service points locationDTO to LngLat form
                LngLat serviceLoc = toLngLat(sp.getLocation());
                //Delivery location
                LngLat delivery = dispatchRec.getDelivery();

                //Find the path from service Point to the Delivery Location [Uses A* and avoids restricted Areas]
                List<LngLat> out = findPathAvoidingRestricted(serviceLoc, delivery, restrictedPolygons);

                //Find the path from Delivery Location to service point [Uses A* and avoids restricted Areas]
                List<LngLat> back = findPathAvoidingRestricted(delivery,serviceLoc, restrictedPolygons);

                int moves = Math.max(0, out.size() - 1) // out
                        + 1                           // hover
                        + Math.max(0, back.size() - 1); // back

                System.out.printf("       Moves: %d (out: %d, hover: 1, back: %d) | Max allowed: %d%n",
                        moves, Math.max(0, out.size() - 1), Math.max(0, back.size() - 1),
                        drone.getCapability().getMaxMoves());

                //max moves constraint
                //explicitly enforcing maxMoves for this particular (drone, servicePoint pair)
                if(moves > drone.getCapability().getMaxMoves()) {
                    System.out.println("       Failed: Exceeds max moves");
                    continue;
                }

                System.out.println("       SUCCESS! Selected drone: " + drone.getName() + " from " + sp.getName());
                System.out.println("\n");
                return new DroneAssignment(drone,sp);
            }
        }

        System.out.println(" NO SUITABLE DRONE FOUND!");
        System.out.println("\n");
        return null;
    }

    //Ignoring path length, cost and other deliveries, can this one drone handle this one dispatch at some service point, given its capabilities and availability
    private boolean canSingleDroneHandleDispatch(Drone drone, MedDispatchRec dispatchRec, List<DronesForService> dfslist) {
        //get nested requirements from dispatchRec
        DispatchRec req = dispatchRec.getRequirements();

        // Only check capacity constraint
        Double cap = req.getCapacity();
        if ( cap!= null && drone.getCapability().getCapacity() < cap) {
            return false;
        }

        return isDroneAvailableForDispatch(drone, dispatchRec, dfslist);
    }

    private LngLat toLngLat(LocationDTO loc) {
        return new LngLat(loc.getLng(), loc.getLat());
    }

    private List<LngLat> findPathAvoidingRestricted(LngLat start,
                                                    LngLat target,
                                                    List<List<LngLat>> restrictedPolygons) {


        // If already "close enough", no path needed beyond start
        //Using the CLOSE_THRESHOLD
        //IF start is within small radius of target , then consider it "done"
        if (calculationService.isCloseTo(start, target)) {
            return List.of(start);
        }

        // PriorityQueue(min heap) of frontier nodes, always gets the node with smallest f, "places we might explore next
        PriorityQueue<Node> open = new PriorityQueue<>(Comparator.comparingDouble(n -> n.f));
        //Map from a discrete grid key (x,y) to Node object
        // if reached same grid cell with cheaper g, you can update it
        Map<String, Node> allNodes = new HashMap<>();
        //Set of grid keys already processed and finalised
        //Best path to these exists already
        Set<String> closed = new HashSet<>();

        //StartNode
            //point=start
            //g = 0.0 ( no cost from start to itself)
            //f = g + h = 0 + heuristic(start, target)
            //parent = null (Started here)

        Node startNode = new Node(start, 0.0, heuristic(start, target), null);
        //add start to open node(all things needed to explore
        open.add(startNode);
        allNodes.put(key(start), startNode);

        int safety = 500_000; // hard cap on node expansions

        //holds node where decided that target is reached
        Node goalNode = null;


        while (!open.isEmpty() && safety-- > 0) {
            //gives node with lowest f(best estimated total cost)
            Node current = open.poll();
            String currentKey = key(current.point);

            //compute grid key for current.point:key(current.point)
            //if key already in closed
                // has been superseded by a better path
                //node is obsolete
            if (closed.contains(currentKey)) {
                continue;
            }
            closed.add(currentKey);

            //Since current.point doesnt need to be exactly target , "close enough" we use the same CLOSE_THRESHOLD
            // Reached target neighbourhood
            if (calculationService.isCloseTo(current.point, target)) {
                goalNode = current;
                break;
            }

            // Explore neighbours

            //For each angle in allowed_angles 0,22.5,45...
            //use nextPosition to move one STEP in that direction
            //each neighbour is exactly one grid step away
            for (double angle : ALLOWED_ANGLES) {
                LngLat neighbourPoint = calculationService.nextPosition(current.point, angle);

                //For each potential step from current to neighbourPoint, check if this segment enters restricted area
                // Skip if this move would cross a restricted area
                if (crossesRestricted(current.point, neighbourPoint, restrictedPolygons)) {
                    continue;
                }

                //If best path finalised to grid cell dont consider again
                String neighbourKey = key(neighbourPoint);
                if (closed.contains(neighbourKey)) {
                    continue;
                }

                //if we go from current to neighbourPoint, how much will total Distance from start be
                // current g + one STEP
                double tentativeG = current.g + CalculationService.STEP;
                //Check if we already have this node
                Node neighbour = allNodes.get(neighbourKey);

                //Two situations
                    //never seen this grid cell before OR
                    //Seen but found a cheaper path now
                if (neighbour == null || tentativeG < neighbour.g) {
                    //compute straight line distance to target
                    double h = heuristic(neighbourPoint, target);
                    if (neighbour == null) {
                        //if first time visiting neighbour
                        //Create new Node
                            //point= neighbourPoint
                            //g = tentativeG
                            //f = g + h
                            //parent = current
                        neighbour = new Node(neighbourPoint, tentativeG, tentativeG + h, current);
                        //Insert into allNodes
                        allNodes.put(neighbourKey, neighbour);
                    } else {
                        //If we already had this node then update its g, f and parent to reflect better path
                        neighbour.g = tentativeG;
                        neighbour.f = tentativeG + h;
                        neighbour.parent = current;
                    }
                    open.add(neighbour);
                }
            }
        }

        // If we never reached close to the target, fall back to start-only path
        if (goalNode == null) {
            return List.of(start);
        }

        // Reconstruct path from goal back to start
            //Follow parent backwards [ Goal node -> its parent -> its parent ->........ startNode (where parent is null)]
        List<LngLat> path = new ArrayList<>();
        Node current = goalNode;
        while (current != null) {
            path.add(current.point);
            current = current.parent;
        }
        //Switch the path to start at StartNode
        Collections.reverse(path);

        return path;
    }


    //If taken one step from "from" to "to" , does step cross restricted
    private boolean crossesRestricted(LngLat from,
                                      LngLat to,
                                      List<List<LngLat>> restrictedPolygons) {

        for (List<LngLat> poly : restrictedPolygons) {
            // If either endpoint lies inside the polygon, treat as crossing
            if (calculationService.isInRegion(from, poly)
                    || calculationService.isInRegion(to, poly)) {
                return true;
            }

            // Check for segment–edge intersections
            for (int i = 0; i < poly.size() - 1; i++) {
                LngLat a = poly.get(i);
                LngLat b = poly.get(i + 1);

                if (segmentsIntersect(from, to, a, b)) {
                    return true;
                }
            }
        }
        return false;
    }

    // Robust segment–segment intersection
    private boolean segmentsIntersect(LngLat p1, LngLat p2,
                                      LngLat q1, LngLat q2) {

        double o1 = orientation(p1, p2, q1);
        double o2 = orientation(p1, p2, q2);
        double o3 = orientation(q1, q2, p1);
        double o4 = orientation(q1, q2, p2);

        // General case: strictly different orientations
        if (o1 * o2 < 0 && o3 * o4 < 0) {
            return true;
        }

        // Collinear / touching cases – use onSegment-style checks
        if (almostZero(o1) && onSegment(p1, p2, q1)) return true;
        if (almostZero(o2) && onSegment(p1, p2, q2)) return true;
        if (almostZero(o3) && onSegment(q1, q2, p1)) return true;
        if (almostZero(o4) && onSegment(q1, q2, p2)) return true;

        return false;
    }

    // Signed area / orientation: >0 left turn, <0 right turn, 0 collinear
    private double orientation(LngLat a, LngLat b, LngLat c) {
        return (b.getLng() - a.getLng()) * (c.getLat() - a.getLat())
                - (b.getLat() - a.getLat()) * (c.getLng() - a.getLng());
    }

    private boolean almostZero(double v) {
        return Math.abs(v) < 1e-12;
    }

    // Check if point p lies on segment [a,b] (with small tolerance)
    private boolean onSegment(LngLat a, LngLat b, LngLat p) {
        if (!almostZero(orientation(a, b, p))) {
            return false;
        }

        double minX = Math.min(a.getLng(), b.getLng()) - 1e-12;
        double maxX = Math.max(a.getLng(), b.getLng()) + 1e-12;
        double minY = Math.min(a.getLat(), b.getLat()) - 1e-12;
        double maxY = Math.max(a.getLat(), b.getLat()) + 1e-12;

        double x = p.getLng();
        double y = p.getLat();

        return x >= minX && x <= maxX && y >= minY && y <= maxY;
    }




    // Unique key for a grid point. nextPosition always yields discrete multiples of STEP.
    //key(p) is identity of grid cell in A*
    private String key(LngLat p) {
        //dividing by STEP converts world coordinates to grid coordinates

        //Steps from east/west from 0
        long x = Math.round(p.getLng() / CalculationService.STEP);
        //steps north/south from 0
        long y = Math.round(p.getLat() / CalculationService.STEP);

        //returning hashable key for use in MAP and SET
        return x + "," + y;
    }

    // Straight-line distance as heuristic
    private double heuristic(LngLat from, LngLat to) {
        return calculationService.calculateDistance(from, to);
    }


    // 7) /calcDeliveryPathAsGeoJson → calcDeliveryPathAsGeoJson


    public GeoJsonLineString calcDeliveryPathAsGeoJson(List<MedDispatchRec> medDispatchRecs) {

        CalcDeliveryPathResult result = calcDeliveryPath(medDispatchRecs);

        if(result.getDronePaths() == null || result.getDronePaths().isEmpty() ) {
            return new GeoJsonLineString("LineString", List.of());
        }

        DronePathDTO dronePath = result.getDronePaths().get(0);

        List<List<Double>> coordinates = new ArrayList<>();

        for (DeliveryPathDTO deliveryPath:dronePath.getDeliveries()) {
            if(deliveryPath.getFlightPath() == null) continue;
            for (LngLat point : deliveryPath.getFlightPath()) {
                coordinates.add(List.of(point.getLng(), point.getLat()));
            }
        }

        return new GeoJsonLineString("LineString", coordinates);
    }

}
