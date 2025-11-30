# Weather-Aware Delivery System - Frontend

A professional, clean React + TypeScript frontend for the Weather-Aware Delivery System.

## Features

- Real-time weather analysis for drone deliveries
- Wind impact visualization with cost adjustments
- Alternative delivery window recommendations
- Clean, professional UI with no gradients or emojis
- Responsive design
- Type-safe API integration

## Tech Stack

- **React 18** - UI framework
- **TypeScript** - Type safety
- **Vite** - Build tool
- **Tailwind CSS** - Styling
- **Recharts** - Data visualization
- **Axios** - HTTP client

## Getting Started

### Prerequisites

- Node.js 18+ and npm

### Installation

```bash
# Install dependencies
npm install
```

### Development

```bash
# Start development server (requires backend running on port 8080)
npm run dev
```

The application will be available at http://localhost:3000

### Building for Production

```bash
# Build for production
npm run build

# Preview production build
npm run preview
```

## API Integration

The frontend connects to the Spring Boot backend via proxy configuration in `vite.config.ts`.

Default backend URL: http://localhost:8080

## Color Palette

- Primary Blue: #2563eb (blue-600)
- Secondary Slate: #64748b (slate-500)
- Success Green: #10b981 (green-500)
- Warning Amber: #f59e0b (amber-500)
- Danger Red: #ef4444 (red-500)
- Background: #f8fafc (slate-50)

## Project Structure

```
frontend/
 src/
    components/       # React components
       DeliveryForm.tsx
       WeatherResults.tsx
       WindImpactChart.tsx
       LoadingSpinner.tsx
    services/         # API service layer
       api.ts
    types/           # TypeScript type definitions
       index.ts
    App.tsx          # Main application component
    main.tsx         # Application entry point
    index.css        # Global styles
 public/              # Static assets
 index.html           # HTML template
 package.json         # Dependencies and scripts
```

## Usage

1. Enter delivery coordinates (latitude/longitude)
2. Configure delivery requirements (capacity, cooling, heating, max cost)
3. Click "Analyze Delivery" to check weather conditions and calculate path
4. View results:
   - If safe: See delivery path, cost, and wind impact analysis
   - If unsafe: See warnings and alternative delivery windows

## License

Part of the ILP coursework submission
