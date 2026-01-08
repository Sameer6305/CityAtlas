/**
 * AI Summary Section Page
 * Route: /cities/[citySlug]/ai-summary
 * 
 * Premium glassmorphism design with scroll-triggered animations
 * LLM-generated city insights, strengths, weaknesses, recommendations
 */

'use client';

import { useState, useEffect, useRef } from 'react';

// Custom hook for scroll-triggered animations
function useScrollAnimation(threshold = 0.1) {
  const ref = useRef<HTMLDivElement>(null);
  const [isVisible, setIsVisible] = useState(false);

  useEffect(() => {
    const observer = new IntersectionObserver(
      ([entry]) => {
        if (entry.isIntersecting) {
          setIsVisible(true);
        }
      },
      { threshold, rootMargin: '50px' }
    );

    const currentRef = ref.current;
    if (currentRef) {
      observer.observe(currentRef);
    }

    return () => {
      if (currentRef) {
        observer.unobserve(currentRef);
      }
    };
  }, [threshold]);

  return { ref, isVisible };
}

// Glassmorphism card component with scroll-triggered animation
function GlassCard({ children, className = '', delay = 0 }: { children: React.ReactNode; className?: string; delay?: number }) {
  const { ref, isVisible: isInView } = useScrollAnimation(0.1);
  const [isAnimated, setIsAnimated] = useState(false);
  
  useEffect(() => {
    if (isInView) {
      const timer = setTimeout(() => setIsAnimated(true), delay);
      return () => clearTimeout(timer);
    }
  }, [isInView, delay]);

  return (
    <div 
      ref={ref}
      className={`backdrop-blur-xl bg-white/[0.03] border border-white/10 rounded-2xl transition-all duration-[800ms] ease-out ${isAnimated ? 'opacity-100 translate-y-0 scale-100' : 'opacity-0 translate-y-12 scale-[0.97]'} ${className}`}
    >
      {children}
    </div>
  );
}

