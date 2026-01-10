'use client';

import Image from 'next/image';
import Link from 'next/link';
import { useState, useEffect, useRef } from 'react';
import { useRouter } from 'next/navigation';
import { useTheme } from '@/store/useAppStore';
import { TiltCard } from '@/components/TiltCard';

export default function Home() {
  const router = useRouter();
  const { theme, toggleTheme } = useTheme();
  const [showContent, setShowContent] = useState(false);
  const [animationComplete, setAnimationComplete] = useState(false);
  const [showVideo, setShowVideo] = useState(false);
  const videoRef = useRef<HTMLVideoElement>(null);

  // Check if video has been played - use a navigation flag, not just session storage
  useEffect(() => {
    // Check if video has already been played in this session
    const hasPlayedVideo = sessionStorage.getItem('cityatlas_video_played') === 'true';
    
    if (hasPlayedVideo) {
      // Skip video if already played in this session
      setShowVideo(false);
      setShowContent(true);
      setAnimationComplete(true);
    } else {
      // Show and play video for first time in session
      setShowVideo(true);
      
      // Ensure video plays after a short delay to let element mount
      const playTimer = setTimeout(() => {
        if (videoRef.current) {
          const playPromise = videoRef.current.play();
          if (playPromise !== undefined) {
            playPromise.catch(err => {
              console.error('Video play error:', err);
              // If autoplay fails, try again
              setTimeout(() => {
                videoRef.current?.play().catch(e => console.error('Retry failed:', e));
              }, 500);
            });
          }
        }
      }, 100);
      
      const timer1 = setTimeout(() => setShowContent(true), 6000); // Start fade at 6s
      const timer2 = setTimeout(() => {
        setAnimationComplete(true);
        setShowVideo(false);
        // Mark that video has been played
        sessionStorage.setItem('cityatlas_video_played', 'true');
      }, 8000); // Complete at 8s
      
      return () => {
        clearTimeout(playTimer);
        clearTimeout(timer1);
        clearTimeout(timer2);
      };
    }
  }, []);

  /**
   * Hero Landing Page - Matching Reference Design
   * Design Features:
   * - Full-screen city skyline background
   * - Dark gradient overlay (navy → deep blue → black)
   * - Glassmorphism UI elements
   * - Centered hero content
   * - Premium SaaS aesthetic with subtle animations
   */

  return (
    <div className="min-h-screen relative overflow-hidden">
      {/* 
        BACKGROUND LAYER
        Full-screen city skyline image with dark gradient overlay
        Creates premium night cityscape atmosphere
      */}
      <div className="fixed inset-0 -z-10">
        {/* City Skyline Background Image */}
        <Image
          src="/background.png"
          alt="City Skyline"
          fill
          className="object-cover"
          priority
          quality={100}
        />
        
        {/* Very light gradient overlay for maximum cityscape visibility */}
        <div className="absolute inset-0 bg-gradient-to-b from-[#0a0e27]/40 via-[#0f1420]/35 to-[#000000]/50" />
        
        {/* Subtle star particle overlay */}
        <div className="absolute inset-0 opacity-40">
          <div className="absolute top-[10%] left-[20%] w-1 h-1 bg-white rounded-full animate-pulse" />
          <div className="absolute top-[15%] left-[60%] w-1 h-1 bg-white rounded-full animate-pulse" />
          <div className="absolute top-[25%] left-[80%] w-1 h-1 bg-cyan-300 rounded-full animate-pulse" />
          <div className="absolute top-[40%] left-[15%] w-1 h-1 bg-white rounded-full animate-pulse" />
          <div className="absolute top-[50%] left-[85%] w-1 h-1 bg-blue-300 rounded-full animate-pulse" />
          <div className="absolute top-[70%] left-[40%] w-1 h-1 bg-white rounded-full animate-pulse" />
        </div>
        
        {/* Soft ambient glow - more subtle */}
        <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[1200px] h-[1200px] bg-cyan-500/3 rounded-full blur-3xl" />
      </div>

      {/* Landing Animation with Video */}
      {showVideo && !animationComplete && (
        <div className="fixed inset-0 z-[60] flex items-center justify-center bg-black">
          <video
            ref={videoRef}
            autoPlay
            muted
            playsInline
            preload="auto"
            className="w-full h-full object-cover"
          >
            <source src="/video.mp4" type="video/mp4" />
          </video>
          {/* Fade Overlay for smooth transition */}
          <div 
            className={`absolute inset-0 bg-black transition-opacity duration-2000 ease-in-out pointer-events-none ${showContent ? 'opacity-100' : 'opacity-0'}`}
          />
          {/* Skip Button */}
          <button
            onClick={() => {
              setShowContent(true);
              setAnimationComplete(true);
              setShowVideo(false);
              // Mark video as played when skipped
              sessionStorage.setItem('cityatlas_video_played', 'true');
            }}
            className="absolute bottom-8 right-8 px-6 py-3 bg-white/10 hover:bg-white/20 backdrop-blur-md border border-white/30 text-white rounded-2xl font-medium transition-all duration-300 hover:scale-105 hover:shadow-lg hover:shadow-white/20"
          >
            Skip Intro →
          </button>
        </div>
      )}

      {/* 
        GLASS NAVIGATION BAR
        Transparent navbar with backdrop blur
        Positioned at top with rounded corners
      */}
      <nav className={`fixed top-0 left-0 right-0 z-40 transition-opacity duration-2000 ${animationComplete ? 'opacity-100' : 'opacity-0'}`} style={{ pointerEvents: animationComplete ? 'auto' : 'none' }}>
        <div className="mx-6 mt-5">
          <div className="backdrop-blur-md bg-white/[0.03] border border-white/[0.08] rounded-xl shadow-lg">
            <div className="px-5 py-2.5 flex items-center justify-between">
              {/* Logo Section */}
              <button 
                onClick={() => router.push('/')} 
                className="flex items-center gap-3 hover:opacity-80 hover:scale-105 transition-all duration-300 group"
              >
                <Image 
                  src="/logo.png" 
                  alt="CityAtlas" 
                  width={38} 
                  height={38} 
                  className="rounded-lg group-hover:rotate-12 transition-transform duration-300" 
                  priority 
                />
                <span className="text-lg font-bold text-white group-hover:text-cyan-300 transition-colors duration-300">CityAtlas</span>
              </button>
              
              {/* Right Actions */}
              <div className="flex items-center gap-3">
                {/* Search Icon */}
                <button
                  className="p-2 rounded-xl hover:bg-white/5 transition-all duration-200 hover:scale-110 hover:-translate-y-0.5 group"
                  aria-label="Search"
                >
                  <svg className="w-5 h-5 text-white/70 group-hover:text-cyan-300 transition-colors duration-200" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
                  </svg>
                </button>
                
                {/* Theme Toggle */}
                <button
                  onClick={toggleTheme}
                  className="p-2 rounded-xl hover:bg-white/5 transition-all duration-200 hover:scale-110 hover:-translate-y-0.5 hover:rotate-12"
                  aria-label="Toggle theme"
                >
                  <span className="text-lg">{theme === 'dark' ? '☀️' : '🌙'}</span>
                </button>
              </div>
            </div>
          </div>
        </div>
      </nav>

      {/* Main Content - Fade In */}
      <div className={`transition-opacity duration-2000 ease-in-out ${animationComplete ? 'opacity-100' : 'opacity-0'} ${animationComplete ? '' : 'pointer-events-none'}`}>
        
        {/* 
          HERO SECTION - Centered Content
          Matching reference design with:
          - App icon in glass container
          - Large title with gradient
          - Subtitle and description
          - Two prominent CTA buttons
        */}
        <section className="min-h-screen flex flex-col items-center justify-center px-6 pt-28 pb-12">
          <div className="text-center max-w-4xl mx-auto">
            
            {/* App Icon in Glass Container with Tilt Effect */}
            <div className="mb-10 animate-fade-in-up">
              <TiltCard maxTilt={20} scale={1.08}>
                <div className="inline-flex p-6 backdrop-blur-md bg-gradient-to-br from-blue-500/[0.08] to-cyan-500/[0.08] border border-white/[0.15] rounded-3xl shadow-lg hover:shadow-2xl hover:shadow-cyan-500/30 hover:border-cyan-400/40 transition-all duration-300 cursor-pointer">
                  <Image 
                    src="/logo.png" 
                    alt="CityAtlas Logo" 
                    width={90} 
                    height={90} 
                    className="rounded-2xl transition-transform duration-300" 
                    priority 
                  />
                </div>
              </TiltCard>
            </div>

            {/* Main Title with Gradient */}
            <h1 className="text-7xl md:text-8xl lg:text-9xl font-extrabold mb-6 bg-gradient-to-r from-cyan-300 via-blue-200 to-cyan-300 bg-clip-text text-transparent leading-none animate-fade-in-up tracking-tight hover:scale-105 hover:tracking-wider transition-all duration-500 cursor-default">
              CityAtlas
            </h1>

            {/* Subtitle */}
            <p className="text-3xl md:text-4xl text-white/95 font-light mb-6 animate-fade-in-up tracking-wide hover:text-cyan-200 hover:scale-105 hover:tracking-widest transition-all duration-500 cursor-default">
              Intelligence in Urban Flow
            </p>

            {/* Description */}
            <p className="text-lg md:text-xl text-white/70 max-w-3xl mx-auto leading-relaxed mb-12 animate-fade-in-up hover:text-white/95 hover:scale-105 transition-all duration-500 cursor-default">
              Explore cities as structured resumes with real-time data and AI-powered insights
            </p>

            {/* CTA Buttons with Tilt Effect */}
            <div className="flex flex-col sm:flex-row gap-4 justify-center items-center animate-fade-in-up">
              {/* Primary Button - Explore Cities with Blue Glow */}
              <TiltCard maxTilt={15} scale={1.05}>
                <Link
                  href="/cities"
                  prefetch={true}
                  className="group relative px-9 py-4 bg-gradient-to-r from-cyan-500 to-blue-600 hover:from-cyan-400 hover:to-blue-500 text-white rounded-xl font-semibold text-base shadow-lg shadow-cyan-500/40 hover:shadow-2xl hover:shadow-cyan-500/70 transition-all duration-300 flex items-center gap-2 overflow-hidden"
                >
                  <span className="relative z-10">Explore Cities</span>
                  <span className="relative z-10 group-hover:translate-x-2 transition-transform duration-300">→</span>
                  {/* Animated shine effect */}
                  <div className="absolute inset-0 -translate-x-full group-hover:translate-x-full transition-transform duration-1000 bg-gradient-to-r from-transparent via-white/20 to-transparent" />
                </Link>
              </TiltCard>

              {/* Secondary Button - Glass Outline */}
              <TiltCard maxTilt={15} scale={1.05}>
                <Link
                  href="/cities/san-francisco"
                  prefetch={true}
                  className="group px-9 py-4 backdrop-blur-md bg-white/[0.03] hover:bg-white/[0.08] border border-white/[0.15] hover:border-cyan-400/50 text-white rounded-xl font-semibold text-base transition-all duration-300 hover:shadow-lg hover:shadow-cyan-500/20 block"
                >
                  <span className="group-hover:text-cyan-300 transition-colors duration-300">See Demo</span>
                </Link>
              </TiltCard>
            </div>

          </div>
        </section>

      </div>
    </div>
  );
}


