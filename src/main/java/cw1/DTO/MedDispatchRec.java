package cw1.DTO;

import cw1.DTO.Internal.CapabilityDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Data @NoArgsConstructor @AllArgsConstructor

public class MedDispatchRec {
    public Integer id;
    public LocalDate date;
    public LocalTime time;
    public DispatchRec requirements;
    public LngLat delivery;
}


