import { useState } from 'react';
import type { MedDispatchRec } from '../types';

interface DeliveryFormProps {
  onSubmit: (dispatches: MedDispatchRec[]) => void;
  onReset: () => void;
  disabled?: boolean;
}

export default function DeliveryForm({ onSubmit, onReset, disabled }: DeliveryFormProps) {
  const [latitude, setLatitude] = useState('55.9445');
  const [longitude, setLongitude] = useState('-3.1892');
  const [capacity, setCapacity] = useState('5.0');
  const [date, setDate] = useState('');
  const [time, setTime] = useState('');

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();

    const dispatch: MedDispatchRec = {
      date: date ? date : undefined,
      time: time ? time : undefined,
      requirements: {
        capacity: parseFloat(capacity) || undefined,
      },
      delivery: {
        lng: parseFloat(longitude),
        lat: parseFloat(latitude),
      },
    };

    onSubmit([dispatch]);
  };

  const handleReset = () => {
    setLatitude('55.9445');
    setLongitude('-3.1892');
    setCapacity('5.0');
    setDate('');
    setTime('');
    onReset();
  };

  return (
    <div className="card">
      <div className="mb-6">
        <h2 className="text-xl font-bold text-primary-400 uppercase tracking-wider">[DELIVERY CONFIGURATION]</h2>
        <p className="mt-2 text-sm text-retro-text font-mono">
          &gt; CONFIGURE DELIVERY PARAMETERS AND DESTINATION
        </p>
      </div>

      <form onSubmit={handleSubmit} className="space-y-6">
        {/* Delivery Location */}
        <div>
          <h3 className="text-sm font-bold text-retro-accent uppercase tracking-wider mb-3 font-mono">
            [DELIVERY LOCATION]
          </h3>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label htmlFor="latitude" className="block text-xs font-bold text-retro-text mb-2 font-mono uppercase tracking-wider">
                LATITUDE
              </label>
              <input
                type="number"
                id="latitude"
                step="0.0001"
                min="-90"
                max="90"
                value={latitude}
                onChange={(e) => setLatitude(e.target.value)}
                className="input"
                required
                disabled={disabled}
              />
            </div>
            <div>
              <label htmlFor="longitude" className="block text-xs font-bold text-retro-text mb-2 font-mono uppercase tracking-wider">
                LONGITUDE
              </label>
              <input
                type="number"
                id="longitude"
                step="0.0001"
                min="-180"
                max="180"
                value={longitude}
                onChange={(e) => setLongitude(e.target.value)}
                className="input"
                required
                disabled={disabled}
              />
            </div>
          </div>
          <p className="mt-2 text-xs text-retro-text opacity-70 font-mono">
            &gt; DEFAULT: EDINBURGH, SCOTLAND | TRY: OCEAN TERMINAL (55.9797, -3.1780)
          </p>
        </div>

        {/* Requirements */}
        <div>
          <h3 className="text-sm font-bold text-retro-accent uppercase tracking-wider mb-3 font-mono">
            [DELIVERY REQUIREMENTS]
          </h3>

          <div className="space-y-4">
            <div>
              <label htmlFor="capacity" className="block text-xs font-bold text-retro-text mb-2 font-mono uppercase tracking-wider">
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
                <label htmlFor="date" className="block text-xs font-bold text-retro-text mb-2 font-mono uppercase tracking-wider">
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
                <label htmlFor="time" className="block text-xs font-bold text-retro-text mb-2 font-mono uppercase tracking-wider">
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
            disabled={disabled}
            className="btn-primary flex-1"
          >
            [ANALYZE DELIVERY]
          </button>
          <button
            type="button"
            onClick={handleReset}
            disabled={disabled}
            className="btn-secondary"
          >
            [RESET]
          </button>
        </div>
      </form>
    </div>
  );
}
