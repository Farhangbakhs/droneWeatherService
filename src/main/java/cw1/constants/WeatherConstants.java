package cw1.constants;

/**
 * Central repository for all weather-related constants used in the drone delivery system.
 *
 * <p>This class contains three main categories of constants:
 * <ol>
 *   <li><b>Wind Impact Multipliers</b> - Used to calculate cost adjustments based on wind conditions</li>
 *   <li><b>Safety Thresholds</b> - Defines safe operating limits for drone flight</li>
 *   <li><b>Forecast Settings</b> - Configuration for forecast data processing</li>
 * </ol>
 *
 * <p>All values are based on:
 * <ul>
 *   <li>Consumer drone specifications (DJI Phantom 4, similar models)</li>
 *   <li>Aviation safety standards</li>
 *   <li>Empirical testing data</li>
 *   <li>Weather API constraints (OpenWeatherMap)</li>
 * </ul>
 *
 * @author Weather-Aware Drone Delivery System
 * @version 2.0.0
 * @since 1.0.0
 */
public final class WeatherConstants {

    // ==================== WIND IMPACT MULTIPLIERS ====================

    /**
     * Headwind penalty rate per meter/second of wind speed.
     *
     * <p><b>Rationale:</b> When flying into wind, drones must expend significantly more energy
     * to maintain forward velocity. The penalty is asymmetric (higher than tailwind benefit)
     * because fighting resistance is more energy-intensive than being pushed.
     *
     * <p><b>Formula:</b> Cost increase = windSpeed(m/s) × 0.025 = 2.5% per m/s
     *
     * <p><b>Example:</b> 8 m/s headwind = 8 × 2.5% = 20% cost increase
     *
     * @see #MAX_HEADWIND_PENALTY
     */
    public static final double HEADWIND_PENALTY_PER_MS = 0.025;

    /**
     * Maximum headwind cost penalty cap (30%).
     *
     * <p><b>Rationale:</b> Prevents unrealistic cost explosions in extreme winds.
     * Reached at 12 m/s (maximum safe wind speed), after which flight is prohibited.
     *
     * <p><b>Example:</b> Even with 20 m/s winds (unsafe), penalty is capped at 30%
     */
    public static final double MAX_HEADWIND_PENALTY = 0.30;

    /**
     * Tailwind benefit rate per meter/second of wind speed.
     *
     * <p><b>Rationale:</b> Wind from behind assists forward motion, reducing energy consumption.
     * The benefit is intentionally asymmetric (1.25% vs 2.5%) because:
     * <ul>
     *   <li>Drones still maintain active flight control (not gliding)</li>
     *   <li>Battery savings are conservative to account for return trip</li>
     *   <li>Safety margin for wind variability</li>
     * </ul>
     *
     * <p><b>Formula:</b> Cost decrease = windSpeed(m/s) × 0.0125 = 1.25% per m/s
     *
     * <p><b>Example:</b> 8 m/s tailwind = 8 × 1.25% = 10% cost decrease
     *
     * @see #MAX_TAILWIND_BENEFIT
     */
    public static final double TAILWIND_BENEFIT_PER_MS = 0.0125;

    /**
     * Maximum tailwind cost benefit cap (15%).
     *
     * <p><b>Rationale:</b> Conservative cap to avoid over-optimistic cost estimates.
     * Accounts for need to fly back (potentially against wind).
     *
     * <p><b>Example:</b> Even with 20 m/s tailwind, benefit is capped at 15%
     */
    public static final double MAX_TAILWIND_BENEFIT = 0.15;

    /**
     * Crosswind penalty rate per meter/second of wind speed.
     *
     * <p><b>Rationale:</b> Wind from the side requires constant course correction
     * and increased motor effort to maintain stability. The penalty is lower than
     * headwind because the impact is primarily on stability, not forward motion.
     *
     * <p><b>Formula:</b> Cost increase = windSpeed(m/s) × 0.01 = 1% per m/s
     *
     * <p><b>Example:</b> 8 m/s crosswind = 8 × 1% = 8% cost increase
     *
     * @see #MAX_CROSSWIND_PENALTY
     */
    public static final double CROSSWIND_PENALTY_PER_MS = 0.01;

    /**
     * Maximum crosswind cost penalty cap (12%).
     *
     * <p><b>Rationale:</b> Prevents excessive penalties while still reflecting
     * the stability challenges of strong perpendicular winds.
     *
     * <p><b>Example:</b> Even with 15 m/s crosswind, penalty is capped at 12%
     */
    public static final double MAX_CROSSWIND_PENALTY = 0.12;

    // ==================== WIND DIRECTION CLASSIFICATION ====================

