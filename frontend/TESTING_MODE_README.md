# Testing Mode - Coordinate-Based Testing Interface

## Overview

The Testing Mode provides an interactive map-based interface for testing the weather-aware drone delivery system using direct coordinate selection instead of address geocoding.

## Features

### 1. Interactive Map Selection
- **Two-step coordinate selection**:
  1. Click map to select **Service Point** (blue marker with 'S')
  2. Click map again to select **Delivery Point** (red marker with 'D')
- Visual feedback with colored markers and connecting line
- Real-time coordinate display
- Distance calculation between points

### 2. Testing Parameters
- **Capacity**: Drone payload capacity in kg
- **Date/Time** (Optional):
  - Leave empty for current weather
  - Set future date/time for forecast weather (max 5 days)

### 3. Results Display
- Reuses existing production components:
  - Weather conditions analysis
  - Flight path visualization
  - Wind impact charts
  - Safety checks

## Usage

### Accessing Testing Mode

1. Start the application
2. Navigate to the testing page:
   - Click **[TESTING MODE]** in the navigation bar
   - Or visit: `http://localhost:5173/testing`

### Testing Workflow

1. **Select Service Point**
   - Click anywhere on the map
   - Blue marker with 'S' appears
   - Coordinates displayed in left panel

2. **Select Delivery Point**
   - Click map again
   - Red marker with 'D' appears
   - Dashed line connects both points
   - Distance shown below map

3. **Configure Parameters**
   - Set capacity (default: 5.0 kg)
   - Optionally set date/time for forecast testing

4. **Calculate Route**
   - Click `[CALCULATE ROUTE]` button
   - System analyzes:
     - Weather conditions at delivery location
     - Optimal flight path
     - Wind impact on cost
     - Safety parameters

5. **View Results**
   - Weather safety assessment
   - Flight path map with service point and delivery location
   - Wind impact analysis
   - Cost breakdown

### Reset and Retry

- Click `[RESET ALL]` to:
  - Clear all markers
  - Reset form fields
  - Remove results
  - Start fresh testing session

## Technical Details

### Components

```
frontend/src/
 pages/
    TestingPage.tsx          # Main testing page container
 components/
    CoordinatePicker.tsx     # Interactive map for coordinate selection
    TestingForm.tsx          # Testing parameters form
    WeatherResults.tsx       # Results display (reused from main app)
    PathMap.tsx              # Flight path visualization (reused)
    WindImpactChart.tsx      # Wind analysis chart (reused)
```

### API Integration

The testing mode uses the same backend API as the main application:

**Endpoint**: `POST /api/v1/calcWeatherAwareDeliveryPath`

**Request Format**:
```json
[
  {
    "delivery": {
      "lng": -3.1892,
      "lat": 55.9445
    },
    "requirements": {
      "capacity": 5.0
    },
    "date": "2025-01-15",  // Optional - for forecast
    "time": "14:30:00"     // Optional - for forecast
  }
]
```

**Response Types**:

1. **Safe to fly** - `WeatherAwareDeliveryResult`:
```json
{
  "deliveryPath": {
    "totalCost": 150.0,
    "totalMoves": 75,
    "dronePaths": [...]
  },
  "weatherConditions": {
    "isSafe": true,
    "riskLevel": "LOW",
    "currentConditions": {...}
  },
  "windImpact": {
    "baseCost": 150.0,
    "windAdjustedCost": 165.5,
    "costAdjustmentPercent": 10.3,
    "segmentAnalysis": [...]
  }
}
```

2. **Unsafe conditions** - `WeatherBlockedResponse`:
```json
{
  "riskLevel": "HIGH",
  "warnings": [
    "Wind speed exceeds safe limit",
    "Poor visibility"
  ],
  "windows": [
    {
      "startTime": 1234567890,
      "endTime": 1234571490,
      "safetyRating": "GOOD",
      "averageWindSpeed": 5.2
    }
  ]
}
```

### Service Point Selection

**Important**: The testing mode interface allows you to visually select coordinates on the map, but the backend automatically selects the optimal service point based on:

