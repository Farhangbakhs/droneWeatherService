package cw1.DTO;


import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
@Data
@NoArgsConstructor
@AllArgsConstructor






public class DispatchRec {
    private Boolean cooling;
    private Boolean heating;
    private Integer capacity;
    private Double maxCost;
}
