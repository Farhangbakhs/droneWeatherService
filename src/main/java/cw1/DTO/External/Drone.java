package cw1.DTO.External;

import cw1.DTO.Internal.CapabilityDTO;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
        @Data
        @NoArgsConstructor
        @AllArgsConstructor


/*
[
        {
        "name": "string",
        "id": 1073741824,
        "capability": {
            "cooling": true,
            "heating": true,
            "capacity": 0.1,
            "maxMoves": 1073741824,
            "costPerMove": 0.1,
            "costInitial": 0.1,
            "costFinal": 0.1
        }
        }
        ]
*/

public class Drone {
    private String name;
    private String id;
    private CapabilityDTO capability;

}

