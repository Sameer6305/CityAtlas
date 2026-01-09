/**
 * City Comparison Page
 * Route: /compare
 * 
 * Side-by-side comparison of two cities across multiple dimensions:
 * - Career & Economy
 * - Quality of Life
 * - Environment
 * - Education
 * - AI Verdict
 */

'use client';

import Image from 'next/image';
import Link from 'next/link';
import { useState, useEffect, useRef, Suspense } from 'react';
import { useSearchParams } from 'next/navigation';
import { useComparison } from '@/store/useAppStore';

// ============================================
// TYPES
// ============================================

interface CityData {
  name: string;
  slug: string;
  country: string;
  population: number;
  description: string;
  gradient: string;
  accentColor: string;
  metrics: {
    // Career & Economy
    jobOpportunityScore: number;
    costOfLiving: number;
    averageSalary: number;
    majorIndustries: string[];
    unemploymentRate: number;
    
    // Quality of Life
    housingAffordability: number;
    safetyIndex: number;
    commuteScore: number;
    healthcareIndex: number;
    
    // Environment
    aqi: number;
    climateComfort: number;
    sustainabilityScore: number;
    greenSpacePercentage: number;
    
    // Education
    educationIndex: number;
    studentFriendliness: number;
    universityCount: number;
    researchOutput: number;
  };
}

// ============================================
// MOCK DATA
// ============================================

const citiesData: Record<string, CityData> = {
  'san-francisco': {
    name: 'San Francisco',
    slug: 'san-francisco',
    country: 'USA',
    population: 815201,
    description: 'Tech innovation hub with iconic landmarks',
    gradient: 'from-blue-500 to-cyan-500',
    accentColor: '#06b6d4',
    metrics: {
      jobOpportunityScore: 92,
      costOfLiving: 182,
      averageSalary: 125000,
      majorIndustries: ['Technology', 'Finance', 'Biotech'],
      unemploymentRate: 3.2,
      housingAffordability: 28,
      safetyIndex: 62,
      commuteScore: 58,
      healthcareIndex: 85,
      aqi: 45,
      climateComfort: 82,
      sustainabilityScore: 78,
      greenSpacePercentage: 18,
      educationIndex: 88,
      studentFriendliness: 72,
      universityCount: 12,
      researchOutput: 94,
    },
  },
  'austin': {
    name: 'Austin',
    slug: 'austin',
    country: 'USA',
    population: 964254,
    description: 'Live music capital and startup ecosystem',
    gradient: 'from-purple-500 to-pink-500',
    accentColor: '#a855f7',
    metrics: {
      jobOpportunityScore: 88,
      costOfLiving: 112,
      averageSalary: 95000,
      majorIndustries: ['Technology', 'Music', 'Healthcare'],
      unemploymentRate: 3.8,
      housingAffordability: 52,
      safetyIndex: 68,
      commuteScore: 45,
      healthcareIndex: 78,
      aqi: 52,
      climateComfort: 65,
      sustainabilityScore: 68,
      greenSpacePercentage: 22,
      educationIndex: 82,
      studentFriendliness: 85,
      universityCount: 8,
      researchOutput: 76,
    },
  },
  'seattle': {
    name: 'Seattle',
    slug: 'seattle',
    country: 'USA',
    population: 749256,
    description: 'Emerald city with thriving tech scene',
    gradient: 'from-green-500 to-teal-500',
    accentColor: '#10b981',
    metrics: {
      jobOpportunityScore: 90,
      costOfLiving: 158,
      averageSalary: 115000,
      majorIndustries: ['Technology', 'Aerospace', 'Retail'],
      unemploymentRate: 3.5,
      housingAffordability: 35,
      safetyIndex: 58,
      commuteScore: 52,
      healthcareIndex: 82,
      aqi: 42,
      climateComfort: 68,
      sustainabilityScore: 82,
      greenSpacePercentage: 28,
      educationIndex: 86,
      studentFriendliness: 78,
      universityCount: 10,
      researchOutput: 88,
    },
  },
  'new-york': {
    name: 'New York',
    slug: 'new-york',
    country: 'USA',
    population: 8336817,
    description: 'The city that never sleeps',
    gradient: 'from-yellow-500 to-orange-500',
    accentColor: '#f59e0b',
    metrics: {
      jobOpportunityScore: 95,
      costOfLiving: 187,
      averageSalary: 105000,
      majorIndustries: ['Finance', 'Media', 'Fashion'],
      unemploymentRate: 4.2,
      housingAffordability: 22,
      safetyIndex: 55,
      commuteScore: 72,
      healthcareIndex: 88,
      aqi: 58,
      climateComfort: 62,
      sustainabilityScore: 65,
      greenSpacePercentage: 14,
      educationIndex: 92,
      studentFriendliness: 68,
      universityCount: 45,
      researchOutput: 96,
    },
  },
  'boston': {
    name: 'Boston',
    slug: 'boston',
    country: 'USA',
    population: 675647,
    description: 'Historical hub of education and innovation',
    gradient: 'from-red-500 to-orange-500',
    accentColor: '#ef4444',
    metrics: {
      jobOpportunityScore: 85,
      costOfLiving: 152,
      averageSalary: 98000,
      majorIndustries: ['Education', 'Healthcare', 'Biotech'],
      unemploymentRate: 3.6,
      housingAffordability: 38,
      safetyIndex: 65,
      commuteScore: 68,
      healthcareIndex: 95,
      aqi: 48,
      climateComfort: 58,
      sustainabilityScore: 72,
      greenSpacePercentage: 16,
      educationIndex: 96,
      studentFriendliness: 92,
      universityCount: 35,
      researchOutput: 98,
    },
  },
};

