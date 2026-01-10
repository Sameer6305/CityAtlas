/**
 * TiltCard Component
 * 
 * A wrapper component that adds 3D tilt effect on hover.
 * Similar to LeetCode contest banners.
 */

'use client';

import { useTilt3D, TiltCardProps } from '@/lib/useTilt3D';

export function TiltCard({ 
  children, 
  className = '', 
  maxTilt = 12,
  scale = 1.03,
  speed = 150,
  enabled = true,
}: TiltCardProps) {
  const { ref, style, onMouseMove, onMouseEnter, onMouseLeave } = useTilt3D({
    maxTilt,
    scale,
    speed,
    enabled,
    perspective: 1000,
    zTranslate: 25,
  });

  return (
    <div
      ref={ref}
      className={`tilt-card-wrapper ${className}`}
      style={{
        ...style,
        transformStyle: 'preserve-3d',
        willChange: 'transform',
      }}
      onMouseMove={onMouseMove}
      onMouseEnter={onMouseEnter}
      onMouseLeave={onMouseLeave}
    >
      {children}
    </div>
  );
}

export default TiltCard;
