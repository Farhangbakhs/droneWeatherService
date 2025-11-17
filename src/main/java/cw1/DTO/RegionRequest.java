package cw1.DTO;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RegionRequest {

    @NotNull
    @Valid
    private LngLat position;

    @NotNull
    @Valid
    private Region region;

    public LngLat getPosition() {
        return position;
    }

    public Region getRegion() {
        return region;
    }

    public void setPosition(LngLat position) {
        this.position = position;
    }

    public void setRegion(Region region) {
        this.region = region;
    }
}