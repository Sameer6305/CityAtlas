/**
 * Base Chart Container
 * 
 * Provides consistent dark-theme styling for all Recharts components.
 * Centralizes common configuration to avoid repetition.
 */

'use client';

import { ReactNode } from 'react';

export interface BaseChartProps {
  children: ReactNode;
  height?: number;
}

/**
 * Dark theme color palette for charts
 * Optimized for accessibility and color-blind users
 */
export const CHART_COLORS = {
  primary: '#6366f1',      // Indigo - main brand color
  success: '#10b981',      // Green - positive metrics
  warning: '#f59e0b',      // Amber - caution
  danger: '#ef4444',       // Red - negative/alerts
  accent: '#8b5cf6',       // Purple - secondary
  info: '#3b82f6',         // Blue - informational
  teal: '#14b8a6',         // Teal - alternative
  pink: '#ec4899',         // Pink - highlights
  
  // Chart-specific gradients
  gradient1: '#6366f1',
  gradient2: '#8b5cf6',
  gradient3: '#ec4899',
  gradient4: '#f59e0b',
  gradient5: '#10b981',
  gradient6: '#14b8a6',
} as const;

/**
 * Common tooltip styling for dark theme
 */
export const TOOLTIP_STYLE = {
  contentStyle: {
    backgroundColor: 'rgba(17, 24, 39, 0.95)',
    border: '1px solid rgba(255, 255, 255, 0.1)',
    borderRadius: '8px',
    boxShadow: '0 4px 6px rgba(0, 0, 0, 0.3)',
  },
  labelStyle: { 
    color: 'rgba(255, 255, 255, 0.9)', 
    fontWeight: 'bold' 
  },
  itemStyle: { 
    color: 'rgba(255, 255, 255, 0.8)' 
  },
} as const;

/**
 * Common axis styling for dark theme
 */
export const AXIS_STYLE = {
  stroke: 'rgba(255, 255, 255, 0.5)',
  style: { fontSize: '12px' },
} as const;

/**
 * Grid styling for dark theme
 */
export const GRID_STYLE = {
  strokeDasharray: '3 3',
  stroke: 'rgba(255, 255, 255, 0.1)',
} as const;
