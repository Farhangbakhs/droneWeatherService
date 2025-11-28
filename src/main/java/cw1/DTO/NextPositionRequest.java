package cw1.DTO;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.DecimalMax;

@JsonIgnoreProperties(ignoreUnknown = true)
public class NextPositionRequest {

    @Valid
    @NotNull
    private LngLat start;


    @DecimalMin("0.0")
    @DecimalMax("360.0")
    @NotNull
    private Double angle;

    public NextPositionRequest() {}

    public NextPositionRequest(LngLat start, Double angle) {
        this.start = start;
        this.angle = angle;
    }

    public LngLat getStart() {
        return start;
    }

    public void setStart(LngLat start) {
        this.start = start;
    }

    public Double getAngle() {
        return angle;
    }

    public void setAngle(Double angle) {
        this.angle = angle;
    }
}