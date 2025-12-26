/**
 * MetricCard Component
 * 
 * Displays KPI metrics with value, label, change indicator, and optional trend
 * Used throughout city profile pages for analytics
 */

'use client';

import { ReactNode } from 'react';

interface MetricCardProps {
  label: string;
  value: string | number;
  unit?: string;
  change?: {
    value: number;
    period: string; // e.g., "vs. last month", "YoY"
  };
  trend?: 'up' | 'down' | 'neutral';
  icon?: string | ReactNode;
  status?: 'good' | 'warning' | 'danger' | 'neutral';
  subtitle?: string;
  loading?: boolean;
}

export function MetricCard({
  label,
  value,
  unit,
  change,
  trend,
  icon,
  status = 'neutral',
  subtitle,
  loading = false,
}: MetricCardProps) {
  // Determine status color
  const getStatusColor = () => {
    switch (status) {
      case 'good':
        return 'text-success';
      case 'warning':
        return 'text-warning';
      case 'danger':
        return 'text-danger';
      default:
        return 'text-text-primary';
    }
  };

  // Determine change color
  const getChangeColor = () => {
    if (!change) return '';
    
    if (trend === 'up') return 'text-success';
    if (trend === 'down') return 'text-danger';
    return 'text-text-secondary';
  };

  // Get trend arrow
  const getTrendArrow = () => {
    if (trend === 'up') return '↑';
    if (trend === 'down') return '↓';
    return '→';
  };

  if (loading) {
    return (
      <div className="card p-6 animate-pulse">
        <div className="h-4 bg-surface-elevated rounded w-1/2 mb-3" />
        <div className="h-8 bg-surface-elevated rounded w-3/4 mb-2" />
        <div className="h-3 bg-surface-elevated rounded w-1/3" />
      </div>
    );
  }

  return (
    <div className="card p-6 hover:scale-[1.02] transition-transform">
      {/* Header with icon */}
      <div className="flex items-start justify-between mb-3">
        <div className="flex-1">
          <p className="text-sm text-text-tertiary font-medium">
            {label}
          </p>
          {subtitle && (
            <p className="text-xs text-text-tertiary mt-1">
              {subtitle}
            </p>
          )}
        </div>
        {icon && (
          <div className="text-2xl opacity-60">
            {typeof icon === 'string' ? icon : icon}
          </div>
        )}
      </div>

      {/* Value */}
      <div className="flex items-baseline gap-2 mb-2">
        <span className={`text-3xl font-bold ${getStatusColor()}`}>
          {value}
        </span>
        {unit && (
          <span className="text-sm text-text-tertiary">
            {unit}
          </span>
        )}
      </div>

      {/* Change Indicator */}
      {change && (
        <div className={`flex items-center gap-1 text-sm font-medium ${getChangeColor()}`}>
          <span>{getTrendArrow()}</span>
          <span>
            {change.value > 0 ? '+' : ''}{change.value}%
          </span>
          <span className="text-xs text-text-tertiary font-normal">
            {change.period}
          </span>
        </div>
      )}

      {/* Mini trend line (visual indicator) */}
      {trend && (
        <div className="mt-3 h-1 bg-surface-elevated rounded-full overflow-hidden">
          <div 
            className={`h-full rounded-full ${
              trend === 'up' ? 'bg-success' : 
              trend === 'down' ? 'bg-danger' : 
              'bg-text-tertiary'
            }`}
            style={{ width: '60%' }}
          />
        </div>
      )}
    </div>
  );
}

/**
 * MetricCardGrid Component
 * 
 * Responsive grid layout for multiple metric cards
 */
interface MetricCardGridProps {
  children: ReactNode;
  columns?: 2 | 3 | 4;
}

export function MetricCardGrid({ children, columns = 4 }: MetricCardGridProps) {
  const gridCols = {
    2: 'grid-cols-1 md:grid-cols-2',
    3: 'grid-cols-1 md:grid-cols-2 lg:grid-cols-3',
    4: 'grid-cols-1 md:grid-cols-2 lg:grid-cols-4',
  };

  return (
    <div className={`grid ${gridCols[columns]} gap-6`}>
      {children}
    </div>
  );
}
