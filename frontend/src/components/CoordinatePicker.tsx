import { useEffect } from 'react';
import { MapContainer, TileLayer, Marker, Polyline, useMapEvents } from 'react-leaflet';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';

interface CoordinatePickerProps {
  onServicePointSelect: (coords: { lat: number; lng: number }) => void;
  onDeliveryPointSelect: (coords: { lat: number; lng: number }) => void;
  servicePoint: { lat: number; lng: number } | null;
  deliveryPoint: { lat: number; lng: number } | null;
}

// Custom icons for service and delivery points
const serviceIcon = L.divIcon({
  className: 'custom-marker-icon',
  html: '<div style="background-color: #3b82f6; color: white; width: 32px; height: 32px; border: 3px solid white; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-weight: bold; font-size: 14px; box-shadow: 0 2px 4px rgba(0,0,0,0.3);">S</div>',
  iconSize: [32, 32],
  iconAnchor: [16, 16],
});

const deliveryIcon = L.divIcon({
  className: 'custom-marker-icon',
  html: '<div style="background-color: #ef4444; color: white; width: 32px; height: 32px; border: 3px solid white; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-weight: bold; font-size: 14px; box-shadow: 0 2px 4px rgba(0,0,0,0.3);">D</div>',
  iconSize: [32, 32],
  iconAnchor: [16, 16],
});

function MapClickHandler({
  servicePoint,
  deliveryPoint,
  onServicePointSelect,
  onDeliveryPointSelect
}: CoordinatePickerProps) {
  const map = useMapEvents({
    click: (e) => {
      if (!servicePoint) {
        onServicePointSelect({ lat: e.latlng.lat, lng: e.latlng.lng });
      } else if (!deliveryPoint) {
        onDeliveryPointSelect({ lat: e.latlng.lat, lng: e.latlng.lng });
      }
    },
  });

  // Update cursor style based on selection state
  useEffect(() => {
    if (!servicePoint || !deliveryPoint) {
      map.getContainer().style.cursor = 'crosshair';
    } else {
      map.getContainer().style.cursor = '';
    }
  }, [servicePoint, deliveryPoint, map]);

  return null;
}

