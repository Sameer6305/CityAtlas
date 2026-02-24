'use client';

import { useEffect } from 'react';

/**
 * City Page Error Boundary — handles errors on city detail routes.
 */
export default function CityError({
  error,
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  useEffect(() => {
    console.error('City page error:', error);
  }, [error]);

  return (
    <div className="min-h-[400px] flex items-center justify-center">
      <div className="text-center space-y-6 max-w-md px-6">
        <div className="w-16 h-16 mx-auto rounded-2xl bg-amber-500/10 border border-amber-500/20 flex items-center justify-center">
          <svg className="w-8 h-8 text-amber-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
          </svg>
        </div>
        <h2 className="text-xl font-bold text-white">City data unavailable</h2>
        <p className="text-white/50 text-sm">
          We couldn&apos;t load data for this city. The backend may be starting up or the city wasn&apos;t found.
        </p>
        <div className="flex gap-3 justify-center">
          <button
            onClick={reset}
            className="px-5 py-2.5 bg-cyan-500/20 hover:bg-cyan-500/30 text-cyan-400 rounded-xl border border-cyan-500/30 transition-all duration-300 font-medium text-sm"
          >
            Retry
          </button>
          <a
            href="/cities"
            className="px-5 py-2.5 bg-white/5 hover:bg-white/10 text-white/60 rounded-xl border border-white/10 transition-all duration-300 font-medium text-sm"
          >
            All Cities
          </a>
        </div>
      </div>
    </div>
  );
}
