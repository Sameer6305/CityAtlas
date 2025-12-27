/**
 * Population Metrics Chart Component
 * 
 * Displays population trends over time using area chart.
 * Shows growth trajectory with gradient fill.
 */

'use client';

import { AreaChart, Area, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts';
import { PopulationDataPoint } from '@/lib/mock/population';
import { CHART_COLORS, TOOLTIP_STYLE, AXIS_STYLE, GRID_STYLE } from './BaseChartContainer';

interface PopulationChartProps {
  data: PopulationDataPoint[];
  height?: number;
}

export function PopulationChart({ 
  data, 
  height = 280 
}: PopulationChartProps) {
  return (
    <ResponsiveContainer width="100%" height={height}>
      <AreaChart
        data={data}
        margin={{ top: 5, right: 30, left: 20, bottom: 5 }}
      >
        <defs>
          <linearGradient id="populationGradient" x1="0" y1="0" x2="0" y2="1">
            <stop offset="5%" stopColor={CHART_COLORS.primary} stopOpacity={0.8}/>
            <stop offset="95%" stopColor={CHART_COLORS.primary} stopOpacity={0.1}/>
          </linearGradient>
        </defs>
        <CartesianGrid {...GRID_STYLE} />
        <XAxis 
          dataKey="year"
          {...AXIS_STYLE}
        />
        <YAxis 
          {...AXIS_STYLE}
          label={{ 
            value: 'Population (Millions)', 
            angle: -90, 
            position: 'insideLeft',
            style: { fill: 'rgba(255, 255, 255, 0.7)' }
          }}
          domain={['dataMin - 0.5', 'dataMax + 0.5']}
        />
        <Tooltip 
          {...TOOLTIP_STYLE}
          // eslint-disable-next-line @typescript-eslint/no-explicit-any
          formatter={(value: number | undefined, name: string | undefined, props: any) => {
            if (!value) return [0, name || ''];
            if (name === 'population' && props?.payload) {
              const payload = props.payload as PopulationDataPoint;
              const growthRate = payload.growthRate;
              return [
                `${value}M (${growthRate > 0 ? '+' : ''}${growthRate}%)`,
                'Population'
              ];
            }
            return [value, name || ''];
          }}
        />
        <Area
          type="monotone"
          dataKey="population"
          name="population"
          stroke={CHART_COLORS.primary}
          strokeWidth={2}
          fill="url(#populationGradient)"
          animationDuration={1000}
          animationEasing="ease-in-out"
        />
      </AreaChart>
    </ResponsiveContainer>
  );
}