// ============================================
// UTILITY FUNCTIONS
// ============================================

function formatNumber(num: number): string {
  if (num >= 1000000) return (num / 1000000).toFixed(1) + 'M';
  if (num >= 1000) return (num / 1000).toFixed(0) + 'K';
  return num.toString();
}

function formatCurrency(num: number): string {
  return '$' + num.toLocaleString();
}

// Determines which city is better for a metric
// higherIsBetter: true means higher value wins, false means lower value wins
function getWinner(valueA: number, valueB: number, higherIsBetter: boolean = true): 'A' | 'B' | 'tie' {
  if (valueA === valueB) return 'tie';
  if (higherIsBetter) {
    return valueA > valueB ? 'A' : 'B';
  }
  return valueA < valueB ? 'A' : 'B';
}

// ============================================
// COMPONENTS
// ============================================

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

// Glassmorphism card component
function GlassCard({ children, className = '', delay = 0 }: { children: React.ReactNode; className?: string; delay?: number }) {
  const { ref, isVisible } = useScrollAnimation(0.1);
  const [isAnimated, setIsAnimated] = useState(false);
  
  useEffect(() => {
    if (isVisible) {
      const timer = setTimeout(() => setIsAnimated(true), delay);
      return () => clearTimeout(timer);
    }
  }, [isVisible, delay]);

  return (
    <div 
      ref={ref}
      className={`backdrop-blur-xl bg-white/[0.03] border border-white/[0.08] rounded-2xl transition-all duration-[800ms] ease-out hover:bg-white/[0.06] hover:border-white/[0.15] hover:shadow-2xl hover:shadow-cyan-500/10 ${isAnimated ? 'opacity-100 translate-y-0 scale-100' : 'opacity-0 translate-y-12 scale-[0.97]'} ${className}`}
    >
      {children}
    </div>
  );
}

// City selector dropdown
function CitySelector({ 
  value, 
  onChange, 
  excludeSlug,
  label 
}: { 
  value: string; 
  onChange: (slug: string) => void; 
  excludeSlug?: string;
  label: string;
}) {
  const cities = Object.values(citiesData).filter(c => c.slug !== excludeSlug);
  const selectedCity = citiesData[value];
  
  return (
    <div className="space-y-2">
      <label className="text-white/60 text-sm font-medium">{label}</label>
      <select
        value={value}
        onChange={(e) => onChange(e.target.value)}
        className="w-full px-4 py-3 bg-white/[0.05] border border-white/[0.1] rounded-xl text-white focus:outline-none focus:border-cyan-500/50 focus:ring-2 focus:ring-cyan-500/20 transition-all appearance-none cursor-pointer"
        style={{ 
          backgroundImage: `url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' fill='none' viewBox='0 0 24 24' stroke='white'%3E%3Cpath stroke-linecap='round' stroke-linejoin='round' stroke-width='2' d='M19 9l-7 7-7-7'%3E%3C/path%3E%3C/svg%3E")`,
          backgroundRepeat: 'no-repeat',
          backgroundPosition: 'right 12px center',
          backgroundSize: '20px',
        }}
      >
        <option value="" className="bg-[#0f1420] text-white/60">Select a city...</option>
        {cities.map((city) => (
          <option key={city.slug} value={city.slug} className="bg-[#0f1420] text-white">
            {city.name}
          </option>
        ))}
      </select>
    </div>
  );
}

