/**
 * PieChart Component
 * Reusable pie chart for proportion visualizations
 */

'use client';

import { PieChart as RechartsPieChart, Pie, Cell, Tooltip, ResponsiveContainer, Legend, PieLabelRenderProps } from 'recharts';

interface PieChartProps {
  data: Array<{
    name: string;
    value: number;
  }>;
  colors: string[];
  height?: number;
  innerRadius?: number;
  showLabels?: boolean;
}

export function PieChart({ 
  data, 
  colors,
  height = 300,
  innerRadius = 0,
  showLabels = true
}: PieChartProps) {
  const renderLabel = (props: PieLabelRenderProps) => {
    return `${props.name}: ${props.value}%`;
  };

  return (
    <ResponsiveContainer width="100%" height={height}>
      <RechartsPieChart>
        <Pie
          data={data}
          cx="50%"
          cy="50%"
          labelLine={showLabels}
          label={showLabels ? renderLabel : false}
          outerRadius={100}
          innerRadius={innerRadius}
          fill="#8884d8"
          dataKey="value"
          animationDuration={1000}
          animationEasing="ease-in-out"
        >
          {data.map((entry, index) => (
            <Cell key={`cell-${index}`} fill={colors[index % colors.length]} />
          ))}
        </Pie>
        <Tooltip
          contentStyle={{
            backgroundColor: 'rgba(17, 24, 39, 0.95)',
            border: '1px solid rgba(255, 255, 255, 0.1)',
            borderRadius: '8px',
            boxShadow: '0 4px 6px rgba(0, 0, 0, 0.3)',
          }}
          itemStyle={{ color: 'rgba(255, 255, 255, 0.8)' }}
        />
        <Legend 
          verticalAlign="bottom" 
          height={36}
          iconType="circle"
        />
      </RechartsPieChart>
    </ResponsiveContainer>
  );
}
