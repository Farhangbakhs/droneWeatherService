import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer, Cell } from 'recharts';
import type { PathSegmentWind } from '../types';

interface WindImpactChartProps {
  segments: PathSegmentWind[];
}

export default function WindImpactChart({ segments }: WindImpactChartProps) {
  // Average segments for readability if too many
  const MAX_DISPLAY_SEGMENTS = 50;

  const prepareChartData = () => {
    if (segments.length <= MAX_DISPLAY_SEGMENTS) {
      // Show all segments without averaging
      return segments.map(segment => ({
        segment: `SEG ${segment.segmentIndex + 1}`,
        costMultiplier: parseFloat((segment.costMultiplier * 100).toFixed(1)),
        windSpeed: parseFloat(segment.windSpeed.toFixed(1)),
        type: segment.windImpactType,
      }));
    }

    // Average segments into groups
    const groupSize = Math.ceil(segments.length / MAX_DISPLAY_SEGMENTS);
    const averaged = [];

    for (let i = 0; i < segments.length; i += groupSize) {
      const group = segments.slice(i, i + groupSize);
      const avgMultiplier = group.reduce((sum, s) => sum + s.costMultiplier, 0) / group.length;
      const avgWindSpeed = group.reduce((sum, s) => sum + s.windSpeed, 0) / group.length;

      // Determine dominant wind type in this group
      const typeCounts = {
        HEADWIND: group.filter(s => s.windImpactType === 'HEADWIND').length,
        TAILWIND: group.filter(s => s.windImpactType === 'TAILWIND').length,
        CROSSWIND: group.filter(s => s.windImpactType === 'CROSSWIND').length,
      };
      const dominantType = Object.entries(typeCounts).reduce((a, b) => a[1] > b[1] ? a : b)[0];

      averaged.push({
        segment: `SEG ${i + 1}-${Math.min(i + groupSize, segments.length)}`,
        costMultiplier: parseFloat((avgMultiplier * 100).toFixed(1)),
        windSpeed: parseFloat(avgWindSpeed.toFixed(1)),
        type: dominantType,
      });
    }

    return averaged;
  };

  const chartData = prepareChartData();

  const getBarColor = (type: string) => {
    switch (type) {
      case 'HEADWIND':
        return '#ff0000'; // retro red
      case 'TAILWIND':
        return '#00ff00'; // retro green
      case 'CROSSWIND':
        return '#ffff00'; // retro yellow
      default:
        return '#333366';
    }
  };

  // Calculate statistics
  const headwindCount = segments.filter(s => s.windImpactType === 'HEADWIND').length;
  const tailwindCount = segments.filter(s => s.windImpactType === 'TAILWIND').length;
  const crosswindCount = segments.filter(s => s.windImpactType === 'CROSSWIND').length;

  return (
    <div>
      <div className="mb-4 grid grid-cols-3 gap-3">
        <div className="text-center p-3 bg-retro-bg border-2 border-red-500">
          <div className="text-xs font-bold text-red-400 uppercase tracking-wider font-mono">HEADWIND</div>
          <div className="text-xl font-bold text-red-400 font-mono">{headwindCount}</div>
          <div className="text-xs text-red-400 font-mono">SEGMENTS</div>
        </div>
        <div className="text-center p-3 bg-retro-bg border-2 border-yellow-500">
          <div className="text-xs font-bold text-yellow-400 uppercase tracking-wider font-mono">CROSSWIND</div>
          <div className="text-xl font-bold text-yellow-400 font-mono">{crosswindCount}</div>
          <div className="text-xs text-yellow-400 font-mono">SEGMENTS</div>
        </div>
        <div className="text-center p-3 bg-retro-bg border-2 border-green-500">
          <div className="text-xs font-bold text-green-400 uppercase tracking-wider font-mono">TAILWIND</div>
          <div className="text-xl font-bold text-green-400 font-mono">{tailwindCount}</div>
          <div className="text-xs text-green-400 font-mono">SEGMENTS</div>
        </div>
      </div>

      <div className="text-sm text-retro-text mb-3 font-mono uppercase tracking-wider">
        &gt; COST MULTIPLIER BY SEGMENT {segments.length > MAX_DISPLAY_SEGMENTS && `(AVERAGED: ${segments.length} SEGMENTS INTO ${chartData.length} GROUPS)`}
      </div>

      <div className="border-2 border-retro-border p-2 bg-retro-bg">
        <ResponsiveContainer width="100%" height={400}>
          <BarChart data={chartData} margin={{ top: 5, right: 5, left: 5, bottom: 5 }}>
            <CartesianGrid strokeDasharray="3 3" stroke="#333366" />
            <XAxis
              dataKey="segment"
              tick={{ fill: '#cccccc', fontSize: 10, fontFamily: 'JetBrains Mono' }}
              stroke="#333366"
            />
            <YAxis
              label={{
                value: 'COST IMPACT (%)',
                angle: -90,
                position: 'insideLeft',
                style: { fill: '#cccccc', fontSize: 10, fontFamily: 'JetBrains Mono' }
              }}
              tick={{ fill: '#cccccc', fontSize: 10, fontFamily: 'JetBrains Mono' }}
              stroke="#333366"
              domain={[85, 120]}
            />
            <Tooltip
              contentStyle={{
                backgroundColor: '#10101a',
                border: '2px solid #333366',
                borderRadius: '0',
                fontSize: '11px',
                fontFamily: 'JetBrains Mono',
                color: '#cccccc'
              }}
              formatter={(value: number, name: string) => {
                if (name === 'costMultiplier') {
                  return [`${value}%`, 'COST IMPACT'];
                }
                return [value, name];
              }}
            />
            <Legend
              wrapperStyle={{ fontSize: '11px', fontFamily: 'JetBrains Mono' }}
              formatter={(value) => {
                if (value === 'costMultiplier') return 'COST IMPACT';
                return value;
              }}
            />
            <Bar dataKey="costMultiplier" name="costMultiplier">
              {chartData.map((entry, index) => (
                <Cell key={`cell-${index}`} fill={getBarColor(entry.type)} />
              ))}
            </Bar>
          </BarChart>
        </ResponsiveContainer>
      </div>

      <div className="mt-4 flex items-center justify-center gap-6 text-xs font-mono">
        <div className="flex items-center gap-2">
          <div className="w-3 h-3 bg-red-500 border border-red-600"></div>
          <span className="text-retro-text uppercase tracking-wider">HEADWIND (+COST)</span>
        </div>
        <div className="flex items-center gap-2">
          <div className="w-3 h-3 bg-yellow-500 border border-yellow-600"></div>
          <span className="text-retro-text uppercase tracking-wider">CROSSWIND (+MINOR)</span>
        </div>
        <div className="flex items-center gap-2">
          <div className="w-3 h-3 bg-green-500 border border-green-600"></div>
          <span className="text-retro-text uppercase tracking-wider">TAILWIND (-COST)</span>
        </div>
      </div>
    </div>
  );
}