1. **Proximity**: Closest service point to delivery location
2. **Drone availability**: Service points with available drones
3. **Capacity**: Service points with drones meeting capacity requirements

The blue "S" marker in the testing interface represents the **starting point for visual reference only**. The actual service point used for the delivery is determined by the backend based on the algorithm above.

### Coordinate System

- **Format**: WGS84 (latitude/longitude)
- **Precision**: 6 decimal places
- **Default center**: Edinburgh, Scotland (55.9445, -3.1892)
- **Zoom level**: 13

## Testing Scenarios

### 1. Short Distance Delivery
- Service and delivery ~500m apart
- Tests basic pathfinding and wind impact

### 2. Long Distance Delivery
- Service and delivery ~5km apart
- Tests extended flight paths and cumulative wind effects

### 3. Current Weather Testing
- Leave date/time empty
- Tests real-time weather integration

### 4. Forecast Weather Testing
- Set date/time 1-5 days in future
- Tests forecast-based planning

### 5. Extreme Weather Scenarios
- Test during high wind periods
- Verify weather blocking and alternative windows

### 6. Edge Cases
- Selections near map boundaries
- Maximum distance deliveries
- Minimum distance deliveries

## Comparison: Testing Mode vs Address Mode

| Feature | Address Mode | Testing Mode |
|---------|-------------|--------------|
| Input Method | Text address | Map click |
| Geocoding | Required | Not needed |
| Coordinate Precision | Depends on geocoder | Exact (6 decimals) |
| Speed | Slower (geocoding) | Faster |
| Use Case | Production | Testing/Development |
| Service Point | Auto-selected | Auto-selected |

## Browser Compatibility

- Chrome/Edge: Full support
- Firefox: Full support
- Safari: Full support
- Mobile: Touch-enabled map interaction

## Known Limitations

1. **Service Point Display**: The visual "S" marker is for reference only. The actual service point used by the backend may be different based on drone availability and proximity algorithms.

2. **No Direct Service Point Control**: You cannot manually override which service point the backend uses. The backend automatically selects the optimal service point.

3. **Map Bounds**: Currently centered on Edinburgh. Future versions could support multiple regions.

4. **Single Delivery**: Testing mode currently supports one delivery at a time. Multi-delivery testing requires using the API directly.

## Future Enhancements

- [ ] Display actual service points as reference markers
- [ ] Show which service point was selected in results
- [ ] Multi-delivery testing support
- [ ] Save/load test scenarios
- [ ] Export test results as JSON
- [ ] Validate coordinates against no-fly zones before API call
- [ ] Historical weather testing (if API supports)
- [ ] Comparison mode (test multiple scenarios side-by-side)

## Development Notes

### Adding New Features

1. **Map Enhancements**: Edit `CoordinatePicker.tsx`
2. **Form Fields**: Edit `TestingForm.tsx`
3. **Results Display**: Edit `WeatherResults.tsx` (affects both modes)
4. **Routing**: Edit `App.tsx`

### Styling

The testing mode uses the same retro terminal theme as the main application:
- Tailwind CSS utility classes
- Custom component classes (`.card`, `.btn-primary`, etc.)
- Retro color scheme defined in `tailwind.config.js`

### TypeScript Types

All types are defined in `src/types/index.ts` and shared between:
- Main address-based mode
- Testing coordinate-based mode
- API client

## Troubleshooting

### "CALCULATE ROUTE" Button Disabled
- **Cause**: One or both points not selected
- **Solution**: Click the map twice to select both service and delivery points

### Map Not Loading
- **Cause**: Leaflet CSS not loaded
- **Solution**: Check that `leaflet/dist/leaflet.css` is imported

### Coordinates Not Updating
- **Cause**: Click handler not registered
- **Solution**: Check browser console for errors

### API Errors
- **Cause**: Backend not running or invalid coordinates
- **Solution**:
  - Verify backend is running on correct port
  - Check coordinate values are valid (lat: -90 to 90, lng: -180 to 180)

## Support

For issues or feature requests related to testing mode:
1. Check this README
2. Review the main application documentation
3. Check the backend API documentation
4. Review the codebase comments
