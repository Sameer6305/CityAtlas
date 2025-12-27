/**
 * Job Sector Distribution Chart Component
 * 
 * Displays employment distribution across sectors.
 * Uses horizontal bar chart for better label readability.
 */

'use client';

import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, Cell } from 'recharts';
import { JobSectorData } from '@/lib/mock/jobs';
import { CHART_COLORS, TOOLTIP_STYLE, AXIS_STYLE, GRID_STYLE } from './BaseChartContainer';

interface JobSectorChartProps {
  data: JobSectorData[];
  height?: number;
}

// Color mapping for different sectors
const SECTOR_COLORS = [
  CHART_COLORS.gradient1,
  CHART_COLORS.gradient2,
  CHART_COLORS.gradient3,
  CHART_COLORS.gradient4,
  CHART_COLORS.gradient5,
  CHART_COLORS.gradient6,
];

export function JobSectorChart({ 
  data, 
  height = 320 
}: JobSectorChartProps) {
  return (
    <ResponsiveContainer width="100%" height={height}>
      <BarChart
        data={data}
        layout="horizontal"
        margin={{ top: 5, right: 30, left: 20, bottom: 5 }}
      >
        <CartesianGrid {...GRID_STYLE} />
        <XAxis 
          type="number"
          {...AXIS_STYLE}
          label={{ 
            value: 'Employees (thousands)', 
            position: 'insideBottom',
            offset: -5,
            style: { fill: 'rgba(255, 255, 255, 0.7)' }
          }}
          tickFormatter={(value) => `${(value / 1000).toFixed(0)}k`}
        />
        <YAxis 
          type="category"
          dataKey="sector"
          {...AXIS_STYLE}
          width={100}
        />
        <Tooltip 
          {...TOOLTIP_STYLE}
          formatter={(value: number | undefined, name: string | undefined) => {
            if (!value) return [0, name || ''];
            if (name === 'employees') {
              return [`${(value / 1000).toFixed(1)}k employees`, 'Count'];
            }
            return [value, name || ''];
          }}
        />
        <Bar
          dataKey="employees"
          name="employees"
          radius={[0, 8, 8, 0]}
          animationDuration={1000}
          animationEasing="ease-in-out"
        >
          {data.map((entry, index) => (
            <Cell 
              key={`cell-${index}`} 
              fill={SECTOR_COLORS[index % SECTOR_COLORS.length]} 
            />
          ))}
        </Bar>
      </BarChart>
    </ResponsiveContainer>
  );
}
