/**
 * BarChart Component
 * Reusable bar chart for comparisons and distributions
 */

'use client';

import { BarChart as RechartsBarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, Legend, Cell } from 'recharts';

interface BarChartProps {
  data: Array<Record<string, string | number>>;
  xKey: string;
  bars: Array<{
    dataKey: string;
    name: string;
    color: string;
  }>;
  yAxisLabel?: string;
  height?: number;
  layout?: 'horizontal' | 'vertical';
  customColors?: string[];
}

export function BarChart({ 
  data, 
  xKey, 
  bars, 
  yAxisLabel,
  height = 300,
  layout = 'horizontal',
  customColors
}: BarChartProps) {
  return (
    <ResponsiveContainer width="100%" height={height}>
      <RechartsBarChart
        data={data}
        layout={layout}
        margin={{ top: 5, right: 30, left: 20, bottom: 5 }}
      >
        <CartesianGrid strokeDasharray="3 3" stroke="rgba(255, 255, 255, 0.1)" />
        {layout === 'horizontal' ? (
          <>
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
          </>
        ) : (
          <>
            <XAxis 
              type="number"
              stroke="rgba(255, 255, 255, 0.5)"
              style={{ fontSize: '12px' }}
            />
            <YAxis 
              type="category"
              dataKey={xKey}
              stroke="rgba(255, 255, 255, 0.5)"
              style={{ fontSize: '12px' }}
            />
          </>
        )}
        <Tooltip
          contentStyle={{
            backgroundColor: 'rgba(17, 24, 39, 0.95)',
            border: '1px solid rgba(255, 255, 255, 0.1)',
            borderRadius: '8px',
            boxShadow: '0 4px 6px rgba(0, 0, 0, 0.3)',
          }}
          labelStyle={{ color: 'rgba(255, 255, 255, 0.9)', fontWeight: 'bold' }}
          itemStyle={{ color: 'rgba(255, 255, 255, 0.8)' }}
          cursor={{ fill: 'rgba(255, 255, 255, 0.05)' }}
        />
        {bars.length > 1 && (
          <Legend 
            wrapperStyle={{ paddingTop: '20px' }}
          />
        )}
        {bars.map((bar) => (
          <Bar
            key={bar.dataKey}
            dataKey={bar.dataKey}
            name={bar.name}
            fill={bar.color}
            radius={[8, 8, 0, 0]}
            animationDuration={1000}
            animationEasing="ease-in-out"
          >
            {customColors && customColors.map((color, index) => (
              <Cell key={`cell-${index}`} fill={color} />
            ))}
          </Bar>
        ))}
      </RechartsBarChart>
    </ResponsiveContainer>
  );
}