// Comparison metric row
function ComparisonMetric({
  label,
  valueA,
  valueB,
  formatFn = (v: number) => v.toString(),
  higherIsBetter = true,
  unit = '',
  iconA,
  iconB,
}: {
  label: string;
  valueA: number;
  valueB: number;
  formatFn?: (v: number) => string;
  higherIsBetter?: boolean;
  unit?: string;
  iconA?: React.ReactNode;
  iconB?: React.ReactNode;
}) {
  const winner = getWinner(valueA, valueB, higherIsBetter);
  
  return (
    <div className="grid grid-cols-3 gap-4 py-4 border-b border-white/[0.05] last:border-0">
      {/* City A Value */}
      <div className={`flex items-center justify-start gap-2 ${winner === 'A' ? 'text-emerald-400' : 'text-white/70'}`}>
        {winner === 'A' && (
          <svg className="w-4 h-4 text-emerald-400" fill="currentColor" viewBox="0 0 20 20">
            <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clipRule="evenodd" />
          </svg>
        )}
        <span className="font-semibold text-lg">{formatFn(valueA)}{unit}</span>
      </div>
      
      {/* Label */}
      <div className="flex items-center justify-center text-center">
        <span className="text-white/60 text-sm font-medium">{label}</span>
      </div>
      
      {/* City B Value */}
      <div className={`flex items-center justify-end gap-2 ${winner === 'B' ? 'text-emerald-400' : 'text-white/70'}`}>
        <span className="font-semibold text-lg">{formatFn(valueB)}{unit}</span>
        {winner === 'B' && (
          <svg className="w-4 h-4 text-emerald-400" fill="currentColor" viewBox="0 0 20 20">
            <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clipRule="evenodd" />
          </svg>
        )}
      </div>
    </div>
  );
}

// Score bar comparison
function ScoreBar({
  label,
  scoreA,
  scoreB,
  colorA = 'from-cyan-500 to-blue-500',
  colorB = 'from-purple-500 to-pink-500',
  higherIsBetter = true,
}: {
  label: string;
  scoreA: number;
  scoreB: number;
  colorA?: string;
  colorB?: string;
  higherIsBetter?: boolean;
}) {
  const winner = getWinner(scoreA, scoreB, higherIsBetter);
  
  return (
    <div className="space-y-3">
      <div className="flex items-center justify-between">
        <span className="text-white/70 text-sm font-medium">{label}</span>
        <div className="flex items-center gap-4 text-sm">
          <span className={`font-semibold ${winner === 'A' ? 'text-emerald-400' : 'text-white/60'}`}>
            {scoreA}
          </span>
          <span className="text-white/30">vs</span>
          <span className={`font-semibold ${winner === 'B' ? 'text-emerald-400' : 'text-white/60'}`}>
            {scoreB}
          </span>
        </div>
      </div>
      <div className="flex gap-2">
        <div className="flex-1 h-3 bg-white/[0.05] rounded-full overflow-hidden">
          <div 
            className={`h-full bg-gradient-to-r ${colorA} rounded-full transition-all duration-1000`}
            style={{ width: `${scoreA}%` }}
          />
        </div>
        <div className="flex-1 h-3 bg-white/[0.05] rounded-full overflow-hidden">
          <div 
            className={`h-full bg-gradient-to-r ${colorB} rounded-full transition-all duration-1000`}
            style={{ width: `${scoreB}%` }}
          />
        </div>
      </div>
    </div>
  );
}

