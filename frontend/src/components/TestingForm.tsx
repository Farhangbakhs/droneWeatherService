import { useState } from 'react';

interface TestingFormProps {
  servicePoint: { lat: number; lng: number } | null;
  deliveryPoint: { lat: number; lng: number } | null;
  onCalculate: (params: TestingParams) => void;
  onReset: () => void;
  disabled?: boolean;
}

export interface TestingParams {
  servicePoint: { lat: number; lng: number };
  deliveryPoint: { lat: number; lng: number };
  date: string;
  time: string;
  capacity: number;
}

export default function TestingForm({
  servicePoint,
  deliveryPoint,
  onCalculate,
  onReset,
  disabled,
}: TestingFormProps) {
  const [capacity, setCapacity] = useState('5.0');
  const [date, setDate] = useState('');
  const [time, setTime] = useState('');

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();

    if (!servicePoint || !deliveryPoint) {
      return;
    }

    onCalculate({
      servicePoint,
      deliveryPoint,
      date,
      time,
      capacity: parseFloat(capacity),
    });
  };

  const handleReset = () => {
    setCapacity('5.0');
    setDate('');
    setTime('');
    onReset();
  };

  const isFormValid = servicePoint !== null && deliveryPoint !== null;

  return (
    <div className="card">
      <div className="mb-6">
        <h2 className="text-xl font-bold text-primary-400 uppercase tracking-wider">
          [TESTING PARAMETERS]
        </h2>
        <p className="mt-2 text-sm text-retro-text font-mono">
          &gt; CONFIGURE DELIVERY PARAMETERS FOR COORDINATE-BASED TESTING
        </p>
      </div>

      <form onSubmit={handleSubmit} className="space-y-6">
        {/* Delivery Requirements */}
        <div>
          <h3 className="text-sm font-bold text-retro-accent uppercase tracking-wider mb-3 font-mono">
            [DELIVERY REQUIREMENTS]
          </h3>

          <div className="space-y-4">
            <div>
              <label
                htmlFor="capacity"
                className="block text-xs font-bold text-retro-text mb-2 font-mono uppercase tracking-wider"
              >
                CAPACITY (KG)
              </label>
              <input
                type="number"
                id="capacity"
                step="0.1"
                min="0"
                value={capacity}
                onChange={(e) => setCapacity(e.target.value)}
                className="input"
                disabled={disabled}
              />
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div>
                <label
                  htmlFor="date"
                  className="block text-xs font-bold text-retro-text mb-2 font-mono uppercase tracking-wider"
                >
                  DATE (OPTIONAL - FOR FORECAST)
                </label>
                <input
                  type="date"
                  id="date"
                  value={date}
                  onChange={(e) => setDate(e.target.value)}
                  className="input"
                  disabled={disabled}
                />
              </div>
              <div>
                <label
                  htmlFor="time"
                  className="block text-xs font-bold text-retro-text mb-2 font-mono uppercase tracking-wider"
                >
                  TIME (OPTIONAL - FOR FORECAST)
                </label>
                <input
                  type="time"
                  id="time"
                  value={time}
                  onChange={(e) => setTime(e.target.value)}
                  className="input"
                  disabled={disabled}
                />
              </div>
            </div>
            <p className="text-xs text-retro-text opacity-70 font-mono -mt-2">
              &gt; LEAVE EMPTY FOR CURRENT WEATHER, SET FUTURE DATE FOR FORECAST (MAX 5 DAYS)
            </p>
          </div>
        </div>

        {/* Actions */}
        <div className="flex gap-3 pt-4">
          <button
            type="submit"
            disabled={disabled || !isFormValid}
            className="btn-primary flex-1"
            title={!isFormValid ? 'Please select both service and delivery points on the map' : ''}
          >
            [CALCULATE ROUTE]
          </button>
          <button
            type="button"
            onClick={handleReset}
            disabled={disabled}
            className="btn-secondary"
          >
            [RESET ALL]
          </button>
        </div>

        {/* Status Message */}
        {!isFormValid && (
          <div className="bg-yellow-900 border-2 border-yellow-500 p-3 text-center">
            <p className="text-xs text-yellow-300 font-mono">
              &gt; PLEASE SELECT BOTH SERVICE AND DELIVERY POINTS ON THE MAP ABOVE
            </p>
          </div>
        )}
      </form>
    </div>
  );
}
