package cw1.service;

import cw1.DTO.External.Drone;
import cw1.DTO.External.DronesForService;
import cw1.DTO.External.RestrictedArea;
import cw1.DTO.External.ServicePoint;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Arrays;
import java.util.List;

@Service
public class RetrieveService {

    private final RestTemplate restTemplate;
    private final String baseUrl; // inject as String

    public RetrieveService(RestTemplate restTemplate,
                           @Value("${ilp.endpoint}") String baseUrl) { //reads the property from the configuration
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    public List<Drone> getAllDrones() { //method to fetch all drones from external api
        String url = UriComponentsBuilder
                .fromHttpUrl(baseUrl)
                .pathSegment("drones")
                .toUriString(); //Starts from base url and appends the path /drones converts to a cleam dtring

        Drone[] drones = restTemplate.getForObject(url, Drone[].class); //makes an http get request to that url. deserielizes the JSON array response into a java array of Drone objects
        return drones == null ? List.of() : Arrays.asList(drones); // if list body is null return empty list, otherwise convert the array into List<Drone>
    }

    public List<DronesForService> getAllDronesForService() {
        String url = UriComponentsBuilder
                .fromHttpUrl(baseUrl)
                .pathSegment("drones-for-service-points")
                .toUriString();

        DronesForService[] dronesForService = restTemplate.getForObject(url, DronesForService[].class);
        return dronesForService == null ? List.of() : Arrays.asList(dronesForService);
    }

    public List<ServicePoint> getAllServicePoints() {
        String url = UriComponentsBuilder
                .fromHttpUrl(baseUrl)
                .pathSegment("service-points")
                .toUriString();

        ServicePoint[] servicePoints = restTemplate.getForObject(url, ServicePoint[].class);
        return servicePoints == null ? List.of() : Arrays.asList(servicePoints);
    }

    public List<RestrictedArea> getAllRestrictedAreas() {
        String url = UriComponentsBuilder
                .fromHttpUrl(baseUrl)
                .pathSegment("restricted-areas")
                .toUriString();

        RestrictedArea[] restrictedAreas = restTemplate.getForObject(url, RestrictedArea[].class);
        return restrictedAreas == null ? List.of() : Arrays.asList(restrictedAreas);
    }




}