// AI Verdict Card
function AIVerdictCard({ cityA, cityB }: { cityA: CityData; cityB: CityData }) {
  const generateVerdict = () => {
    const aMetrics = cityA.metrics;
    const bMetrics = cityB.metrics;
    
    // Calculate overall scores for each category
    const professionalScoreA = (aMetrics.jobOpportunityScore + (100 - aMetrics.costOfLiving/2) + aMetrics.commuteScore) / 3;
    const professionalScoreB = (bMetrics.jobOpportunityScore + (100 - bMetrics.costOfLiving/2) + bMetrics.commuteScore) / 3;
    
    const studentScoreA = (aMetrics.educationIndex + aMetrics.studentFriendliness + (100 - aMetrics.costOfLiving/2)) / 3;
    const studentScoreB = (bMetrics.educationIndex + bMetrics.studentFriendliness + (100 - bMetrics.costOfLiving/2)) / 3;
    
    const familyScoreA = (aMetrics.safetyIndex + aMetrics.healthcareIndex + aMetrics.housingAffordability + aMetrics.educationIndex) / 4;
    const familyScoreB = (bMetrics.safetyIndex + bMetrics.healthcareIndex + bMetrics.housingAffordability + bMetrics.educationIndex) / 4;
    
    return {
      professionals: {
        winner: professionalScoreA > professionalScoreB ? cityA.name : cityB.name,
        scoreA: Math.round(professionalScoreA),
        scoreB: Math.round(professionalScoreB),
        reason: professionalScoreA > professionalScoreB 
          ? `Higher job opportunities and better industry diversity`
          : `Better cost-to-salary ratio and work-life balance`,
      },
      students: {
        winner: studentScoreA > studentScoreB ? cityA.name : cityB.name,
        scoreA: Math.round(studentScoreA),
        scoreB: Math.round(studentScoreB),
        reason: studentScoreA > studentScoreB
          ? `More universities and stronger research output`
          : `More affordable with good student amenities`,
      },
      families: {
        winner: familyScoreA > familyScoreB ? cityA.name : cityB.name,
        scoreA: Math.round(familyScoreA),
        scoreB: Math.round(familyScoreB),
        reason: familyScoreA > familyScoreB
          ? `Better safety and healthcare infrastructure`
          : `More affordable housing and family-friendly environment`,
      },
    };
  };
  
  const verdict = generateVerdict();
  
  return (
    <GlassCard className="p-6" delay={200}>
      <div className="flex items-center gap-3 mb-6">
        <div className="w-12 h-12 rounded-xl bg-gradient-to-br from-cyan-500/20 to-purple-500/20 flex items-center justify-center">
          <svg className="w-6 h-6 text-cyan-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9.663 17h4.673M12 3v1m6.364 1.636l-.707.707M21 12h-1M4 12H3m3.343-5.657l-.707-.707m2.828 9.9a5 5 0 117.072 0l-.548.547A3.374 3.374 0 0014 18.469V19a2 2 0 11-4 0v-.531c0-.895-.356-1.754-.988-2.386l-.548-.547z" />
          </svg>
        </div>
        <div>
          <h3 className="text-xl font-semibold text-white">AI Verdict</h3>
          <p className="text-white/50 text-sm">Personalized recommendations based on your profile</p>
        </div>
      </div>
      
      <div className="space-y-6">
        {/* For Professionals */}
        <div className="p-4 bg-white/[0.02] rounded-xl border border-white/[0.05]">
          <div className="flex items-center gap-2 mb-3">
            <svg className="w-5 h-5 text-blue-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 13.255A23.931 23.931 0 0112 15c-3.183 0-6.22-.62-9-1.745M16 6V4a2 2 0 00-2-2h-4a2 2 0 00-2 2v2m4 6h.01M5 20h14a2 2 0 002-2V8a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z" />
            </svg>
            <span className="text-white font-medium">For Professionals</span>
          </div>
          <div className="flex items-center gap-3 mb-2">
            <span className="px-3 py-1 bg-emerald-500/20 text-emerald-400 rounded-full text-sm font-medium">
              Winner: {verdict.professionals.winner}
            </span>
            <span className="text-white/40 text-sm">
              {verdict.professionals.scoreA} vs {verdict.professionals.scoreB}
            </span>
          </div>
          <p className="text-white/60 text-sm">{verdict.professionals.reason}</p>
        </div>
        
        {/* For Students */}
        <div className="p-4 bg-white/[0.02] rounded-xl border border-white/[0.05]">
          <div className="flex items-center gap-2 mb-3">
            <svg className="w-5 h-5 text-purple-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path d="M12 14l9-5-9-5-9 5 9 5z" />
              <path d="M12 14l6.16-3.422a12.083 12.083 0 01.665 6.479A11.952 11.952 0 0012 20.055a11.952 11.952 0 00-6.824-2.998 12.078 12.078 0 01.665-6.479L12 14z" />
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 14l9-5-9-5-9 5 9 5zm0 0l6.16-3.422a12.083 12.083 0 01.665 6.479A11.952 11.952 0 0012 20.055a11.952 11.952 0 00-6.824-2.998 12.078 12.078 0 01.665-6.479L12 14zm-4 6v-7.5l4-2.222" />
            </svg>
            <span className="text-white font-medium">For Students</span>
          </div>
          <div className="flex items-center gap-3 mb-2">
            <span className="px-3 py-1 bg-emerald-500/20 text-emerald-400 rounded-full text-sm font-medium">
              Winner: {verdict.students.winner}
            </span>
            <span className="text-white/40 text-sm">
              {verdict.students.scoreA} vs {verdict.students.scoreB}
            </span>
          </div>
          <p className="text-white/60 text-sm">{verdict.students.reason}</p>
        </div>
        
        {/* For Families */}
        <div className="p-4 bg-white/[0.02] rounded-xl border border-white/[0.05]">
          <div className="flex items-center gap-2 mb-3">
            <svg className="w-5 h-5 text-amber-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6" />
            </svg>
            <span className="text-white font-medium">For Families</span>
          </div>
          <div className="flex items-center gap-3 mb-2">
            <span className="px-3 py-1 bg-emerald-500/20 text-emerald-400 rounded-full text-sm font-medium">
              Winner: {verdict.families.winner}
            </span>
            <span className="text-white/40 text-sm">
              {verdict.families.scoreA} vs {verdict.families.scoreB}
            </span>
          </div>
          <p className="text-white/60 text-sm">{verdict.families.reason}</p>
        </div>
      </div>
      
      {/* Trade-offs Summary */}
      <div className="mt-6 p-4 bg-gradient-to-r from-cyan-500/10 to-purple-500/10 rounded-xl border border-white/[0.08]">
        <h4 className="text-white font-medium mb-2">Key Trade-offs</h4>
        <p className="text-white/60 text-sm leading-relaxed">
          <strong className="text-cyan-400">{cityA.name}</strong> offers {cityA.metrics.jobOpportunityScore > cityB.metrics.jobOpportunityScore ? 'better job opportunities' : 'a more balanced lifestyle'} 
          {' '}but comes with {cityA.metrics.costOfLiving > cityB.metrics.costOfLiving ? 'higher living costs' : 'different trade-offs'}. 
          <strong className="text-purple-400"> {cityB.name}</strong> provides {cityB.metrics.housingAffordability > cityA.metrics.housingAffordability ? 'more affordable housing' : 'unique advantages'} 
          {' '}and {cityB.metrics.studentFriendliness > cityA.metrics.studentFriendliness ? 'is more student-friendly' : 'has its own strengths'}.
        </p>
      </div>
    </GlassCard>
  );
}

