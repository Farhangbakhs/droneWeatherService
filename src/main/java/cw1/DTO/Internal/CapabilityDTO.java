package cw1.DTO.Internal;


import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
@Data
@NoArgsConstructor
@AllArgsConstructor



public class CapabilityDTO {
    private Boolean cooling;
    private Boolean heating;
    private Double capacity;
    private Integer maxMoves;
    private Double costPerMove;
    private Double costInitial;
    private Double costFinal;
}
