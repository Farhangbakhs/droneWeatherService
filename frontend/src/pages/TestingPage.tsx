import { useState } from 'react';
import CoordinatePicker from '../components/CoordinatePicker';
import TestingForm, { TestingParams } from '../components/TestingForm';
import WeatherResults from '../components/WeatherResults';
import LoadingSpinner from '../components/LoadingSpinner';
import { deliveryApi } from '../services/api';
import type { MedDispatchRec, WeatherAwareDeliveryResult, WeatherBlockedResponse } from '../types';

export default function TestingPage() {
  const [servicePoint, setServicePoint] = useState<{ lat: number; lng: number } | null>(null);
  const [deliveryPoint, setDeliveryPoint] = useState<{ lat: number; lng: number } | null>(null);
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState<WeatherAwareDeliveryResult | WeatherBlockedResponse | null>(null);
  const [error, setError] = useState<string | null>(null);

  const handleServicePointSelect = (coords: { lat: number; lng: number }) => {
    setServicePoint(coords);
  };

  const handleDeliveryPointSelect = (coords: { lat: number; lng: number }) => {
    setDeliveryPoint(coords);
  };

  const handleCalculate = async (params: TestingParams) => {
    setLoading(true);
    setError(null);
    setResult(null);

    try {
      // Construct the dispatch record
      const dispatch: MedDispatchRec = {
        date: params.date || undefined,
        time: params.time || undefined,
        requirements: {
          capacity: params.capacity || undefined,
        },
        delivery: {
          lng: params.deliveryPoint.lng,
          lat: params.deliveryPoint.lat,
        },
      };

      console.log('🧪 TESTING MODE: Calculating route with explicit service point');
      console.log('   Service Point:', params.servicePoint);
      console.log('   Delivery Point:', params.deliveryPoint);
      console.log('   This will bypass the slow ILP service point query');

      // CRITICAL: Use the new request format with testing mode fields
      // This enables the fast path that skips ILP service point endpoint query
      const data = await deliveryApi.calcWeatherAwareDeliveryPath({
        dispatches: [dispatch],
        explicitServicePoint: params.servicePoint,  // Pass the selected service point
        skipServicePointSelection: true,            // Enable testing mode bypass
      });

      console.log('✅ Route calculation complete');
      setResult(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'An error occurred while processing your request');
      console.error('❌ Route calculation failed:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleReset = () => {
    setServicePoint(null);
    setDeliveryPoint(null);
    setResult(null);
    setError(null);
  };

  return (
    <div className="min-h-screen bg-retro-bg">
      {/* Header */}
      <header className="bg-retro-panel border-b-4 border-retro-border shadow-retro">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6">
          <div className="flex items-center justify-between">
            <div>
              <h1 className="text-3xl font-display font-bold text-primary-400 tracking-wider">
                [COORDINATE-BASED TESTING MODE]
              </h1>
              <p className="mt-2 text-retro-text font-mono text-sm">
                &gt; INTERACTIVE MAP TESTING FOR WEATHER-AWARE DRONE DELIVERY
              </p>
            </div>
            <div className="flex items-center gap-3 font-mono text-xs">
              <div className="flex items-center gap-2">
                <span className="w-3 h-3 bg-retro-success animate-pulse"></span>
                <span className="text-retro-success font-bold">TESTING MODE</span>
              </div>
            </div>
          </div>
        </div>
      </header>

      {/* Navigation */}
      <nav className="bg-retro-panel border-b-2 border-retro-border">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex gap-4 py-3">
            <a
              href="/"
              className="px-4 py-2 text-sm font-mono text-retro-text hover:text-primary-400 hover:bg-retro-bg border-2 border-transparent hover:border-primary-500 transition-all uppercase tracking-wider"
            >
              [← BACK TO MAIN]
            </a>
            <div className="px-4 py-2 text-sm font-mono text-primary-400 bg-retro-bg border-2 border-primary-500 uppercase tracking-wider">
              [TESTING MODE]
            </div>
          </div>
        </div>
      </nav>

      {/* Main Content */}
      <main className="max-w-full mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {/* Instructions Section */}
        <div className="max-w-4xl mx-auto mb-8">
          <div className="card border-primary-500">
            <h2 className="text-lg font-bold text-primary-400 uppercase tracking-wider mb-3">
              [TESTING MODE INSTRUCTIONS]
            </h2>
            <div className="space-y-2 text-sm text-retro-text font-mono">
              <p>&gt; 1. CLICK THE MAP TO SELECT SERVICE POINT (BLUE MARKER WITH 'S')</p>
              <p>&gt; 2. CLICK THE MAP AGAIN TO SELECT DELIVERY POINT (RED MARKER WITH 'D')</p>
              <p>&gt; 3. CONFIGURE DELIVERY PARAMETERS (CAPACITY, DATE/TIME)</p>
              <p>&gt; 4. CLICK 'CALCULATE ROUTE' TO ANALYZE THE DELIVERY PATH</p>
              <p className="text-yellow-400">&gt; NOTE: THIS MODE BYPASSES ADDRESS GEOCODING FOR DIRECT COORDINATE TESTING</p>
            </div>
          </div>
        </div>

        {/* Coordinate Picker Section */}
        <div className="max-w-7xl mx-auto mb-8">
          <div className="card">
            <h2 className="text-xl font-bold text-primary-400 uppercase tracking-wider mb-6">
              [COORDINATE SELECTION]
            </h2>
            <CoordinatePicker
              servicePoint={servicePoint}
              deliveryPoint={deliveryPoint}
              onServicePointSelect={handleServicePointSelect}
              onDeliveryPointSelect={handleDeliveryPointSelect}
            />
          </div>
        </div>

        {/* Testing Form Section */}
        <div className="max-w-4xl mx-auto mb-8">
          <TestingForm
            servicePoint={servicePoint}
            deliveryPoint={deliveryPoint}
            onCalculate={handleCalculate}
            onReset={handleReset}
            disabled={loading}
          />
        </div>

        {/* Results Section */}
        <div className="max-w-full">
          {loading && (
            <div className="max-w-4xl mx-auto">
              <LoadingSpinner />
            </div>
          )}

          {error && (
            <div className="max-w-4xl mx-auto">
              <div className="card border-red-500">
                <div className="flex items-start gap-3">
                  <div className="flex-shrink-0">
                    <div className="w-6 h-6 border-2 border-red-500 flex items-center justify-center text-red-500 font-bold">
                      X
                    </div>
                  </div>
                  <div className="flex-1">
                    <h3 className="text-lg font-bold text-red-400">[ERROR]</h3>
                    <p className="mt-1 text-red-300">{error}</p>
                  </div>
                </div>
              </div>
            </div>
          )}

          {result && !loading && (
            <WeatherResults result={result} />
          )}

          {!loading && !result && !error && (servicePoint || deliveryPoint) && (
            <div className="max-w-4xl mx-auto">
              <div className="card text-center py-12">
                <div className="text-primary-500 text-6xl mb-4">[ ]</div>
                <h3 className="text-lg font-bold text-retro-text uppercase">&gt; WAITING FOR INPUT</h3>
                <p className="mt-2 text-retro-text opacity-70 font-mono text-sm">
                  {!servicePoint && 'SELECT SERVICE POINT ON MAP TO BEGIN'}
                  {servicePoint && !deliveryPoint && 'SELECT DELIVERY POINT ON MAP TO CONTINUE'}
                  {servicePoint && deliveryPoint && 'CONFIGURE PARAMETERS AND CLICK CALCULATE ROUTE'}
                </p>
              </div>
            </div>
          )}

          {!loading && !result && !error && !servicePoint && !deliveryPoint && (
            <div className="max-w-4xl mx-auto">
              <div className="card text-center py-12">
                <div className="text-primary-500 text-6xl mb-4">[ ]</div>
                <h3 className="text-lg font-bold text-retro-text uppercase">&gt; READY FOR TESTING</h3>
                <p className="mt-2 text-retro-text opacity-70 font-mono text-sm">
                  CLICK THE MAP ABOVE TO SELECT COORDINATES
                </p>
              </div>
            </div>
          )}
        </div>
      </main>

      {/* Footer */}
      <footer className="mt-16 border-t-4 border-retro-border bg-retro-panel shadow-retro">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6">
          <p className="text-center text-xs text-retro-text font-mono">
            WEATHER-AWARE DELIVERY SYSTEM v1.0.0 | TESTING MODE | COORDINATE-BASED PATH CALCULATION
          </p>
        </div>
      </footer>
    </div>
  );
}
