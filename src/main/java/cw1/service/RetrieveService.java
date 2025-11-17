package cw1.service;

import cw1.DTO.External.Drone;
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
                           @Value("${ilp.service.url}") String baseUrl) { //reads the property from the configuration
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    public List<Drone> getAllDrones() { //method to fetch all drones from external api
        String url = UriComponentsBuilder
                .fromHttpUrl(baseUrl)
                .pathSegment("drones")
                .toUriString(); //Starts from base url and appends the path /drones converts to a cleam dtring

        Drone[] drones = restTemplate.getForObject(url, Drone[].class); //makes a http get request to that url. deserielizes the JSON array response into a java array of Drone objects
        return drones == null ? List.of() : Arrays.asList(drones); // if list body is null return empty list, otherwise convert the array into List<Drone>
    }


}
