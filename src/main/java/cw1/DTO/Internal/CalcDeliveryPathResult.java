package cw1.DTO.Internal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * All deliveries (legs) flown by one drone in this result.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor

public class CalcDeliveryPathResult {
    private double totalCost;
    private long totalMoves;
    private List<DronePathDTO> dronePaths;
}
