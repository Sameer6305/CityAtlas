/**
 * AreaChart Component
 * Reusable area chart for filled trend visualizations
 */

'use client';

import { AreaChart as RechartsAreaChart, Area, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, Legend } from 'recharts';

interface AreaChartProps {
  data: Array<Record<string, string | number>>;
  xKey: string;
  areas: Array<{
    dataKey: string;
    name: string;
    color: string;
    fillOpacity?: number;
  }>;
  yAxisLabel?: string;
  height?: number;
}

export function AreaChart({ 
  data, 
  xKey, 
  areas, 
  yAxisLabel,
  height = 300 
}: AreaChartProps) {
  return (
    <ResponsiveContainer width="100%" height={height}>
      <RechartsAreaChart
        data={data}
        margin={{ top: 5, right: 30, left: 20, bottom: 5 }}
      >
        <defs>
          {areas.map((area) => (
            <linearGradient key={area.dataKey} id={`gradient-${area.dataKey}`} x1="0" y1="0" x2="0" y2="1">
              <stop offset="5%" stopColor={area.color} stopOpacity={area.fillOpacity || 0.8}/>
              <stop offset="95%" stopColor={area.color} stopOpacity={0.1}/>
            </linearGradient>
          ))}
        </defs>
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
        />
        {areas.map((area) => (
          <Area
            key={area.dataKey}
            type="monotone"
            dataKey={area.dataKey}
            name={area.name}
            stroke={area.color}
            strokeWidth={2}
            fill={`url(#gradient-${area.dataKey})`}
            animationDuration={1000}
            animationEasing="ease-in-out"
          />
        ))}
      </RechartsAreaChart>
    </ResponsiveContainer>
  );
}
