import type { Config } from "tailwindcss";

const config: Config = {
  darkMode: 'class',
  content: [
    "./src/pages/**/*.{js,ts,jsx,tsx,mdx}",
    "./src/components/**/*.{js,ts,jsx,tsx,mdx}",
    "./src/app/**/*.{js,ts,jsx,tsx,mdx}",
  ],
  theme: {
    extend: {
      colors: {
        background: '#0a0a0f',
        surface: '#141419',
        'surface-elevated': '#1a1a24',
        'surface-border': '#2a2a35',
        primary: {
          DEFAULT: '#3b82f6',
          hover: '#2563eb',
          muted: '#1e3a8a',
        },
        accent: '#8b5cf6',
        success: '#10b981',
        warning: '#f59e0b',
        danger: '#ef4444',
        text: {
          primary: '#f8fafc',
          secondary: '#94a3b8',
          tertiary: '#64748b',
          link: '#60a5fa',
        },
        chart: {
          1: '#3b82f6',
          2: '#8b5cf6',
          3: '#10b981',
          4: '#f59e0b',
          5: '#ec4899',
          6: '#06b6d4',
        },
      },
      fontFamily: {
        sans: ['Inter', 'system-ui', 'sans-serif'],
        mono: ['JetBrains Mono', 'Fira Code', 'Courier New', 'monospace'],
      },
      boxShadow: {
        sm: '0 1px 2px rgba(0, 0, 0, 0.5)',
        DEFAULT: '0 4px 6px rgba(0, 0, 0, 0.6)',
        md: '0 4px 6px rgba(0, 0, 0, 0.6)',
        lg: '0 10px 15px rgba(0, 0, 0, 0.7)',
        glow: '0 0 20px rgba(59, 130, 246, 0.3)',
      },
    },
  },
  plugins: [],
};
export default config;