    /**
     * Angle threshold (in degrees) for classifying wind as headwind.
     *
     * <p><b>Definition:</b> If relative angle between wind and flight direction
     * is 0-45°, wind is classified as headwind (flying into wind).
     *
     * <p><b>Example:</b>
     * <ul>
     *   <li>Flying north (0°) with wind from north (0°) → 0° relative → HEADWIND</li>
     *   <li>Flying north (0°) with wind from NE (45°) → 45° relative → HEADWIND</li>
     *   <li>Flying north (0°) with wind from east (90°) → 90° relative → CROSSWIND</li>
     * </ul>
     */
    public static final double HEADWIND_ANGLE_THRESHOLD = 45.0;

    /**
     * Angle threshold (in degrees) for classifying wind as tailwind.
     *
     * <p><b>Definition:</b> If relative angle between wind and flight direction
     * is 135-180°, wind is classified as tailwind (wind from behind).
     *
     * <p><b>Example:</b>
     * <ul>
     *   <li>Flying north (0°) with wind from south (180°) → 180° relative → TAILWIND</li>
     *   <li>Flying north (0°) with wind from SSW (135°) → 135° relative → TAILWIND</li>
     *   <li>Flying north (0°) with wind from west (90°) → 90° relative → CROSSWIND</li>
     * </ul>
     */
    public static final double TAILWIND_ANGLE_THRESHOLD = 135.0;

    // Note: 45° < angle < 135° is implicitly CROSSWIND (perpendicular wind)

    // ==================== FLIGHT SAFETY THRESHOLDS ====================

    /**
     * Maximum safe wind speed for drone operations (12 m/s ≈ 43 km/h ≈ 27 mph).
     *
     * <p><b>Rationale:</b> Based on consumer drone specifications:
     * <ul>
     *   <li>DJI Phantom 4: Max wind resistance 10 m/s (24 mph)</li>
     *   <li>DJI Mavic 2: Max wind resistance 10.5 m/s (24 mph)</li>
     *   <li>Safety margin: +20% for medical delivery reliability</li>
     * </ul>
     *
     * <p><b>Risk Level:</b> CRITICAL if exceeded (flight prohibited)
     */
    public static final double MAX_SAFE_WIND_SPEED = 12.0;

    /**
     * Maximum safe wind gust speed (15 m/s ≈ 54 km/h ≈ 34 mph).
     *
     * <p><b>Rationale:</b> Gusts pose sudden control challenges. Set 25% higher
     * than sustained wind limit to allow brief gusts while sustained winds are moderate.
     *
     * <p><b>Risk Level:</b> HIGH if exceeded (proceed with extreme caution)
     */
    public static final double MAX_SAFE_WIND_GUST = 15.0;

    /**
     * Minimum safe visibility for visual line-of-sight operations (5000 meters = 5 km).
     *
     * <p><b>Rationale:</b> Based on aviation visual flight rules (VFR):
     * <ul>
     *   <li>FAA Part 107: Min 3 statute miles (4.8 km) for drone operations</li>
     *   <li>Safety margin: Rounded to 5 km for obstacle avoidance</li>
     *   <li>Allows pilot to see drone and potential obstacles</li>
     * </ul>
     *
     * <p><b>Risk Level:</b> MEDIUM if below threshold (reduced visibility hazard)
     */
    public static final int MIN_SAFE_VISIBILITY = 5000;

    /**
     * Maximum safe precipitation rate (5.0 mm/h = light rain).
     *
     * <p><b>Rationale:</b> Based on electronics protection and flight stability:
     * <ul>
     *   <li>Light rain: 0-2.5 mm/h (safe with weatherproofing)</li>
     *   <li>Moderate rain: 2.5-10 mm/h (risky - reduced visibility, motor strain)</li>
     *   <li>Heavy rain: >10 mm/h (unsafe - water damage risk)</li>
     *   <li>Threshold set at 5 mm/h: Conservative limit for medical cargo</li>
     * </ul>
     *
     * <p><b>Risk Level:</b> HIGH if exceeded (water ingress, visibility issues)
     */
    public static final double MAX_SAFE_PRECIPITATION = 5.0;

    /**
     * Minimum safe temperature for drone operations (-10°C = 14°F).
     *
     * <p><b>Rationale:</b> Battery chemistry and electronics limits:
     * <ul>
     *   <li>LiPo batteries: Reduced capacity below 0°C</li>
     *   <li>Below -10°C: Significant voltage drop and flight time reduction</li>
     *   <li>Icing risk on rotors and sensors</li>
     *   <li>Motor lubricant viscosity increases</li>
     * </ul>
     *
     * <p><b>Risk Level:</b> HIGH if below threshold (battery failure risk)
     */
    public static final double MIN_SAFE_TEMP = -10.0;

