/**
 * ChartCard Component
 * 
 * Wrapper for chart visualizations with title, description, and actions
 * Provides consistent styling for all analytics charts
 */

'use client';

import { ReactNode } from 'react';

interface ChartCardProps {
  title: string;
  description?: string;
  children: ReactNode;
  actions?: ReactNode;
  height?: number | string;
  loading?: boolean;
  error?: string;
  footer?: ReactNode;
}

export function ChartCard({
  title,
  description,
  children,
  actions,
  height = 300,
  loading = false,
  error,
  footer,
}: ChartCardProps) {
  const heightStyle = typeof height === 'number' ? `${height}px` : height;

  return (
    <div className="glass-card p-5">
      {/* Header */}
      <div className="flex items-start justify-between mb-4">
        <div className="flex-1">
          <h3 className="text-lg font-semibold text-white mb-1">
            {title}
          </h3>
          {description && (
            <p className="text-sm text-white/60">
              {description}
            </p>
          )}
        </div>
        {actions && (
          <div className="flex items-center gap-2">
            {actions}
          </div>
        )}
      </div>

      {/* Chart Area */}
      <div 
        className="relative bg-white/5 rounded-lg overflow-hidden border border-white/10"
        style={{ height: heightStyle }}
      >
        {loading && (
          <div className="absolute inset-0 flex items-center justify-center">
            <div className="flex flex-col items-center gap-3">
              <div className="w-12 h-12 border-4 border-cyan-500 border-t-transparent rounded-full animate-spin" />
              <p className="text-sm text-white/60">Loading data...</p>
            </div>
          </div>
        )}

        {error && !loading && (
          <div className="absolute inset-0 flex items-center justify-center">
            <div className="text-center max-w-sm px-4">
              <div className="text-4xl mb-3">⚠️</div>
              <p className="text-sm text-red-400 font-medium mb-1">Error loading chart</p>
              <p className="text-xs text-white/50">{error}</p>
            </div>
          </div>
        )}

        {!loading && !error && (
          <div className="w-full h-full p-4">
            {children}
          </div>
        )}
      </div>

      {/* Footer */}
      {footer && (
        <div className="mt-4 pt-4 border-t border-white/10">
          {footer}
        </div>
      )}
    </div>
  );
}

/**
 * ChartCardGrid Component
 * 
 * Responsive grid for multiple chart cards
 */
interface ChartCardGridProps {
  children: ReactNode;
  columns?: 1 | 2;
}

export function ChartCardGrid({ children, columns = 2 }: ChartCardGridProps) {
  const gridCols = columns === 1 
    ? 'grid-cols-1' 
    : 'grid-cols-1 lg:grid-cols-2';

  return (
    <div className={`grid ${gridCols} gap-5`}>
      {children}
    </div>
  );
}

/**
 * ChartPlaceholder Component
 * 
 * Placeholder content for charts when no data or implementation pending
 */
export function ChartPlaceholder({ message = 'Chart will render here' }: { message?: string }) {
  return (
    <div className="w-full h-full flex items-center justify-center">
      <div className="text-center">
        <div className="text-6xl mb-4 opacity-20">📊</div>
        <p className="text-white/50 text-sm">{message}</p>
      </div>
    </div>
  );
}