// ============================================
// MAIN PAGE COMPONENT
// ============================================

function ComparePageContent() {
  const searchParams = useSearchParams();
  const { comparisonList, removeFromComparison } = useComparison();
  
  const [isLoaded, setIsLoaded] = useState(false);
  const [cityASlug, setCityASlug] = useState<string>('');
  const [cityBSlug, setCityBSlug] = useState<string>('');
  
  // Initialize from URL params or comparison list
  useEffect(() => {
    const cityA = searchParams.get('cityA');
    const cityB = searchParams.get('cityB');
    
    if (cityA && citiesData[cityA]) {
      setCityASlug(cityA);
    } else if (comparisonList.length > 0 && citiesData[comparisonList[0]]) {
      setCityASlug(comparisonList[0]);
    }
    
    if (cityB && citiesData[cityB]) {
      setCityBSlug(cityB);
    } else if (comparisonList.length > 1 && citiesData[comparisonList[1]]) {
      setCityBSlug(comparisonList[1]);
    }
    
    const timer = setTimeout(() => setIsLoaded(true), 100);
    return () => clearTimeout(timer);
  }, [searchParams, comparisonList]);
  
  const cityA = cityASlug ? citiesData[cityASlug] : null;
  const cityB = cityBSlug ? citiesData[cityBSlug] : null;
  const canCompare = cityA && cityB;
  
  return (
    <div className={`relative min-h-screen overflow-hidden transition-opacity duration-[1200ms] ease-out ${isLoaded ? 'opacity-100' : 'opacity-0'}`}>
      {/* Full-screen Background */}
      <div className="fixed inset-0 -z-10">
        <Image
          src="/background.png"
          alt="City Skyline"
          fill
          className="object-cover"
          priority
          quality={100}
        />
        <div className="absolute inset-0 bg-gradient-to-b from-[#0a0e27]/10 via-[#0f1420]/5 to-[#000000]/15" />
      </div>

      {/* Main Content */}
      <div className="min-h-screen p-6 lg:p-8">
        {/* Header */}
        <div className="max-w-7xl mx-auto mb-8">
          <div className="flex items-center gap-4 mb-6">
            <Link 
              href="/cities"
              className="p-2 rounded-xl bg-white/[0.05] border border-white/[0.08] hover:bg-white/[0.1] transition-all"
            >
              <svg className="w-5 h-5 text-white/60" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10 19l-7-7m0 0l7-7m-7 7h18" />
              </svg>
            </Link>
            <div>
              <h1 className="text-3xl font-bold text-white">City Comparison</h1>
              <p className="text-white/50">Compare cities side-by-side to find your perfect match</p>
            </div>
          </div>
          
          {/* City Selectors */}
          <GlassCard className="p-6" delay={50}>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              <div className="space-y-4">
                <div className={`w-4 h-4 rounded-full bg-gradient-to-r ${cityA?.gradient || 'from-cyan-500 to-blue-500'}`} />
                <CitySelector
                  label="City A"
                  value={cityASlug}
                  onChange={setCityASlug}
                  excludeSlug={cityBSlug}
                />
                {cityA && (
                  <div className="flex items-center gap-3 p-3 bg-white/[0.02] rounded-xl">
                    <div className={`w-10 h-10 rounded-lg bg-gradient-to-br ${cityA.gradient} flex items-center justify-center text-white font-bold`}>
                      {cityA.name.charAt(0)}
                    </div>
                    <div>
                      <p className="text-white font-medium">{cityA.name}</p>
                      <p className="text-white/50 text-sm">{cityA.country} • Pop: {formatNumber(cityA.population)}</p>
                    </div>
                  </div>
                )}
              </div>
              
              <div className="space-y-4">
                <div className={`w-4 h-4 rounded-full bg-gradient-to-r ${cityB?.gradient || 'from-purple-500 to-pink-500'}`} />
                <CitySelector
                  label="City B"
                  value={cityBSlug}
                  onChange={setCityBSlug}
                  excludeSlug={cityASlug}
                />
                {cityB && (
                  <div className="flex items-center gap-3 p-3 bg-white/[0.02] rounded-xl">
                    <div className={`w-10 h-10 rounded-lg bg-gradient-to-br ${cityB.gradient} flex items-center justify-center text-white font-bold`}>
                      {cityB.name.charAt(0)}
                    </div>
                    <div>
                      <p className="text-white font-medium">{cityB.name}</p>
                      <p className="text-white/50 text-sm">{cityB.country} • Pop: {formatNumber(cityB.population)}</p>
                    </div>
                  </div>
                )}
              </div>
            </div>
          </GlassCard>
        </div>
        
        {/* Comparison Content */}
        {canCompare ? (
          <div className="max-w-7xl mx-auto space-y-6">
            {/* City Headers */}
            <div className="grid grid-cols-3 gap-4 text-center">
              <div className={`p-4 rounded-xl bg-gradient-to-br ${cityA.gradient}/20 border border-white/[0.08]`}>
                <h2 className="text-xl font-bold text-white">{cityA.name}</h2>
                <p className="text-white/50 text-sm">{cityA.description}</p>
              </div>
              <div className="flex items-center justify-center">
                <span className="text-white/40 text-2xl font-bold">VS</span>
              </div>
              <div className={`p-4 rounded-xl bg-gradient-to-br ${cityB.gradient}/20 border border-white/[0.08]`}>
                <h2 className="text-xl font-bold text-white">{cityB.name}</h2>
                <p className="text-white/50 text-sm">{cityB.description}</p>
              </div>
            </div>
            
            {/* Career & Economy */}
            <GlassCard className="p-6" delay={100}>
              <div className="flex items-center gap-3 mb-6">
                <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-emerald-500/20 to-teal-500/20 flex items-center justify-center">
                  <svg className="w-5 h-5 text-emerald-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                  </svg>
                </div>
                <div>
                  <h3 className="text-lg font-semibold text-white">Career & Economy</h3>
                  <p className="text-white/50 text-sm">Job opportunities and financial outlook</p>
                </div>
              </div>
              
              <div className="space-y-2">
                <ComparisonMetric
                  label="Job Opportunity Score"
                  valueA={cityA.metrics.jobOpportunityScore}
                  valueB={cityB.metrics.jobOpportunityScore}
                  formatFn={(v) => v.toString()}
                  unit="/100"
                  higherIsBetter={true}
                />
                <ComparisonMetric
                  label="Cost of Living Index"
                  valueA={cityA.metrics.costOfLiving}
                  valueB={cityB.metrics.costOfLiving}
                  formatFn={(v) => v.toString()}
                  higherIsBetter={false}
                />
                <ComparisonMetric
                  label="Average Salary"
                  valueA={cityA.metrics.averageSalary}
                  valueB={cityB.metrics.averageSalary}
                  formatFn={formatCurrency}
                  higherIsBetter={true}
                />
                <ComparisonMetric
                  label="Unemployment Rate"
                  valueA={cityA.metrics.unemploymentRate}
                  valueB={cityB.metrics.unemploymentRate}
                  formatFn={(v) => v.toString()}
                  unit="%"
                  higherIsBetter={false}
                />
              </div>
              
              {/* Major Industries */}
              <div className="mt-6 grid grid-cols-3 gap-4">
                <div>
                  <div className="flex flex-wrap gap-2">
                    {cityA.metrics.majorIndustries.map((ind) => (
                      <span key={ind} className="px-3 py-1 bg-cyan-500/20 text-cyan-400 rounded-full text-xs font-medium">
                        {ind}
                      </span>
                    ))}
                  </div>
                </div>
                <div className="flex items-center justify-center">
                  <span className="text-white/40 text-sm">Major Industries</span>
                </div>
                <div>
                  <div className="flex flex-wrap gap-2 justify-end">
                    {cityB.metrics.majorIndustries.map((ind) => (
                      <span key={ind} className="px-3 py-1 bg-purple-500/20 text-purple-400 rounded-full text-xs font-medium">
                        {ind}
                      </span>
                    ))}
                  </div>
                </div>
              </div>
            </GlassCard>
            
            {/* Quality of Life */}
            <GlassCard className="p-6" delay={120}>
              <div className="flex items-center gap-3 mb-6">
                <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-blue-500/20 to-cyan-500/20 flex items-center justify-center">
                  <svg className="w-5 h-5 text-blue-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6" />
                  </svg>
                </div>
                <div>
                  <h3 className="text-lg font-semibold text-white">Quality of Life</h3>
                  <p className="text-white/50 text-sm">Living conditions and daily experience</p>
                </div>
              </div>
              
              <div className="space-y-4">
                <ScoreBar label="Housing Affordability" scoreA={cityA.metrics.housingAffordability} scoreB={cityB.metrics.housingAffordability} />
                <ScoreBar label="Safety Index" scoreA={cityA.metrics.safetyIndex} scoreB={cityB.metrics.safetyIndex} />
                <ScoreBar label="Commute Score" scoreA={cityA.metrics.commuteScore} scoreB={cityB.metrics.commuteScore} />
                <ScoreBar label="Healthcare Index" scoreA={cityA.metrics.healthcareIndex} scoreB={cityB.metrics.healthcareIndex} />
              </div>
            </GlassCard>
            
            {/* Environment */}
            <GlassCard className="p-6" delay={140}>
              <div className="flex items-center gap-3 mb-6">
                <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-green-500/20 to-emerald-500/20 flex items-center justify-center">
                  <svg className="w-5 h-5 text-green-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3.055 11H5a2 2 0 012 2v1a2 2 0 002 2 2 2 0 012 2v2.945M8 3.935V5.5A2.5 2.5 0 0010.5 8h.5a2 2 0 012 2 2 2 0 104 0 2 2 0 012-2h1.064M15 20.488V18a2 2 0 012-2h3.064M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                  </svg>
                </div>
                <div>
                  <h3 className="text-lg font-semibold text-white">Environment</h3>
                  <p className="text-white/50 text-sm">Air quality and sustainability</p>
                </div>
              </div>
              
              <div className="space-y-2">
                <ComparisonMetric
                  label="Air Quality Index (AQI)"
                  valueA={cityA.metrics.aqi}
                  valueB={cityB.metrics.aqi}
                  higherIsBetter={false}
                />
                <ComparisonMetric
                  label="Climate Comfort"
                  valueA={cityA.metrics.climateComfort}
                  valueB={cityB.metrics.climateComfort}
                  unit="/100"
                  higherIsBetter={true}
                />
                <ComparisonMetric
                  label="Sustainability Score"
                  valueA={cityA.metrics.sustainabilityScore}
                  valueB={cityB.metrics.sustainabilityScore}
                  unit="/100"
                  higherIsBetter={true}
                />
                <ComparisonMetric
                  label="Green Space"
                  valueA={cityA.metrics.greenSpacePercentage}
                  valueB={cityB.metrics.greenSpacePercentage}
                  unit="%"
                  higherIsBetter={true}
                />
              </div>
            </GlassCard>
            
            {/* Education */}
            <GlassCard className="p-6" delay={160}>
              <div className="flex items-center gap-3 mb-6">
                <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-purple-500/20 to-pink-500/20 flex items-center justify-center">
                  <svg className="w-5 h-5 text-purple-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path d="M12 14l9-5-9-5-9 5 9 5z" />
                    <path d="M12 14l6.16-3.422a12.083 12.083 0 01.665 6.479A11.952 11.952 0 0012 20.055a11.952 11.952 0 00-6.824-2.998 12.078 12.078 0 01.665-6.479L12 14z" />
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 14l9-5-9-5-9 5 9 5zm0 0l6.16-3.422a12.083 12.083 0 01.665 6.479A11.952 11.952 0 0012 20.055a11.952 11.952 0 00-6.824-2.998 12.078 12.078 0 01.665-6.479L12 14zm-4 6v-7.5l4-2.222" />
                  </svg>
                </div>
                <div>
                  <h3 className="text-lg font-semibold text-white">Education</h3>
                  <p className="text-white/50 text-sm">Academic institutions and opportunities</p>
                </div>
              </div>
              
              <div className="space-y-4">
                <ScoreBar label="Education Index" scoreA={cityA.metrics.educationIndex} scoreB={cityB.metrics.educationIndex} colorA="from-purple-500 to-pink-500" colorB="from-cyan-500 to-blue-500" />
                <ScoreBar label="Student Friendliness" scoreA={cityA.metrics.studentFriendliness} scoreB={cityB.metrics.studentFriendliness} colorA="from-purple-500 to-pink-500" colorB="from-cyan-500 to-blue-500" />
                <ScoreBar label="Research Output" scoreA={cityA.metrics.researchOutput} scoreB={cityB.metrics.researchOutput} colorA="from-purple-500 to-pink-500" colorB="from-cyan-500 to-blue-500" />
              </div>
              
              <div className="mt-4 grid grid-cols-3 gap-4 text-center">
                <div className="p-3 bg-white/[0.02] rounded-xl">
                  <p className="text-2xl font-bold text-white">{cityA.metrics.universityCount}</p>
                  <p className="text-white/50 text-sm">Universities</p>
                </div>
                <div className="flex items-center justify-center">
                  <span className="text-white/30 text-sm">Count</span>
                </div>
                <div className="p-3 bg-white/[0.02] rounded-xl">
                  <p className="text-2xl font-bold text-white">{cityB.metrics.universityCount}</p>
                  <p className="text-white/50 text-sm">Universities</p>
                </div>
              </div>
            </GlassCard>
            
            {/* AI Verdict */}
            <AIVerdictCard cityA={cityA} cityB={cityB} />
          </div>
        ) : (
          /* Empty State */
          <div className="max-w-7xl mx-auto">
            <GlassCard className="p-12 text-center" delay={100}>
              <div className="w-20 h-20 mx-auto mb-6 rounded-2xl bg-gradient-to-br from-cyan-500/20 to-purple-500/20 flex items-center justify-center">
                <svg className="w-10 h-10 text-cyan-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" />
                </svg>
              </div>
              <h3 className="text-2xl font-semibold text-white mb-3">Select Two Cities to Compare</h3>
              <p className="text-white/50 max-w-md mx-auto">
                Choose cities from the dropdowns above to see a detailed side-by-side comparison of career opportunities, quality of life, environment, and more.
              </p>
            </GlassCard>
          </div>
        )}
      </div>
    </div>
  );
}

// Loading fallback component
function ComparePageLoading() {
  return (
    <div className="relative min-h-screen overflow-hidden">
      <div className="fixed inset-0 -z-10">
        <Image
          src="/background.png"
          alt="City Skyline"
          fill
          className="object-cover"
          priority
          quality={100}
        />
        <div className="absolute inset-0 bg-gradient-to-b from-[#0a0e27]/10 via-[#0f1420]/5 to-[#000000]/15" />
      </div>
      <div className="min-h-screen p-6 lg:p-8 flex items-center justify-center">
        <div className="text-center">
          <div className="w-16 h-16 mx-auto mb-4 rounded-2xl bg-gradient-to-br from-cyan-500/20 to-purple-500/20 flex items-center justify-center animate-pulse">
            <svg className="w-8 h-8 text-cyan-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" />
            </svg>
          </div>
          <p className="text-white/50">Loading comparison...</p>
        </div>
      </div>
    </div>
  );
}

// Default export wrapped in Suspense for useSearchParams
export default function ComparePage() {
  return (
    <Suspense fallback={<ComparePageLoading />}>
      <ComparePageContent />
    </Suspense>
  );
}