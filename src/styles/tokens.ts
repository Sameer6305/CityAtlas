/**
 * Design Tokens for CityAtlas
 * TypeScript constants for use in React components
 * Mirrors the CSS variables in theme.css for type-safe usage
 */

// ========================================
// COLOR PALETTE
// ========================================

export const colors = {
  // Background colors
  background: '#0a0a0f',
  surface: '#141419',
  surfaceElevated: '#1a1a24',
  surfaceBorder: '#2a2a35',

  // Primary colors
  primary: {
    DEFAULT: '#3b82f6',
    hover: '#2563eb',
    muted: '#1e3a8a',
    light: '#60a5fa',
  },

  // Accent colors
  accent: {
    DEFAULT: '#8b5cf6',
    hover: '#7c3aed',
  },

  // Semantic colors
  success: {
    DEFAULT: '#10b981',
    bg: '#064e3b',
  },
  warning: {
    DEFAULT: '#f59e0b',
    bg: '#78350f',
  },
  danger: {
    DEFAULT: '#ef4444',
    bg: '#7f1d1d',
  },
  info: {
    DEFAULT: '#06b6d4',
    bg: '#164e63',
  },

  // Text colors
  text: {
    primary: '#f8fafc',
    secondary: '#94a3b8',
    tertiary: '#64748b',
    link: '#60a5fa',
    disabled: '#475569',
  },

  // Chart colors
  chart: {
    1: '#3b82f6',
    2: '#8b5cf6',
    3: '#10b981',
    4: '#f59e0b',
    5: '#ec4899',
    6: '#06b6d4',
  },
} as const;

// ========================================
// TYPOGRAPHY
// ========================================

export const typography = {
  fontFamily: {
    sans: "'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif",
    mono: "'JetBrains Mono', 'Fira Code', 'Courier New', monospace",
  },

  fontSize: {
    xs: '0.75rem',     // 12px
    sm: '0.875rem',    // 14px
    base: '1rem',      // 16px
    lg: '1.125rem',    // 18px
    xl: '1.25rem',     // 20px
    '2xl': '1.5rem',   // 24px
    '3xl': '2rem',     // 32px
    '4xl': '2.5rem',   // 40px
  },

  fontWeight: {
    normal: 400,
    medium: 500,
    semibold: 600,
    bold: 700,
  },

  lineHeight: {
    tight: 1.25,
    snug: 1.375,
    normal: 1.5,
    relaxed: 1.625,
    loose: 2,
  },

  letterSpacing: {
    tight: '-0.025em',
    normal: '0',
    wide: '0.025em',
  },
} as const;

// ========================================
// SPACING
// ========================================

export const spacing = {
  0: '0',
  1: '0.25rem',    // 4px
  2: '0.5rem',     // 8px
  3: '0.75rem',    // 12px
  4: '1rem',       // 16px
  5: '1.25rem',    // 20px
  6: '1.5rem',     // 24px
  8: '2rem',       // 32px
  10: '2.5rem',    // 40px
  12: '3rem',      // 48px
  16: '4rem',      // 64px
  20: '5rem',      // 80px
  24: '6rem',      // 96px
} as const;

// ========================================
// BORDER RADIUS
// ========================================

export const borderRadius = {
  none: '0',
  sm: '0.375rem',   // 6px
  md: '0.5rem',     // 8px
  lg: '0.75rem',    // 12px
  xl: '1rem',       // 16px
  full: '9999px',
} as const;

// ========================================
// SHADOWS
// ========================================

export const shadows = {
  xs: '0 1px 2px rgba(0, 0, 0, 0.5)',
  sm: '0 2px 4px rgba(0, 0, 0, 0.5)',
  md: '0 4px 6px rgba(0, 0, 0, 0.6)',
  lg: '0 10px 15px rgba(0, 0, 0, 0.7)',
  xl: '0 20px 25px rgba(0, 0, 0, 0.8)',
  '2xl': '0 25px 50px rgba(0, 0, 0, 0.85)',
  glow: {
    primary: '0 0 20px rgba(59, 130, 246, 0.3)',
    accent: '0 0 20px rgba(139, 92, 246, 0.3)',
    success: '0 0 20px rgba(16, 185, 129, 0.3)',
  },
} as const;

