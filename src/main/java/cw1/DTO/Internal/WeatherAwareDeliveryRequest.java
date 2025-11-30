package cw1.DTO.Internal;

import cw1.DTO.LngLat;
import cw1.DTO.MedDispatchRec;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;

import java.util.List;

/**
 * Request DTO for weather-aware delivery path calculation
 * Supports both normal mode (auto service point selection) and testing mode (explicit coordinates)
 *
 * Normal Mode: System queries ILP endpoint for all service points and selects nearest
 * Testing Mode: Uses explicitly provided service point coordinates, bypassing ILP endpoint
 */
public class WeatherAwareDeliveryRequest {

    @Valid
    private List<MedDispatchRec> dispatches;

    /**
     * TESTING MODE ONLY: Explicitly specified service point coordinates
     * When provided along with skipServicePointSelection=true, the system will use
     * these coordinates directly instead of querying the ILP service points endpoint.
     * This dramatically speeds up calculations for international testing scenarios.
     */
    @JsonProperty("explicitServicePoint")
    private LngLat explicitServicePoint;

    /**
     * TESTING MODE FLAG: When true, bypasses service point selection logic
     * Must be used together with explicitServicePoint for testing mode to activate.
     * Default: false (normal mode with automatic service point selection)
     */
    @JsonProperty("skipServicePointSelection")
    private Boolean skipServicePointSelection;

    // Default constructor
    public WeatherAwareDeliveryRequest() {
        this.skipServicePointSelection = false;
    }

    // Constructor for normal mode (backward compatibility)
    public WeatherAwareDeliveryRequest(List<MedDispatchRec> dispatches) {
        this.dispatches = dispatches;
        this.skipServicePointSelection = false;
    }

    // Full constructor for testing mode
    public WeatherAwareDeliveryRequest(List<MedDispatchRec> dispatches,
                                      LngLat explicitServicePoint,
                                      Boolean skipServicePointSelection) {
        this.dispatches = dispatches;
        this.explicitServicePoint = explicitServicePoint;
        this.skipServicePointSelection = skipServicePointSelection;
    }

    // Getters and setters
    public List<MedDispatchRec> getDispatches() {
        return dispatches;
    }

    public void setDispatches(List<MedDispatchRec> dispatches) {
        this.dispatches = dispatches;
    }

    public LngLat getExplicitServicePoint() {
        return explicitServicePoint;
    }

    public void setExplicitServicePoint(LngLat explicitServicePoint) {
        this.explicitServicePoint = explicitServicePoint;
    }

    public Boolean getSkipServicePointSelection() {
        return skipServicePointSelection != null ? skipServicePointSelection : false;
    }

    public void setSkipServicePointSelection(Boolean skipServicePointSelection) {
        this.skipServicePointSelection = skipServicePointSelection;
    }

    /**
     * Helper method to determine if request is in testing mode
     * Testing mode is active when:
     * 1. skipServicePointSelection is explicitly set to true
     * 2. explicitServicePoint coordinates are provided
     *
     * @return true if in testing mode, false for normal mode
     */
    public boolean isTestingMode() {
        return Boolean.TRUE.equals(skipServicePointSelection)
            && explicitServicePoint != null;
    }

    /**
     * Validates the request based on mode
     *
     * @throws IllegalArgumentException if request is invalid
     */
    public void validate() {
        if (dispatches == null || dispatches.isEmpty()) {
            throw new IllegalArgumentException("Dispatches list cannot be empty");
        }

        if (Boolean.TRUE.equals(skipServicePointSelection) && explicitServicePoint == null) {
            throw new IllegalArgumentException(
                "Testing mode requires explicitServicePoint to be provided");
        }

        // Validate first dispatch has delivery location
        // Note: We can't check getDelivery() here if dispatches aren't properly deserialized yet
        // The controller will handle this validation after proper deserialization
    }
}
