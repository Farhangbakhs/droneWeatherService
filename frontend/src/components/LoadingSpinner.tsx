export default function LoadingSpinner() {
  return (
    <div className="card flex flex-col items-center justify-center py-12">
      <div className="relative">
        <div className="h-16 w-16 border-4 border-retro-border"></div>
        <div className="absolute top-0 left-0 h-16 w-16 border-4 border-primary-500 border-t-transparent animate-spin"></div>
      </div>
      <p className="mt-6 text-retro-accent font-bold font-mono uppercase tracking-wider">&gt; ANALYZING WEATHER CONDITIONS...</p>
      <p className="mt-2 text-sm text-retro-text font-mono uppercase tracking-wider opacity-70">[CALCULATING OPTIMAL DELIVERY PATH]</p>
    </div>
  );
}