// ========================================
// TRANSITIONS
// ========================================

export const transitions = {
  duration: {
    fast: '150ms',
    base: '200ms',
    slow: '300ms',
    slower: '500ms',
  },
  easing: {
    linear: 'linear',
    in: 'cubic-bezier(0.4, 0, 1, 1)',
    out: 'cubic-bezier(0, 0, 0.2, 1)',
    inOut: 'cubic-bezier(0.4, 0, 0.2, 1)',
    bounce: 'cubic-bezier(0.68, -0.55, 0.265, 1.55)',
  },
} as const;

// ========================================
// Z-INDEX
// ========================================

export const zIndex = {
  base: 0,
  dropdown: 1000,
  sticky: 1100,
  fixed: 1200,
  modalBackdrop: 1300,
  modal: 1400,
  popover: 1500,
  tooltip: 1600,
} as const;

// ========================================
// BREAKPOINTS
// ========================================

export const breakpoints = {
  sm: '640px',
  md: '768px',
  lg: '1024px',
  xl: '1280px',
  '2xl': '1536px',
} as const;

// Numeric values for use in JS media queries
export const breakpointValues = {
  sm: 640,
  md: 768,
  lg: 1024,
  xl: 1280,
  '2xl': 1536,
} as const;

// ========================================
// CONTAINER WIDTHS
// ========================================

export const containerWidths = {
  xs: '640px',
  sm: '768px',
  md: '1024px',
  lg: '1280px',
  xl: '1536px',
} as const;

// ========================================
// UTILITY FUNCTIONS
// ========================================

/**
 * Get a color value by path
 * @example getColor('primary', 'hover') => '#2563eb'
 */
export function getColor(category: keyof typeof colors, variant?: string): string {
  const colorCategory = colors[category];
  if (typeof colorCategory === 'string') {
    return colorCategory;
  }
  if (variant && typeof colorCategory === 'object') {
    return (colorCategory as Record<string, string>)[variant] || (colorCategory as Record<string, string>).DEFAULT;
  }
  return (colorCategory as Record<string, string>).DEFAULT || '';
}

/**
 * Get spacing value
 * @example getSpacing(4) => '1rem'
 */
export function getSpacing(value: keyof typeof spacing): string {
  return spacing[value];
}

/**
 * Get font size value
 * @example getFontSize('lg') => '1.125rem'
 */
export function getFontSize(size: keyof typeof typography.fontSize): string {
  return typography.fontSize[size];
}

/**
 * Create a transition string
 * @example createTransition('all', 'base', 'out') => 'all 200ms cubic-bezier(0, 0, 0.2, 1)'
 */
export function createTransition(
  property: string = 'all',
  duration: keyof typeof transitions.duration = 'base',
  easing: keyof typeof transitions.easing = 'out'
): string {
  return `${property} ${transitions.duration[duration]} ${transitions.easing[easing]}`;
}

/**
 * Check if viewport matches a breakpoint
 * @example matchesBreakpoint('md') => boolean
 */
export function matchesBreakpoint(breakpoint: keyof typeof breakpointValues): boolean {
  if (typeof window === 'undefined') return false;
  return window.innerWidth >= breakpointValues[breakpoint];
}

// ========================================
// TYPE EXPORTS
// ========================================

export type ColorCategory = keyof typeof colors;
export type SpacingValue = keyof typeof spacing;
export type FontSize = keyof typeof typography.fontSize;
export type FontWeight = keyof typeof typography.fontWeight;
export type BorderRadius = keyof typeof borderRadius;
export type Shadow = keyof typeof shadows;
export type Breakpoint = keyof typeof breakpoints;
export type ZIndex = keyof typeof zIndex;
