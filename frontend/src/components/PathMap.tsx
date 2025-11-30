import { useEffect } from 'react';
import { MapContainer, TileLayer, Polyline, Marker, Popup, useMap } from 'react-leaflet';
import type { LngLat } from '../types';
import 'leaflet/dist/leaflet.css';
import L from 'leaflet';

// Fix for default marker icons
delete (L.Icon.Default.prototype as any)._getIconUrl;
L.Icon.Default.mergeOptions({
  iconRetinaUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-icon-2x.png',
  iconUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-icon.png',
  shadowUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-shadow.png',
});

interface PathMapProps {
  flightPaths: LngLat[][];
  center?: LngLat;
}

function MapBounds({ paths }: { paths: LngLat[][] }) {
  const map = useMap();

  useEffect(() => {
    if (paths.length > 0) {
      const allPoints = paths.flat();
      if (allPoints.length > 0) {
        const bounds = L.latLngBounds(
          allPoints.map(p => [p.lat, p.lng] as [number, number])
        );
        map.fitBounds(bounds, { padding: [50, 50] });
      }
    }
  }, [paths, map]);

  return null;
}

export default function PathMap({ flightPaths, center }: PathMapProps) {
  const defaultCenter: [number, number] = center
    ? [center.lat, center.lng]
    : [55.9445, -3.1892];

  const colors = ['#00ff00', '#00ffff', '#ff00ff', '#ffff00', '#ff8800'];

  return (
    <div className="w-full h-full min-h-[500px] border-4 border-retro-border bg-retro-panel shadow-retro-lg">
      <MapContainer
        center={defaultCenter}
        zoom={13}
        className="w-full h-full min-h-[500px]"
        style={{ background: '#1a1a2e' }}
      >
        <TileLayer
          attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
          url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
        />

        <MapBounds paths={flightPaths} />

        {flightPaths.map((path, pathIndex) => {
          if (path.length === 0) return null;

          const color = colors[pathIndex % colors.length];
          const positions = path.map(p => [p.lat, p.lng] as [number, number]);

          return (
            <div key={pathIndex}>
              <Polyline
                positions={positions}
                color={color}
                weight={3}
                opacity={0.8}
              />

              {/* Start marker */}
              <Marker position={positions[0]}>
                <Popup>
                  <div className="font-mono text-xs">
                    <div className="font-bold">START {pathIndex + 1}</div>
                    <div>Lat: {path[0].lat.toFixed(6)}</div>
                    <div>Lng: {path[0].lng.toFixed(6)}</div>
                  </div>
                </Popup>
              </Marker>

              {/* End marker */}
              {path.length > 1 && (
                <Marker position={positions[positions.length - 1]}>
                  <Popup>
                    <div className="font-mono text-xs">
                      <div className="font-bold">END {pathIndex + 1}</div>
                      <div>Lat: {path[path.length - 1].lat.toFixed(6)}</div>
                      <div>Lng: {path[path.length - 1].lng.toFixed(6)}</div>
                    </div>
                  </Popup>
                </Marker>
              )}

              {/* Delivery points (consecutive duplicates) */}
              {path.map((point, idx) => {
                if (idx > 0 && idx < path.length - 1) {
                  if (point.lat === path[idx - 1].lat && point.lng === path[idx - 1].lng) {
                    return (
                      <Marker
                        key={`${pathIndex}-${idx}`}
                        position={[point.lat, point.lng]}
                        icon={L.divIcon({
                          className: 'delivery-marker',
                          html: '<div style="width:12px; height:12px; background:' + color + '; border:2px solid white; border-radius:50%;"></div>',
                          iconSize: [16, 16],
                          iconAnchor: [8, 8],
                        })}
                      >
                        <Popup>
                          <div className="font-mono text-xs">
                            <div className="font-bold text-retro-accent">DELIVERY</div>
                            <div>Path: {pathIndex + 1}</div>
                            <div>Lat: {point.lat.toFixed(6)}</div>
                            <div>Lng: {point.lng.toFixed(6)}</div>
                          </div>
                        </Popup>
                      </Marker>
                    );
                  }
                }
                return null;
              })}
            </div>
          );
        })}
      </MapContainer>
    </div>
  );
}
