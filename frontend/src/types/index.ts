export interface LngLat {
  lng: number;
  lat: number;
}

export interface DispatchRec {
  capacity?: number;
  cooling?: boolean;
  heating?: boolean;
  maxCost?: number;
}

export interface MedDispatchRec {
  id?: number;
  date?: string;
  time?: string;
  requirements: DispatchRec;
  delivery: LngLat;
}

export interface WeatherSummary {
  timestamp: number;
  dateTime: string;
  temperatureCelsius: number;
  windSpeedMps: number;
  windGustMps?: number;
  windDirectionDeg?: number;
  condition: string;
  description: string;
  visibility?: number;
  humidity: number;
  precipitationMm: number;
  probabilityOfPrecipitation?: number;
  cloudCoverage?: number;
}

export interface FlightConditionsResponse {
  isSafe: boolean;
  riskLevel: string;
  warnings: string[];
  currentConditions: WeatherSummary;
  weatherDataSource?: string;
  metrics: {
    windSafe: boolean;
    visibilitySafe: boolean;
    precipitationSafe: boolean;
    temperatureSafe: boolean;
    maxSafeWindSpeed: number;
    minSafeVisibility: number;
    maxSafePrecipitation: number;
    minSafeTemp: number;
    maxSafeTemp: number;
  };
}

export interface DeliveryWindow {
  startTime: number;
  endTime: number;
  startDateTime: string;
  endDateTime: string;
  durationMinutes: number;
  safetyRating: string;
  averageWindSpeed: number;
  averageCondition: string;
  forecastDetails: WeatherSummary[];
}

export interface WeatherBlockedResponse {
  riskLevel: string;
  warnings: string[];
  windows: DeliveryWindow[];
}

export interface DronePathDTO {
  droneId: string;
  deliveries: DeliveryPathDTO[];
}

export interface DeliveryPathDTO {
  dispatchId?: number;
  flightPath: LngLat[];
}

export interface CalcDeliveryPathResult {
  totalCost: number;
  totalMoves: number;
  dronePaths: DronePathDTO[];
}

export interface PathSegmentWind {
  segmentIndex: number;
  flightDirection: number;
  windDirection: number;
  windSpeed: number;
  windImpactType: string;
  relativeWindAngle: number;
  costMultiplier: number;
}

export interface WindImpactAnalysis {
  baseCost: number;
  windAdjustedCost: number;
  costAdjustmentPercent: number;
  averageWindSpeed: number;
  averageWindDirection: number;
  segmentAnalysis: PathSegmentWind[];
}

export interface WeatherAwareDeliveryResult {
  deliveryPath: CalcDeliveryPathResult;
  weatherConditions: FlightConditionsResponse;
  windImpact: WindImpactAnalysis;
}

/**
 * Request DTO for weather-aware delivery path calculation
 * Supports both normal mode and testing mode
 */
export interface WeatherAwareDeliveryRequest {
  dispatches: MedDispatchRec[];

  /**
   * TESTING MODE ONLY: Explicitly specified service point coordinates
   * When provided along with skipServicePointSelection=true, the system will use
   * these coordinates directly instead of querying the ILP service points endpoint.
   * This dramatically speeds up calculations for international testing scenarios (30-60s → 2-3s).
   */
  explicitServicePoint?: LngLat;

  /**
   * TESTING MODE FLAG: When true, bypasses service point selection logic
   * Must be used together with explicitServicePoint for testing mode to activate.
   * Default: false (normal mode with automatic service point selection)
   */
  skipServicePointSelection?: boolean;
}
