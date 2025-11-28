package cw1.DTO.Internal;

import cw1.DTO.LngLat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor

public class DeliveryPathDTO {
    private Integer deliveryId;
    private List<LngLat> flightPath;
}