    /**
     * Maximum safe temperature for drone operations (45°C = 113°F).
     *
     * <p><b>Rationale:</b> Electronics and motor overheating:
     * <ul>
     *   <li>LiPo batteries: Increased discharge rate and thermal runaway risk above 40°C</li>
     *   <li>Motor overheating: Reduced efficiency and lifespan</li>
     *   <li>Electronic component stress</li>
     *   <li>Medical cargo heat sensitivity (some medications)</li>
     * </ul>
     *
     * <p><b>Risk Level:</b> HIGH if exceeded (overheating hazard)
     */
    public static final double MAX_SAFE_TEMP = 45.0;

    // ==================== FORECAST SETTINGS ====================

    /**
     * OpenWeatherMap forecast interval in seconds (10800 seconds = 3 hours).
     *
     * <p><b>Rationale:</b> OpenWeatherMap 5-day forecast API provides data at
     * 3-hour intervals. This constant is used for:
     * <ul>
     *   <li>Window duration calculations</li>
     *   <li>Timestamp interpolation</li>
     *   <li>Forecast data validation</li>
     * </ul>
     *
     * <p><b>Usage:</b> When grouping consecutive safe forecast periods, add 3 hours
     * to the last forecast timestamp to estimate window end time.
     */
    public static final int FORECAST_INTERVAL_SECONDS = 10800;

    /**
     * Default minimum delivery window duration in minutes (30 minutes).
     *
     * <p><b>Rationale:</b> Practical minimum for delivery operations:
     * <ul>
     *   <li>Allows time for delivery completion (~10-20 min flight)</li>
     *   <li>Buffer for delays (drone prep, loading, etc.)</li>
     *   <li>Multiple delivery attempts if needed</li>
     * </ul>
     *
     * <p><b>Usage:</b> Filter out safe weather windows shorter than this duration
     * when recommending alternative delivery times.
     */
    public static final int DEFAULT_MIN_WINDOW_DURATION_MINUTES = 30;

    /**
     * Minimum wind speed threshold for impact analysis (0.5 m/s).
     *
     * <p><b>Rationale:</b> Below this threshold, wind has negligible effect:
     * <ul>
     *   <li>0.5 m/s ≈ 1.8 km/h ≈ 1.1 mph (calm air)</li>
     *   <li>Measurement noise in weather data</li>
     *   <li>Avoid unnecessary cost calculations for calm conditions</li>
     * </ul>
     *
     * <p><b>Usage:</b> If windSpeed < 0.5 m/s, return neutral wind analysis
     * with no cost adjustment.
     */
    public static final double MIN_WIND_SPEED_FOR_ANALYSIS = 0.5;

    // ==================== ANGLE NORMALIZATION ====================

    /**
     * Full circle in degrees (360°).
     *
     * <p><b>Usage:</b> Normalize angles to 0-360° range for bearing calculations.
     *
     * <p><b>Example:</b> -30° → 330°, 450° → 90°
     */
    public static final double FULL_CIRCLE_DEGREES = 360.0;

    /**
     * Half circle in degrees (180°).
     *
     * <p><b>Usage:</b> Normalize relative wind angles to 0-180° range.
     * We only care about the smallest angular difference, not which side.
     *
     * <p><b>Example:</b> |350° - 10°| = 340°, but normalized = 20° (shortest path)
     */
    public static final double HALF_CIRCLE_DEGREES = 180.0;

    // ==================== RISK LEVEL THRESHOLDS ====================

    /**
     * Number of warnings required for CRITICAL risk level (3 or more).
     *
     * <p><b>Risk Classification:</b>
     * <ul>
     *   <li>0 warnings → LOW risk</li>
     *   <li>1 warning → MEDIUM risk</li>
     *   <li>2 warnings → HIGH risk</li>
     *   <li>3+ warnings → CRITICAL risk</li>
     * </ul>
     */
    public static final int CRITICAL_RISK_WARNING_COUNT = 3;

    /**
     * Number of warnings required for HIGH risk level (2 or more).
     */
    public static final int HIGH_RISK_WARNING_COUNT = 2;

    // ==================== CONSTRUCTOR ====================

    /**
     * Private constructor to prevent instantiation.
     *
     * <p>This is a utility class containing only static constants.
     * Attempting to instantiate will throw an AssertionError.
     *
     * @throws AssertionError always
     */
    private WeatherConstants() {
        throw new AssertionError("WeatherConstants is a utility class and cannot be instantiated");
    }
}
