import { BrowserRouter as Router, Routes, Route, Link, useLocation } from 'react-router-dom';
import { useState } from 'react';
import DeliveryForm from './components/DeliveryForm';
import WeatherResults from './components/WeatherResults';
import LoadingSpinner from './components/LoadingSpinner';
import TestingPage from './pages/TestingPage';
import { deliveryApi } from './services/api';
import type { MedDispatchRec, WeatherAwareDeliveryResult, WeatherBlockedResponse } from './types';

function MainPage() {
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState<WeatherAwareDeliveryResult | WeatherBlockedResponse | null>(null);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (dispatches: MedDispatchRec[]) => {
    setLoading(true);
    setError(null);
    setResult(null);

    try {
      const data = await deliveryApi.calcWeatherAwareDeliveryPath(dispatches);
      setResult(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'An error occurred while processing your request');
      console.error('Error:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleReset = () => {
    setResult(null);
    setError(null);
  };

  return (
    <div className="min-h-screen bg-retro-bg">
      {/* Header */}
      <Header />

      {/* Main Content */}
      <main className="max-w-full mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {/* Form Section - Full Width */}
        <div className="max-w-4xl mx-auto mb-8">
          <DeliveryForm onSubmit={handleSubmit} onReset={handleReset} disabled={loading} />
        </div>

        {/* Results Section - Full Width */}
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

          {!loading && !result && !error && (
            <div className="max-w-4xl mx-auto">
              <div className="card text-center py-12">
                <div className="text-primary-500 text-6xl mb-4">[ ]</div>
                <h3 className="text-lg font-bold text-retro-text uppercase">&gt; READY FOR ANALYSIS</h3>
                <p className="mt-2 text-retro-text opacity-70 font-mono text-sm">
                  ENTER DELIVERY PARAMETERS TO BEGIN
                </p>
              </div>
            </div>
          )}
        </div>
      </main>

      {/* Footer */}
      <Footer />
    </div>
  );
}

function Header() {
  const location = useLocation();
  const isTestingMode = location.pathname === '/testing';

  return (
    <>
      <header className="bg-retro-panel border-b-4 border-retro-border shadow-retro">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6">
          <div className="flex items-center justify-between">
            <div>
              <h1 className="text-3xl font-display font-bold text-primary-400 tracking-wider">
                [WEATHER-AWARE DELIVERY SYSTEM]
              </h1>
              <p className="mt-2 text-retro-text font-mono text-sm">
                &gt; DRONE PATHFINDING WITH REAL-TIME WEATHER INTELLIGENCE
              </p>
            </div>
            <div className="flex items-center gap-3 font-mono text-xs">
              <div className="flex items-center gap-2">
                <span className="w-3 h-3 bg-retro-success animate-pulse"></span>
                <span className="text-retro-success font-bold">ONLINE</span>
              </div>
            </div>
          </div>
        </div>
      </header>

      {/* Navigation */}
      <nav className="bg-retro-panel border-b-2 border-retro-border">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex gap-4 py-3">
            <Link
              to="/"
              className={`px-4 py-2 text-sm font-mono border-2 transition-all uppercase tracking-wider ${
                !isTestingMode
                  ? 'text-primary-400 bg-retro-bg border-primary-500'
                  : 'text-retro-text border-transparent hover:text-primary-400 hover:bg-retro-bg hover:border-primary-500'
              }`}
            >
              [ADDRESS MODE]
            </Link>
            <Link
              to="/testing"
              className={`px-4 py-2 text-sm font-mono border-2 transition-all uppercase tracking-wider ${
                isTestingMode
                  ? 'text-primary-400 bg-retro-bg border-primary-500'
                  : 'text-retro-text border-transparent hover:text-primary-400 hover:bg-retro-bg hover:border-primary-500'
              }`}
            >
              [TESTING MODE]
            </Link>
          </div>
        </div>
      </nav>
    </>
  );
}

function Footer() {
  return (
    <footer className="mt-16 border-t-4 border-retro-border bg-retro-panel shadow-retro">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6">
        <p className="text-center text-xs text-retro-text font-mono">
          WEATHER-AWARE DELIVERY SYSTEM v1.0.0 | POWERED BY OPENWEATHERMAP API | CW3 EXTENSION
        </p>
      </div>
    </footer>
  );
}

function App() {
  return (
    <Router>
      <Routes>
        <Route path="/" element={<MainPage />} />
        <Route path="/testing" element={<TestingPage />} />
      </Routes>
    </Router>
  );
}

export default App;
