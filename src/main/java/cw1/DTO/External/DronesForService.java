// cw1/DTO/External/DronesForService.java
package cw1.DTO.External;

import cw1.DTO.Internal.DroneAvailable;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;

@Data @NoArgsConstructor @AllArgsConstructor
public class DronesForService {
    private Integer servicePointId;       // matches JSON number
    private List<DroneAvailable> drones;  // matches JSON array
}
