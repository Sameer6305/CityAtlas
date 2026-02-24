import Link from 'next/link';

/**
 * 404 — City not found.
 */
export default function CityNotFound() {
  return (
    <div className="min-h-[400px] flex items-center justify-center">
      <div className="text-center space-y-6 max-w-md px-6">
        <div className="text-6xl font-bold text-white/10">404</div>
        <h2 className="text-xl font-bold text-white">City not found</h2>
        <p className="text-white/50 text-sm">
          The city you&apos;re looking for doesn&apos;t exist or hasn&apos;t been indexed yet.
        </p>
        <Link
          href="/cities"
          className="inline-block px-5 py-2.5 bg-cyan-500/20 hover:bg-cyan-500/30 text-cyan-400 rounded-xl border border-cyan-500/30 transition-all duration-300 font-medium text-sm"
        >
          Browse Cities
        </Link>
      </div>
    </div>
  );
}
