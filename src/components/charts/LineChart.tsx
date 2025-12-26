/**
 * LineChart Component
 * Reusable line chart for trends and time-series data
 */

'use client';

import { LineChart as RechartsLineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, Legend } from 'recharts';

interface LineChartProps {
  data: Array<Record<string, string | number>>;
  xKey: string;
  lines: Array<{
    dataKey: string;
    name: string;
    color: string;
    strokeWidth?: number;
  }>;
  yAxisLabel?: string;
  height?: number;
}

export function LineChart({ 
  data, 
  xKey, 
  lines, 
  yAxisLabel,
  height = 300 
}: LineChartProps) {
  return (
    <ResponsiveContainer width="100%" height={height}>
      <RechartsLineChart
        data={data}
        margin={{ top: 5, right: 30, left: 20, bottom: 5 }}
      >
        <CartesianGrid strokeDasharray="3 3" stroke="rgba(255, 255, 255, 0.1)" />
        <XAxis 
          dataKey={xKey}
          stroke="rgba(255, 255, 255, 0.5)"
          style={{ fontSize: '12px' }}
        />
        <YAxis 
          stroke="rgba(255, 255, 255, 0.5)"
          style={{ fontSize: '12px' }}
          label={yAxisLabel ? { value: yAxisLabel, angle: -90, position: 'insideLeft', style: { fill: 'rgba(255, 255, 255, 0.7)' } } : undefined}
        />
        <Tooltip
          contentStyle={{
            backgroundColor: 'rgba(17, 24, 39, 0.95)',
            border: '1px solid rgba(255, 255, 255, 0.1)',
            borderRadius: '8px',
            boxShadow: '0 4px 6px rgba(0, 0, 0, 0.3)',
          }}
          labelStyle={{ color: 'rgba(255, 255, 255, 0.9)', fontWeight: 'bold' }}
          itemStyle={{ color: 'rgba(255, 255, 255, 0.8)' }}
        />
        <Legend 
          wrapperStyle={{ paddingTop: '20px' }}
          iconType="line"
        />
        {lines.map((line) => (
          <Line
            key={line.dataKey}
            type="monotone"
            dataKey={line.dataKey}
            name={line.name}
            stroke={line.color}
            strokeWidth={line.strokeWidth || 2}
            dot={{ r: 4, fill: line.color }}
            activeDot={{ r: 6 }}
            animationDuration={1000}
            animationEasing="ease-in-out"
          />
        ))}
      </RechartsLineChart>
    </ResponsiveContainer>
  );
}
