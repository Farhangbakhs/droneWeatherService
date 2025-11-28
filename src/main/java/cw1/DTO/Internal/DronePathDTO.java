package cw1.DTO.Internal;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;


@Data
@NoArgsConstructor
@AllArgsConstructor

public class DronePathDTO {
    private String droneId;
    private List<DeliveryPathDTO> deliveries;
}