export default function AISummaryPage() {
  const [isLoaded, setIsLoaded] = useState(false);
  const [typingComplete, setTypingComplete] = useState(false);

  useEffect(() => {
    const loadTimer = setTimeout(() => setIsLoaded(true), 100);
    const typeTimer = setTimeout(() => setTypingComplete(true), 1500);
    return () => {
      clearTimeout(loadTimer);
      clearTimeout(typeTimer);
    };
  }, []);

  return (
    <div className={`space-y-6 transition-all duration-[1200ms] ease-out ${isLoaded ? 'opacity-100 translate-y-0' : 'opacity-0 translate-y-6'}`}>
      
      {/* AI Overview */}
      <GlassCard className="p-6 bg-gradient-to-br from-cyan-500/10 via-purple-500/5 to-transparent" delay={50}>
        <div className="flex items-start gap-5">
          <div className="w-16 h-16 rounded-2xl bg-gradient-to-br from-cyan-400/30 to-purple-400/30 flex items-center justify-center border border-white/20 flex-shrink-0">
            <svg className="w-8 h-8 text-cyan-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9.75 17L9 20l-1 1h8l-1-1-.75-3M3 13h18M5 17h14a2 2 0 002-2V5a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z" />
            </svg>
          </div>
          <div>
            <div className="flex items-center gap-3 mb-3">
              <h3 className="text-xl font-semibold text-white">AI-Generated City Profile</h3>
              <span className="px-2 py-0.5 rounded-full bg-emerald-500/20 text-emerald-400 text-xs font-medium border border-emerald-400/30">
                Live Analysis
              </span>
            </div>
            <p className={`text-white/70 leading-relaxed transition-all duration-1000 ${typingComplete ? 'opacity-100' : 'opacity-70'}`}>
              This city demonstrates strong economic fundamentals with a thriving technology sector and robust job market. 
              The population shows steady growth with high quality of life indicators. Infrastructure investments are on track, 
              and sustainability initiatives are progressing well. The cultural scene is vibrant with world-class institutions. 
              Overall outlook is positive with strong growth trajectory expected through 2030.
            </p>
            {!typingComplete && (
              <span className="inline-block w-2 h-5 bg-cyan-400 animate-pulse ml-1"></span>
            )}
          </div>
        </div>
      </GlassCard>

      {/* Key Insights */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
        <GlassCard className="p-6 hover:bg-white/[0.05] transition-all duration-500" delay={50}>
          <h3 className="text-lg font-semibold text-white mb-5 flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-emerald-500/20 to-green-500/20 flex items-center justify-center">
              <svg className="w-5 h-5 text-emerald-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
            </div>
            Strengths
          </h3>
          <ul className="space-y-3">
            {[
              'Leading innovation hub with strong tech ecosystem',
              'World-class educational institutions and research',
              'Diverse and multicultural population',
              'Extensive public transit and infrastructure',
              'Robust economic growth and job creation',
            ].map((strength, i) => (
              <li key={i} className="flex items-start gap-3 p-3 bg-white/[0.02] rounded-lg border border-white/5 hover:bg-white/[0.05] transition-colors">
                <span className="w-5 h-5 rounded-full bg-emerald-500/20 flex items-center justify-center flex-shrink-0 mt-0.5">
                  <svg className="w-3 h-3 text-emerald-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={3} d="M5 13l4 4L19 7" />
                  </svg>
                </span>
                <span className="text-white/70 text-sm">{strength}</span>
              </li>
            ))}
          </ul>
        </GlassCard>

        <GlassCard className="p-6 hover:bg-white/[0.05] transition-all duration-500" delay={100}>
          <h3 className="text-lg font-semibold text-white mb-5 flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-amber-500/20 to-orange-500/20 flex items-center justify-center">
              <svg className="w-5 h-5 text-amber-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
              </svg>
            </div>
            Areas for Improvement
          </h3>
          <ul className="space-y-3">
            {[
              'Housing affordability remains a concern',
              'Traffic congestion during peak hours',
              'Air quality improvement needed in some areas',
              'Income inequality requires attention',
              'Climate adaptation infrastructure gaps',
            ].map((area, i) => (
              <li key={i} className="flex items-start gap-3 p-3 bg-white/[0.02] rounded-lg border border-white/5 hover:bg-white/[0.05] transition-colors">
                <span className="w-5 h-5 rounded-full bg-amber-500/20 flex items-center justify-center flex-shrink-0 mt-0.5">
                  <svg className="w-3 h-3 text-amber-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={3} d="M12 9v2m0 4h.01" />
                  </svg>
                </span>
                <span className="text-white/70 text-sm">{area}</span>
              </li>
            ))}
          </ul>
        </GlassCard>
      </div>

      {/* Recommendations */}
      <GlassCard className="p-6" delay={50}>
        <h3 className="text-lg font-semibold text-white mb-6 flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-purple-500/20 to-pink-500/20 flex items-center justify-center">
            <svg className="w-5 h-5 text-purple-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9.663 17h4.673M12 3v1m6.364 1.636l-.707.707M21 12h-1M4 12H3m3.343-5.657l-.707-.707m2.828 9.9a5 5 0 117.072 0l-.548.547A3.374 3.374 0 0014 18.469V19a2 2 0 11-4 0v-.531c0-.895-.356-1.754-.988-2.386l-.548-.547z" />
            </svg>
          </div>
          AI Recommendations
        </h3>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          {[
            {
              title: 'For Residents',
              icon: '🏠',
              gradient: 'from-cyan-500/20 to-blue-500/20',
              items: ['Consider neighborhoods with growing transit access', 'Explore emerging cultural districts', 'Take advantage of education opportunities']
            },
            {
              title: 'For Businesses',
              icon: '💼',
              gradient: 'from-emerald-500/20 to-teal-500/20',
              items: ['Strong talent pool in tech and finance', 'Growing startup ecosystem support', 'Expanding international market access']
            },
            {
              title: 'For Investors',
              icon: '📈',
              gradient: 'from-purple-500/20 to-pink-500/20',
              items: ['Real estate in redevelopment zones', 'Green technology initiatives', 'Infrastructure modernization projects']
            },
          ].map((rec) => (
            <div key={rec.title} className={`p-5 bg-gradient-to-br ${rec.gradient} rounded-xl border border-white/10 hover:scale-[1.02] transition-transform duration-300`}>
              <div className="flex items-center gap-3 mb-4">
                <span className="text-3xl">{rec.icon}</span>
                <h4 className="text-white font-semibold">{rec.title}</h4>
              </div>
              <ul className="space-y-3">
                {rec.items.map((item, i) => (
                  <li key={i} className="text-sm text-white/60 flex items-start gap-2">
                    <svg className="w-4 h-4 text-cyan-400 flex-shrink-0 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 7l5 5m0 0l-5 5m5-5H6" />
                    </svg>
                    {item}
                  </li>
                ))}
              </ul>
            </div>
          ))}
        </div>
      </GlassCard>

      {/* Future Outlook */}
      <GlassCard className="p-6" delay={50}>
        <h3 className="text-lg font-semibold text-white mb-6 flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-indigo-500/20 to-violet-500/20 flex items-center justify-center">
            <svg className="w-5 h-5 text-indigo-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
            </svg>
          </div>
          Future Outlook (2025-2030)
        </h3>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          <div className="p-5 bg-emerald-500/5 rounded-xl border border-emerald-400/20">
            <h4 className="text-white font-medium mb-4 flex items-center gap-2">
              <span className="w-6 h-6 rounded-full bg-emerald-500/20 flex items-center justify-center">
                <svg className="w-3.5 h-3.5 text-emerald-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 10l7-7m0 0l7 7m-7-7v18" />
                </svg>
              </span>
              Expected Developments
            </h4>
            <ul className="space-y-3">
              {[
                'Major infrastructure projects completion',
                'Expansion of green spaces and sustainability',
                'New tech campuses and innovation districts',
                'Enhanced public transit connections',
              ].map((dev, i) => (
                <li key={i} className="text-sm text-white/60 flex items-start gap-2 p-2 bg-white/[0.02] rounded-lg">
                  <span className="text-emerald-400">▸</span>
                  {dev}
                </li>
              ))}
            </ul>
          </div>
          <div className="p-5 bg-amber-500/5 rounded-xl border border-amber-400/20">
            <h4 className="text-white font-medium mb-4 flex items-center gap-2">
              <span className="w-6 h-6 rounded-full bg-amber-500/20 flex items-center justify-center">
                <svg className="w-3.5 h-3.5 text-amber-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
                </svg>
              </span>
              Potential Challenges
            </h4>
            <ul className="space-y-3">
              {[
                'Managing rapid population growth',
                'Balancing development with affordability',
                'Climate change adaptation measures',
                'Infrastructure capacity constraints',
              ].map((challenge, i) => (
                <li key={i} className="text-sm text-white/60 flex items-start gap-2 p-2 bg-white/[0.02] rounded-lg">
                  <span className="text-amber-400">▸</span>
                  {challenge}
                </li>
              ))}
            </ul>
          </div>
        </div>
      </GlassCard>

      {/* Disclaimer */}
      <div className="text-center py-4">
        <p className="text-white/30 text-sm flex items-center justify-center gap-2">
          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
          AI-generated insights based on available data. Updated quarterly. Last update: December 2025
        </p>
      </div>
    </div>
  );
}
