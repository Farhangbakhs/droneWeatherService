import type { WeatherAwareDeliveryResult, WeatherBlockedResponse, LngLat } from '../types';
import WindImpactChart from './WindImpactChart';
import PathMap from './PathMap';

interface WeatherResultsProps {
  result: WeatherAwareDeliveryResult | WeatherBlockedResponse;
}

function isWeatherBlocked(result: WeatherAwareDeliveryResult | WeatherBlockedResponse): result is WeatherBlockedResponse {
  return 'windows' in result;
}

export default function WeatherResults({ result }: WeatherResultsProps) {
  if (isWeatherBlocked(result)) {
    return <WeatherBlockedView result={result} />;
  }

  return <SuccessfulDeliveryView result={result} />;
}

function WeatherBlockedView({ result }: { result: WeatherBlockedResponse }) {
  return (
    <div className="space-y-6">
      {/* Weather Warning */}
      <div className="card border-retro-danger">
        <div className="flex items-start gap-3">
          <div className="flex-shrink-0">
            <div className="w-8 h-8 border-2 border-retro-danger flex items-center justify-center text-retro-danger font-bold text-xl">
              !
            </div>
          </div>
          <div className="flex-1">
            <h3 className="text-lg font-bold text-red-400 uppercase tracking-wider">
              [UNSAFE WEATHER CONDITIONS]
            </h3>
            <div className="mt-2">
              <span className={`badge ${getRiskBadgeClass(result.riskLevel)}`}>
                RISK: {result.riskLevel}
              </span>
            </div>
            <div className="mt-3 space-y-2">
              {result.warnings.map((warning, index) => (
                <div key={index} className="flex items-start gap-2 text-sm text-red-300">
                  <span className="flex-shrink-0 mt-0.5">&gt;</span>
                  <span>{warning}</span>
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>

      {/* Alternative Windows */}
      {result.windows.length > 0 && (
        <div className="card">
          <h3 className="text-lg font-bold text-retro-accent uppercase tracking-wider mb-4">
            [ALTERNATIVE DELIVERY WINDOWS]
          </h3>
          <p className="text-sm text-retro-text mb-4 font-mono">
            &gt; {result.windows.length} SAFE WINDOW{result.windows.length !== 1 ? 'S' : ''} DETECTED IN FORECAST
          </p>
          <div className="space-y-3">
            {result.windows.map((window, index) => (
              <div key={index} className="border-2 border-retro-border bg-retro-bg p-4">
                <div className="flex items-start justify-between">
                  <div className="flex-1">
                    <div className="flex items-center gap-2 mb-2">
                      <span className={`badge ${getSafetyBadgeClass(window.safetyRating)}`}>
                        {window.safetyRating}
                      </span>
                      <span className="text-sm text-retro-text font-mono">
                        [{window.durationMinutes} MIN]
                      </span>
                    </div>
                    <div className="text-sm text-retro-text font-mono">
                      <div className="font-bold">
                        START: {new Date(window.startTime * 1000).toLocaleString()}
                      </div>
                      <div className="text-retro-text opacity-70">
                        END: {new Date(window.endTime * 1000).toLocaleString()}
                      </div>
                    </div>
                    <div className="mt-2 text-sm text-retro-text opacity-70 font-mono">
                      WIND: {window.averageWindSpeed.toFixed(1)} M/S | {window.averageCondition.toUpperCase()}
                    </div>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {result.windows.length === 0 && (
        <div className="card text-center py-8">
          <div className="text-red-500 text-4xl mb-4 font-mono">[X]</div>
          <h3 className="text-sm font-bold text-retro-text uppercase tracking-wider">NO SAFE WINDOWS AVAILABLE</h3>
          <p className="mt-1 text-sm text-retro-text opacity-70 font-mono">
            NO SUITABLE DELIVERY WINDOWS IN FORECAST PERIOD
          </p>
        </div>
      )}
    </div>
  );
}

function SuccessfulDeliveryView({ result }: { result: WeatherAwareDeliveryResult }) {
  const { deliveryPath, weatherConditions, windImpact } = result;

  // Extract flight paths for map visualization
  const flightPaths: LngLat[][] = deliveryPath.dronePaths.map(dronePath =>
    dronePath.deliveries.flatMap(delivery => delivery.flightPath)
  );

  return (
    <div className="space-y-6">
      {/* Success Header */}
      <div className="max-w-4xl mx-auto">
        <div className="card border-retro-success">
          <div className="flex items-start gap-3">
            <div className="flex-shrink-0">
              <div className="w-8 h-8 border-2 border-retro-success flex items-center justify-center text-retro-success font-bold text-xl">
                ✓
              </div>
            </div>
            <div className="flex-1">
              <h3 className="text-lg font-bold text-green-400 uppercase tracking-wider">
                [SAFE TO FLY]
              </h3>
              <p className="mt-1 text-green-300 font-mono text-sm">
                &gt; WEATHER CONDITIONS WITHIN SAFE PARAMETERS
              </p>
              {weatherConditions.weatherDataSource && (
                <p className="mt-1 text-blue-300 font-mono text-sm">
                  &gt; {weatherConditions.weatherDataSource.toUpperCase()}
                </p>
              )}
              <div className="mt-2">
                <span className={`badge ${getRiskBadgeClass(weatherConditions.riskLevel)}`}>
                  RISK: {weatherConditions.riskLevel}
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Top Row: Key Metrics Side by Side */}
      <div className="max-w-full mx-auto grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Current Weather */}
        <div className="card">
          <h3 className="text-lg font-bold text-primary-400 uppercase tracking-wider mb-4">
            [CURRENT WEATHER CONDITIONS]
          </h3>
          <div className="grid grid-cols-2 gap-3">
            <WeatherMetric
              label="TEMPERATURE"
              value={`${weatherConditions.currentConditions.temperatureCelsius.toFixed(1)}°C`}
            />
            <WeatherMetric
              label="WIND SPEED"
              value={`${weatherConditions.currentConditions.windSpeedMps.toFixed(1)} M/S`}
            />
            <WeatherMetric
              label="VISIBILITY"
              value={`${((weatherConditions.currentConditions.visibility || 0) / 1000).toFixed(1)} KM`}
            />
            <WeatherMetric
              label="HUMIDITY"
              value={`${weatherConditions.currentConditions.humidity}%`}
            />
            <WeatherMetric
              label="CONDITION"
              value={weatherConditions.currentConditions.condition.toUpperCase()}
            />
            <WeatherMetric
              label="PRECIP"
              value={`${weatherConditions.currentConditions.precipitationMm.toFixed(1)} MM`}
            />
          </div>
        </div>

        {/* Delivery Path Summary */}
        <div className="card">
          <h3 className="text-lg font-bold text-primary-400 uppercase tracking-wider mb-4">
            [DELIVERY PATH SUMMARY]
          </h3>
          <div className="grid grid-cols-3 gap-3 mb-4">
            <div className="text-center p-4 bg-retro-bg border-2 border-retro-border">
              <div className="text-2xl font-bold text-primary-400 font-mono">
                {deliveryPath.totalMoves}
              </div>
              <div className="text-xs text-retro-text mt-1 font-mono uppercase tracking-wider">TOTAL MOVES</div>
            </div>
            <div className="text-center p-4 bg-retro-bg border-2 border-retro-border">
              <div className="text-2xl font-bold text-primary-400 font-mono">
                {deliveryPath.totalCost.toFixed(2)}
              </div>
              <div className="text-xs text-retro-text mt-1 font-mono uppercase tracking-wider">BASE COST</div>
            </div>
            <div className="text-center p-4 bg-retro-bg border-2 border-retro-border">
              <div className="text-2xl font-bold text-primary-400 font-mono">
                {deliveryPath.dronePaths.length}
              </div>
              <div className="text-xs text-retro-text mt-1 font-mono uppercase tracking-wider">DRONES</div>
            </div>
          </div>

          {/* Wind Impact Summary inline */}
          <div className="grid grid-cols-2 gap-3">
            <div className="p-3 bg-retro-bg border-2 border-retro-border">
              <div className="text-xs text-retro-text mb-1 font-mono uppercase tracking-wider">ADJUSTED COST</div>
              <div className="text-xl font-bold text-retro-text font-mono">
                {windImpact.windAdjustedCost.toFixed(2)}
              </div>
              <div className={`text-xs mt-1 font-bold font-mono ${windImpact.costAdjustmentPercent > 0 ? 'text-red-400' : 'text-green-400'}`}>
                {windImpact.costAdjustmentPercent > 0 ? '+' : ''}{windImpact.costAdjustmentPercent.toFixed(1)}% FROM BASE
              </div>
            </div>
            <div className="p-3 bg-retro-bg border-2 border-retro-border">
              <div className="text-xs text-retro-text mb-1 font-mono uppercase tracking-wider">AVERAGE WIND</div>
              <div className="text-xl font-bold text-retro-text font-mono">
                {windImpact.averageWindSpeed.toFixed(1)} M/S
              </div>
              <div className="text-xs text-retro-text mt-1 font-mono">
                DIR: {windImpact.averageWindDirection}°
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Flight Path Map - Full Width */}
      <div className="max-w-full mx-auto">
        <div className="card">
          <h3 className="text-lg font-bold text-primary-400 uppercase tracking-wider mb-4">
            [FLIGHT PATH VISUALIZATION]
          </h3>
          <PathMap flightPaths={flightPaths} />
        </div>
      </div>

      {/* Wind Impact Chart - Full Width */}
      {windImpact.segmentAnalysis.length > 0 && (
        <div className="max-w-full mx-auto">
          <div className="card">
            <h3 className="text-lg font-bold text-primary-400 uppercase tracking-wider mb-4">
              [WIND IMPACT ANALYSIS BY SEGMENT]
            </h3>
            <WindImpactChart segments={windImpact.segmentAnalysis} />
          </div>
        </div>
      )}

      {/* Safety Checks - Centered */}
      <div className="max-w-4xl mx-auto">
        <div className="card">
          <h3 className="text-lg font-bold text-primary-400 uppercase tracking-wider mb-4">
            [SAFETY CHECKS]
          </h3>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <SafetyCheck
              label="WIND CONDITIONS"
              passed={weatherConditions.metrics.windSafe}
              details={`MAX SAFE: ${weatherConditions.metrics.maxSafeWindSpeed} M/S`}
            />
            <SafetyCheck
              label="VISIBILITY"
              passed={weatherConditions.metrics.visibilitySafe}
              details={`MIN SAFE: ${weatherConditions.metrics.minSafeVisibility / 1000} KM`}
            />
            <SafetyCheck
              label="PRECIPITATION"
              passed={weatherConditions.metrics.precipitationSafe}
              details={`MAX SAFE: ${weatherConditions.metrics.maxSafePrecipitation} MM/H`}
            />
            <SafetyCheck
              label="TEMPERATURE RANGE"
              passed={weatherConditions.metrics.temperatureSafe}
              details={`RANGE: ${weatherConditions.metrics.minSafeTemp}°C TO ${weatherConditions.metrics.maxSafeTemp}°C`}
            />
          </div>
        </div>
      </div>
    </div>
  );
}

function WeatherMetric({ label, value }: { label: string; value: string }) {
  return (
    <div className="p-3 bg-retro-bg border-2 border-retro-border">
      <div className="text-xs text-retro-text opacity-70 font-mono uppercase tracking-wider mb-1">{label}</div>
      <div className="text-sm font-bold text-retro-accent font-mono">{value}</div>
    </div>
  );
}

function SafetyCheck({ label, passed, details }: { label: string; passed: boolean; details: string }) {
  return (
    <div className="flex items-center justify-between p-3 bg-retro-bg border-2 border-retro-border">
      <div className="flex items-center gap-3">
        <div className={`flex-shrink-0 h-6 w-6 border-2 flex items-center justify-center font-bold ${passed ? 'border-retro-success text-retro-success' : 'border-retro-danger text-retro-danger'}`}>
          {passed ? '✓' : 'X'}
        </div>
        <div>
          <div className="text-sm font-bold text-retro-text font-mono uppercase tracking-wider">{label}</div>
          <div className="text-xs text-retro-text opacity-70 font-mono">{details}</div>
        </div>
      </div>
      <span className={`badge ${passed ? 'badge-success' : 'badge-danger'}`}>
        {passed ? 'PASS' : 'FAIL'}
      </span>
    </div>
  );
}

function getRiskBadgeClass(riskLevel: string): string {
  switch (riskLevel) {
    case 'LOW':
      return 'badge-success';
    case 'MEDIUM':
      return 'badge-warning';
    case 'HIGH':
    case 'CRITICAL':
      return 'badge-danger';
    default:
      return 'badge-info';
  }
}

function getSafetyBadgeClass(rating: string): string {
  switch (rating) {
    case 'EXCELLENT':
      return 'badge-success';
    case 'GOOD':
      return 'badge-info';
    case 'ACCEPTABLE':
      return 'badge-warning';
    default:
      return 'badge-info';
  }
}
