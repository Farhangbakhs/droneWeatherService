package cw1.DTO.Internal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Analysis of wind impact on delivery path costs
 * Provides detailed breakdown of how wind affects flight efficiency
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WindImpactAnalysis {
    private double baseCost;                    // Original calculated cost without wind adjustment
    private double windAdjustedCost;            // Cost after wind impact analysis
    private double costAdjustmentPercent;       // Percentage change due to wind (+ means increase, - means decrease)
    private double averageWindSpeed;            // Average wind speed across path (m/s)
    private Integer averageWindDirection;       // Average wind direction (degrees)
    private List<PathSegmentWind> segmentAnalysis; // Per-segment wind analysis

    /**
     * Wind impact analysis for individual path segment
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PathSegmentWind {
        private int segmentIndex;               // Which segment in the path
        private double flightDirection;         // Direction of travel (degrees)
        private double windDirection;           // Wind direction (degrees)
        private double windSpeed;               // Wind speed (m/s)
        private String windImpactType;          // HEADWIND, TAILWIND, CROSSWIND
        private double relativeWindAngle;       // Angle between flight and wind direction
        private double costMultiplier;          // How much this segment's cost is multiplied (1.0 = no change)
    }
}
