package cw1.DTO.Internal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response for weather-aware delivery path calculation
 * Contains both delivery path and weather condition analysis
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WeatherAwareDeliveryResult {
    private CalcDeliveryPathResult deliveryPath;        // Normal delivery path result
    private FlightConditionsResponse weatherConditions; // Current weather conditions at delivery location
    private WindImpactAnalysis windImpact;              // Wind impact analysis on delivery costs (optional)
}