export default function CoordinatePicker({
  servicePoint,
  deliveryPoint,
  onServicePointSelect,
  onDeliveryPointSelect,
}: CoordinatePickerProps) {
  const edinburghCenter: [number, number] = [55.9445, -3.1892];

  // Create line between points if both are selected
  const linePositions: [number, number][] =
    servicePoint && deliveryPoint
      ? [
          [servicePoint.lat, servicePoint.lng],
          [deliveryPoint.lat, deliveryPoint.lng],
        ]
      : [];

  return (
    <div className="space-y-4">
      {/* Instructions Panel */}
      <div className="bg-retro-bg border-2 border-primary-500 p-4">
        <div className="flex items-start gap-3">
          <div className="flex-shrink-0">
            <div className="w-8 h-8 border-2 border-primary-500 flex items-center justify-center text-primary-500 font-bold text-xl">
              {!servicePoint ? '1' : !deliveryPoint ? '2' : '✓'}
            </div>
          </div>
          <div className="flex-1">
            <h4 className="text-sm font-bold text-primary-400 uppercase tracking-wider font-mono">
              {!servicePoint && '[STEP 1: SELECT SERVICE POINT]'}
              {servicePoint && !deliveryPoint && '[STEP 2: SELECT DELIVERY POINT]'}
              {servicePoint && deliveryPoint && '[BOTH POINTS SELECTED]'}
            </h4>
            <p className="mt-1 text-xs text-retro-text font-mono">
              {!servicePoint && '> Click anywhere on the map to set the service point location'}
              {servicePoint && !deliveryPoint && '> Click anywhere on the map to set the delivery destination'}
              {servicePoint && deliveryPoint && '> Use the Reset button below to clear selection and start over'}
            </p>
          </div>
        </div>
      </div>

      {/* Map Container */}
      <div className="w-full h-full min-h-[500px] border-4 border-retro-border bg-retro-panel shadow-retro-lg">
        <MapContainer
          center={edinburghCenter}
          zoom={13}
          className="w-full h-full min-h-[500px]"
          style={{ background: '#1a1a2e' }}
        >
          <TileLayer
            attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
            url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
          />

          <MapClickHandler
            servicePoint={servicePoint}
            deliveryPoint={deliveryPoint}
            onServicePointSelect={onServicePointSelect}
            onDeliveryPointSelect={onDeliveryPointSelect}
          />

          {/* Service Point Marker */}
          {servicePoint && (
            <Marker
              position={[servicePoint.lat, servicePoint.lng]}
              icon={serviceIcon}
            />
          )}

          {/* Delivery Point Marker */}
          {deliveryPoint && (
            <Marker
              position={[deliveryPoint.lat, deliveryPoint.lng]}
              icon={deliveryIcon}
            />
          )}

          {/* Line between points */}
          {linePositions.length > 0 && (
            <Polyline
              positions={linePositions}
              color="#6366f1"
              weight={3}
              opacity={0.7}
              dashArray="10, 10"
            />
          )}
        </MapContainer>
      </div>

      {/* Coordinates Display */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        {/* Service Point Coordinates */}
        <div className={`p-4 border-2 ${servicePoint ? 'border-blue-500 bg-blue-950' : 'border-retro-border bg-retro-bg'}`}>
          <div className="flex items-center gap-2 mb-2">
            <div className="w-6 h-6 bg-blue-500 border-2 border-white rounded-full flex items-center justify-center text-white text-xs font-bold">
              S
            </div>
            <h4 className="text-xs font-bold text-retro-text uppercase tracking-wider font-mono">
              SERVICE POINT
            </h4>
          </div>
          {servicePoint ? (
            <div className="text-sm text-retro-text font-mono space-y-1">
              <div>LAT: {servicePoint.lat.toFixed(6)}</div>
              <div>LNG: {servicePoint.lng.toFixed(6)}</div>
            </div>
          ) : (
            <div className="text-sm text-retro-text opacity-50 font-mono">
              NOT SELECTED
            </div>
          )}
        </div>

        {/* Delivery Point Coordinates */}
        <div className={`p-4 border-2 ${deliveryPoint ? 'border-red-500 bg-red-950' : 'border-retro-border bg-retro-bg'}`}>
          <div className="flex items-center gap-2 mb-2">
            <div className="w-6 h-6 bg-red-500 border-2 border-white rounded-full flex items-center justify-center text-white text-xs font-bold">
              D
            </div>
            <h4 className="text-xs font-bold text-retro-text uppercase tracking-wider font-mono">
              DELIVERY POINT
            </h4>
          </div>
          {deliveryPoint ? (
            <div className="text-sm text-retro-text font-mono space-y-1">
              <div>LAT: {deliveryPoint.lat.toFixed(6)}</div>
              <div>LNG: {deliveryPoint.lng.toFixed(6)}</div>
            </div>
          ) : (
            <div className="text-sm text-retro-text opacity-50 font-mono">
              NOT SELECTED
            </div>
          )}
        </div>
      </div>

      {/* Distance Display (if both points selected) */}
      {servicePoint && deliveryPoint && (
        <div className="bg-retro-bg border-2 border-primary-500 p-3 text-center">
          <div className="text-xs text-retro-text font-mono">
            APPROXIMATE DISTANCE: {calculateDistance(servicePoint, deliveryPoint).toFixed(0)} METERS
          </div>
        </div>
      )}
    </div>
  );
}

// Helper function to calculate distance between two points (Haversine formula)
function calculateDistance(
  point1: { lat: number; lng: number },
  point2: { lat: number; lng: number }
): number {
  const R = 6371000; // Earth's radius in meters
  const lat1Rad = (point1.lat * Math.PI) / 180;
  const lat2Rad = (point2.lat * Math.PI) / 180;
  const deltaLat = ((point2.lat - point1.lat) * Math.PI) / 180;
  const deltaLng = ((point2.lng - point1.lng) * Math.PI) / 180;

  const a =
    Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2) +
    Math.cos(lat1Rad) * Math.cos(lat2Rad) * Math.sin(deltaLng / 2) * Math.sin(deltaLng / 2);
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

  return R * c;
}
