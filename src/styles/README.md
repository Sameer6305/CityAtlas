# CityAtlas Design System

**Dark-first analytics dashboard design system**

---

## 📁 Structure

```
src/styles/
├── theme.css    → CSS variables & component presets
├── tokens.ts    → TypeScript constants for React components
└── README.md    → This file
```

---

## 🎨 Usage

### In CSS/Tailwind
Use CSS variables directly:

```css
.my-component {
  background: var(--color-surface);
  color: var(--color-text-primary);
  padding: var(--space-6);
  border-radius: var(--radius-md);
}
```

### In React Components
Import TypeScript tokens:

```tsx
import { colors, spacing, typography } from '@/styles/tokens';

const MyComponent = () => (
  <div style={{ 
    color: colors.text.primary,
    padding: spacing[6],
    fontSize: typography.fontSize.lg 
  }}>
    Content
  </div>
);
```

### With Utility Classes
Use predefined component classes:

```tsx
<div className="card">
  <h2 className="text-primary">Title</h2>
  <p className="text-secondary">Description</p>
</div>
```

---

## 🎯 Design Principles

### 1. **Dark Mode First**
- Deep blacks (#0a0a0f) reduce eye strain
- High contrast for data-heavy interfaces
- WCAG 2.1 AA compliant

### 2. **Color Semantics**
- **Blue** → Actions, CTAs, primary brand
- **Green** → Positive metrics, success states
- **Red** → Negative metrics, errors
- **Amber** → Warnings, caution
- **Purple** → Accents, highlights

### 3. **4px Grid System**
All spacing uses multiples of 4px for consistency

### 4. **Type Hierarchy**
- `text-primary` (#f8fafc) → Headings, important text
- `text-secondary` (#94a3b8) → Body text
- `text-tertiary` (#64748b) → Labels, captions

---

## 🧩 Component Presets

### Card
```html
<div class="card">
  Standard card with hover effect
</div>

<div class="card card-elevated">
  Elevated card with shadow
</div>
```

### Metric Card
```html
<div class="card-metric">
  <div class="card-metric-label">Population</div>
  <div class="card-metric-value">815,201</div>
  <div class="card-metric-change positive">↑ +2.3%</div>
</div>
```

### Status Badges
```html
<span class="badge badge-success">Active</span>
<span class="badge badge-warning">Pending</span>
<span class="badge badge-danger">Error</span>
<span class="badge badge-info">Info</span>
```

---

## ♿ Accessibility

### Focus States
All interactive elements have visible focus rings:
```css
*:focus-visible {
  outline: 2px solid var(--color-primary);
  box-shadow: var(--shadow-glow-primary);
}
```

### Reduced Motion
Respects user preferences:
```css
@media (prefers-reduced-motion: reduce) {
  /* Animations disabled */
}
```

### High Contrast
Adjusts for users needing higher contrast:
```css
@media (prefers-contrast: high) {
  /* Enhanced contrast */
}
```

### Contrast Ratios (WCAG AA)
- White on background: **17:1** ✅
- Secondary text: **7.5:1** ✅
- Primary blue: **8.2:1** ✅

---

## 📊 Chart Colors

Use for data visualization (high contrast):

```ts
import { colors } from '@/styles/tokens';

const chartColors = [
  colors.chart[1], // Blue
  colors.chart[2], // Purple
  colors.chart[3], // Green
  colors.chart[4], // Amber
  colors.chart[5], // Pink
  colors.chart[6], // Cyan
];
```

---

## 🔧 Utility Functions

### getColor
```ts
import { getColor } from '@/styles/tokens';

const primaryHover = getColor('primary', 'hover'); // '#2563eb'
```

### createTransition
```ts
import { createTransition } from '@/styles/tokens';

const transition = createTransition('all', 'base', 'out');
// → 'all 200ms cubic-bezier(0, 0, 0.2, 1)'
```

### matchesBreakpoint
```ts
import { matchesBreakpoint } from '@/styles/tokens';

if (matchesBreakpoint('md')) {
  // Screen is >= 768px
}
```

---

## 📐 Layout System

### Container Widths
- `xs`: 640px
- `sm`: 768px
- `md`: 1024px
- `lg`: 1280px
- `xl`: 1536px

### Grid
```html
<div class="grid-container grid-cols-3">
  <div>Column 1</div>
  <div>Column 2</div>
  <div>Column 3</div>
</div>
```

---

## 🎭 Interactive States

### Hover
Cards lift on hover with shadow and border color change

### Active
Buttons and tabs show darker background when active

### Disabled
Reduced opacity with disabled cursor

### Loading
Skeleton screens use animated gradient shimmer

---

## 🚀 Next Steps

Now that the design system is ready:
1. ✅ Build UI components (buttons, inputs, tabs)
2. ✅ Create layout components (navigation, sidebar)
3. ✅ Implement data visualization components (charts)
4. ✅ Build city profile pages

---

**This design system ensures CityAtlas feels like a premium SaaS product.**
