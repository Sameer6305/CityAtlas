/**
 * useTilt3D Hook
 * 
 * Creates a smooth 3D tilt effect on hover, similar to LeetCode contest banners.
 * The card tilts towards the cursor position with configurable intensity.
 */

import { useRef, useState, useCallback, useEffect } from 'react';

interface TiltStyle {
  transform: string;
  transition: string;
}

interface UseTilt3DOptions {
  /** Maximum tilt angle in degrees (default: 15) */
  maxTilt?: number;
  /** Scale on hover (default: 1.02) */
  scale?: number;
  /** Transition speed in ms (default: 150) */
  speed?: number;
  /** Enable/disable the effect (default: true) */
  enabled?: boolean;
  /** Perspective distance in px (default: 1000) */
  perspective?: number;
  /** Z-axis translation on hover in px (default: 20) */
  zTranslate?: number;
}

interface UseTilt3DReturn {
  ref: React.RefObject<HTMLDivElement>;
  style: TiltStyle;
  onMouseMove: (e: React.MouseEvent<HTMLDivElement>) => void;
  onMouseEnter: (e: React.MouseEvent<HTMLDivElement>) => void;
  onMouseLeave: () => void;
}

export function useTilt3D(options: UseTilt3DOptions = {}): UseTilt3DReturn {
  const {
    maxTilt = 15,
    scale = 1.02,
    speed = 150,
    enabled = true,
    perspective = 1000,
    zTranslate = 20,
  } = options;

  const ref = useRef<HTMLDivElement>(null);
  const [isHovered, setIsHovered] = useState(false);
  const [tiltStyle, setTiltStyle] = useState<TiltStyle>({
    transform: 'perspective(1000px) rotateX(0deg) rotateY(0deg) scale(1) translateZ(0px)',
    transition: `transform ${speed}ms cubic-bezier(0.03, 0.98, 0.52, 0.99)`,
  });

  const handleMouseMove = useCallback((e: React.MouseEvent<HTMLDivElement>) => {
    if (!enabled || !ref.current) return;

    const element = ref.current;
    const rect = element.getBoundingClientRect();
    
    // Calculate cursor position relative to element center (0 to 1, then -0.5 to 0.5)
    const x = (e.clientX - rect.left) / rect.width - 0.5;
    const y = (e.clientY - rect.top) / rect.height - 0.5;

    // Calculate tilt angles
    // Tilt on Y-axis when moving left/right (rotateY)
    // Tilt on X-axis when moving up/down (rotateX) - inverted for natural feel
    const rotateY = x * maxTilt;
    const rotateX = -y * maxTilt;

    setTiltStyle({
      transform: `perspective(${perspective}px) rotateX(${rotateX}deg) rotateY(${rotateY}deg) scale(${scale}) translateZ(${zTranslate}px)`,
      transition: `transform ${speed}ms cubic-bezier(0.03, 0.98, 0.52, 0.99)`,
    });
  }, [enabled, maxTilt, scale, speed, perspective, zTranslate]);

  const handleMouseEnter = useCallback((e: React.MouseEvent<HTMLDivElement>) => {
    if (!enabled) return;
    setIsHovered(true);
    handleMouseMove(e);
  }, [enabled, handleMouseMove]);

  const handleMouseLeave = useCallback(() => {
    if (!enabled) return;
    setIsHovered(false);
    setTiltStyle({
      transform: 'perspective(1000px) rotateX(0deg) rotateY(0deg) scale(1) translateZ(0px)',
      transition: `transform ${speed * 2}ms cubic-bezier(0.03, 0.98, 0.52, 0.99)`,
    });
  }, [enabled, speed]);

  return {
    ref,
    style: tiltStyle,
    onMouseMove: handleMouseMove,
    onMouseEnter: handleMouseEnter,
    onMouseLeave: handleMouseLeave,
  };
}

/**
 * TiltCard Component Props
 * Use this for a simpler wrapper approach
 */
export interface TiltCardProps {
  children: React.ReactNode;
  className?: string;
  maxTilt?: number;
  scale?: number;
  speed?: number;
  enabled?: boolean;
}
