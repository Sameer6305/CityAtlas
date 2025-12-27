/**
 * Cost of Living Chart Component
 * 
 * Displays cost of living index by category.
 * Vertical bar chart with national average reference line.
 */

'use client';

import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, ReferenceLine, Cell } from 'recharts';
import { CostOfLivingData } from '@/lib/mock/costOfLiving';
import { CHART_COLORS, TOOLTIP_STYLE, AXIS_STYLE, GRID_STYLE } from './BaseChartContainer';

interface CostOfLivingChartProps {
  data: CostOfLivingData[];
  height?: number;
  showNationalAverage?: boolean;
}

// Color mapping based on index value (higher = more expensive = warmer color)
const getCategoryColor = (index: number): string => {
  if (index >= 180) return CHART_COLORS.danger;
  if (index >= 150) return CHART_COLORS.warning;
  if (index >= 120) return CHART_COLORS.accent;
  if (index >= 100) return CHART_COLORS.info;
  return CHART_COLORS.success;
};

export function CostOfLivingChart({ 
  data, 
  height = 300,
  showNationalAverage = true
}: CostOfLivingChartProps) {
  return (
    <ResponsiveContainer width="100%" height={height}>
      <BarChart
        data={data}
        margin={{ top: 5, right: 30, left: 20, bottom: 5 }}
      >
        <CartesianGrid {...GRID_STYLE} />
        <XAxis 
          dataKey="category"
          {...AXIS_STYLE}
          angle={-45}
          textAnchor="end"
          height={80}
        />
        <YAxis 
          {...AXIS_STYLE}
          label={{ 
            value: 'Index (National Avg = 100)', 
            angle: -90, 
            position: 'insideLeft',
            style: { fill: 'rgba(255, 255, 255, 0.7)' }
          }}
        />
        <Tooltip 
          {...TOOLTIP_STYLE}
          // eslint-disable-next-line @typescript-eslint/no-explicit-any
          formatter={(value: number | undefined, name: string | undefined, props: any) => {
            if (!value) return [0, name || ''];
            if (name === 'index' && props?.payload) {
              const payload = props.payload as CostOfLivingData;
              const monthlyAvg = payload.monthlyAvg;
              return [
                `Index: ${value} | $${monthlyAvg}/mo`,
                'Cost Index'
              ];
            }
            return [value, name || ''];
          }}
        />
        
        {/* National average reference line */}
        {showNationalAverage && (
          <ReferenceLine 
            y={100} 
            stroke={CHART_COLORS.info}
            strokeDasharray="3 3"
            label={{ 
              value: 'National Avg', 
              position: 'right',
              fill: CHART_COLORS.info
            }}
          />
        )}
        
        <Bar
          dataKey="index"
          name="index"
          radius={[8, 8, 0, 0]}
          animationDuration={1000}
          animationEasing="ease-in-out"
        >
          {data.map((entry, index) => (
            <Cell 
              key={`cell-${index}`} 
              fill={getCategoryColor(entry.index)} 
            />
          ))}
        </Bar>
      </BarChart>
    </ResponsiveContainer>
  );
}
