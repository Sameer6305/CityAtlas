/**
 * AI Summary Section Page
 * Route: /cities/[citySlug]/ai-summary
 * 
 * Fetches rule-based AI city insights from backend endpoint:
 * GET /api/ai/summary/{citySlug}
 * 
 * Displays personality, strengths, weaknesses, and bestSuitedFor
 * with full transparency about the rule-based analysis approach.
 */

'use client';

import { useState, useEffect, useRef } from 'react';
import { useParams } from 'next/navigation';

const API_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080/api';

interface AiSummaryData {
  personality: string;
  strengths: string[];
  weaknesses: string[];
  bestSuitedFor: string[];
  economyScore: number | null;
  livabilityScore: number | null;
  sustainabilityScore: number | null;
  overallScore: number | null;
  dataCompleteness: number | null;
  scoreExplanations: {
    economy: string | null;
    livability: string | null;
    sustainability: string | null;
    overall: string | null;
  } | null;
}

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
  const params = useParams();
  const citySlug = params.citySlug as string;
  const [summary, setSummary] = useState<AiSummaryData | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const loadTimer = setTimeout(() => setIsLoaded(true), 100);
    const typeTimer = setTimeout(() => setTypingComplete(true), 1500);
    return () => {
      clearTimeout(loadTimer);
      clearTimeout(typeTimer);
    };
  }, []);

  useEffect(() => {
    if (!citySlug) return;
    setLoading(true);
    setError(null);
    fetch(`${API_URL}/ai/summary/${citySlug}`)
      .then(res => {
        if (!res.ok) throw new Error(`City not found or AI summary unavailable (${res.status})`);
        return res.json();
      })
      .then((data: AiSummaryData) => {
        setSummary(data);
        setLoading(false);
      })
      .catch(err => {
        setError(err.message);
        setLoading(false);
      });
  }, [citySlug]);

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-[400px]">
        <div className="text-center space-y-4">
          <div className="w-12 h-12 border-4 border-cyan-400/30 border-t-cyan-400 rounded-full animate-spin mx-auto" />
          <p className="text-white/50 text-sm">Generating AI analysis...</p>
        </div>
      </div>
    );
  }

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
              <span className="px-2 py-0.5 rounded-full bg-blue-500/20 text-blue-400 text-xs font-medium border border-blue-400/30">
                Rule-Based Analysis
              </span>
            </div>
            {error ? (
              <p className="text-white/50 leading-relaxed">
                AI summary is not available for this city. The analysis requires the city to be registered in the database 
                with economic and demographic metrics. Try a major city like San Francisco, New York, or Austin.
              </p>
            ) : (
              <>
                <p className={`text-white/70 leading-relaxed transition-all duration-1000 ${typingComplete ? 'opacity-100' : 'opacity-70'}`}>
                  {summary?.personality || 'No personality summary available.'}
                </p>
                {!typingComplete && (
                  <span className="inline-block w-2 h-5 bg-cyan-400 animate-pulse ml-1"></span>
                )}
              </>
            )}
          </div>
        </div>
      </GlassCard>

      {/* Key Insights — from real backend */}
      {summary && !error && (
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
              {(summary.strengths || []).map((strength, i) => (
                <li key={i} className="flex items-start gap-3 p-3 bg-white/[0.02] rounded-lg border border-white/5 hover:bg-white/[0.05] transition-colors">
                  <span className="w-5 h-5 rounded-full bg-emerald-500/20 flex items-center justify-center flex-shrink-0 mt-0.5">
                    <svg className="w-3 h-3 text-emerald-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={3} d="M5 13l4 4L19 7" />
                    </svg>
                  </span>
                  <span className="text-white/70 text-sm">{strength}</span>
                </li>
              ))}
              {(!summary.strengths || summary.strengths.length === 0) && (
                <p className="text-white/40 text-sm">No strengths data available.</p>
              )}
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
              {(summary.weaknesses || []).map((area, i) => (
                <li key={i} className="flex items-start gap-3 p-3 bg-white/[0.02] rounded-lg border border-white/5 hover:bg-white/[0.05] transition-colors">
                  <span className="w-5 h-5 rounded-full bg-amber-500/20 flex items-center justify-center flex-shrink-0 mt-0.5">
                    <svg className="w-3 h-3 text-amber-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={3} d="M12 9v2m0 4h.01" />
                    </svg>
                  </span>
                  <span className="text-white/70 text-sm">{area}</span>
                </li>
              ))}
              {(!summary.weaknesses || summary.weaknesses.length === 0) && (
                <p className="text-white/40 text-sm">No weaknesses data available.</p>
              )}
            </ul>
          </GlassCard>
        </div>
      )}

      {/* Best Suited For — from real backend */}
      {summary?.bestSuitedFor && summary.bestSuitedFor.length > 0 && (
        <GlassCard className="p-6" delay={50}>
          <h3 className="text-lg font-semibold text-white mb-6 flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-purple-500/20 to-pink-500/20 flex items-center justify-center">
              <svg className="w-5 h-5 text-purple-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z" />
              </svg>
            </div>
            Best Suited For
          </h3>
          <div className="flex flex-wrap gap-3">
            {summary.bestSuitedFor.map((audience, i) => (
              <div
                key={i}
                className="px-4 py-2.5 rounded-xl bg-gradient-to-br from-purple-500/10 to-pink-500/10 border border-purple-500/20 text-white/80 text-sm font-medium hover:bg-white/[0.05] transition-colors duration-300"
              >
                {audience}
              </div>
            ))}
          </div>
        </GlassCard>
      )}

      {/* AI Computed Scores — from CityFeatureComputer */}
      {summary && summary.overallScore != null && (
        <GlassCard className="p-6" delay={75}>
          <h3 className="text-lg font-semibold text-white mb-5 flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-cyan-500/20 to-blue-500/20 flex items-center justify-center">
              <svg className="w-5 h-5 text-cyan-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" />
              </svg>
            </div>
            Computed Scores
            <span className="text-white/30 text-xs font-normal ml-2">0–100 scale</span>
          </h3>

          {/* Overall score ring */}
          <div className="flex items-center gap-8 mb-6">
            <div className="relative w-24 h-24 flex-shrink-0">
              <svg className="w-24 h-24 -rotate-90" viewBox="0 0 100 100">
                <circle cx="50" cy="50" r="42" fill="none" stroke="rgba(255,255,255,0.05)" strokeWidth="8" />
                <circle cx="50" cy="50" r="42" fill="none"
                  stroke={summary.overallScore >= 70 ? '#06b6d4' : summary.overallScore >= 50 ? '#f59e0b' : '#ef4444'}
                  strokeWidth="8" strokeLinecap="round"
                  strokeDasharray={`${(summary.overallScore / 100) * 264} 264`}
                />
              </svg>
              <div className="absolute inset-0 flex items-center justify-center">
                <span className="text-2xl font-bold text-white">{Math.round(summary.overallScore)}</span>
              </div>
            </div>
            <div>
              <p className="text-white font-semibold text-lg">Overall Score</p>
              <p className="text-white/50 text-sm mt-1">{summary.scoreExplanations?.overall || 'Weighted from economy, livability, sustainability'}</p>
              {summary.dataCompleteness != null && (
                <p className="text-white/30 text-xs mt-2">Data completeness: {Math.round(summary.dataCompleteness)}%</p>
              )}
            </div>
          </div>

          {/* Dimension breakdown */}
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            {[
              { label: 'Economy', score: summary.economyScore, color: 'emerald', explanation: summary.scoreExplanations?.economy },
              { label: 'Livability', score: summary.livabilityScore, color: 'cyan', explanation: summary.scoreExplanations?.livability },
              { label: 'Sustainability', score: summary.sustainabilityScore, color: 'amber', explanation: summary.scoreExplanations?.sustainability },
            ].map(dim => (
              <div key={dim.label} className="p-4 rounded-xl bg-white/[0.03] border border-white/5">
                <div className="flex justify-between items-center mb-2">
                  <span className="text-white/70 text-sm font-medium">{dim.label}</span>
                  <span className={`text-lg font-bold ${dim.score != null && dim.score >= 70 ? `text-${dim.color}-400` : dim.score != null && dim.score >= 50 ? 'text-yellow-400' : 'text-white/40'}`}>
                    {dim.score != null ? Math.round(dim.score) : '—'}
                  </span>
                </div>
                <div className="w-full h-2 rounded-full bg-white/[0.05] overflow-hidden">
                  <div
                    className={`h-full rounded-full transition-all duration-1000 ${dim.color === 'emerald' ? 'bg-emerald-500' : dim.color === 'cyan' ? 'bg-cyan-500' : 'bg-amber-500'}`}
                    style={{ width: `${dim.score ?? 0}%` }}
                  />
                </div>
                {dim.explanation && <p className="text-white/40 text-xs mt-2">{dim.explanation}</p>}
              </div>
            ))}
          </div>
        </GlassCard>
      )}

      {/* Data Source + Methodology Disclaimer */}
      <GlassCard className="p-6" delay={50}>
        <div className="flex items-start gap-4">
          <div className="w-12 h-12 rounded-xl bg-gradient-to-br from-blue-500/20 to-indigo-500/20 flex items-center justify-center flex-shrink-0">
            <svg className="w-6 h-6 text-blue-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
          </div>
          <div className="space-y-3">
            <h3 className="text-lg font-semibold text-white">About This Analysis</h3>
            <div className="space-y-2 text-sm text-white/60">
              <p>
                <span className="text-white font-medium">Methodology:</span> This summary is generated by a 
                <strong className="text-blue-400"> rule-based analysis engine</strong>, not a large language model. 
                It evaluates city metrics (GDP, population, unemployment, cost of living, air quality) using 
                transparent, human-readable logic with scored components.
              </p>
              <p>
                <span className="text-white font-medium">Data:</span> Analysis is based on real city data from the 
                database, World Bank indicators, and air quality measurements.
              </p>
              <p className="text-white/40 text-xs pt-2 border-t border-white/5">
                All insights are derived from verifiable data sources. No fabricated content.
              </p>
            </div>
          </div>
        </div>
      </GlassCard>
    </div>
  );
}
