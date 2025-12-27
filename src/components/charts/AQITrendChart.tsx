/**
 * AQI Trend Chart Component
 * 
 * Displays air quality index trends over time.
 * Reusable component that accepts data via props.
 */

'use client';

import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, ReferenceLine } from 'recharts';
import { AQIDataPoint } from '@/lib/mock/aqi';
import { CHART_COLORS, TOOLTIP_STYLE, AXIS_STYLE, GRID_STYLE } from './BaseChartContainer';

interface AQITrendChartProps {
  data: AQIDataPoint[];
  height?: number;
  showBenchmark?: boolean;
}

export function AQITrendChart({ 
  data, 
  height = 280,
  showBenchmark = true 
}: AQITrendChartProps) {
  return (
    <ResponsiveContainer width="100%" height={height}>
      <LineChart
        data={data}
        margin={{ top: 5, right: 30, left: 20, bottom: 5 }}
      >
        <CartesianGrid {...GRID_STYLE} />
        <XAxis 
          dataKey="month"
          {...AXIS_STYLE}
        />
        <YAxis 
          {...AXIS_STYLE}
          label={{ 
            value: 'AQI Value', 
            angle: -90, 
            position: 'insideLeft',
            style: { fill: 'rgba(255, 255, 255, 0.7)' }
          }}
        />
        <Tooltip {...TOOLTIP_STYLE} />
        
        {/* Good air quality benchmark line */}
        {showBenchmark && (
          <ReferenceLine 
            y={50} 
            stroke={CHART_COLORS.info}
            strokeDasharray="3 3"
            label={{ 
              value: 'Good (≤50)', 
              position: 'right',
              fill: CHART_COLORS.info
            }}
          />
        )}
        
        <Line
          type="monotone"
          dataKey="aqi"
          name="AQI"
          stroke={CHART_COLORS.success}
          strokeWidth={3}
          dot={{ r: 4, fill: CHART_COLORS.success }}
          activeDot={{ r: 6 }}
          animationDuration={1000}
          animationEasing="ease-in-out"
        />
      </LineChart>
    </ResponsiveContainer>
  );
}
