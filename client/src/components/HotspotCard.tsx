import type { Hotspot } from '../types';
import { calcHeatScore, heatLevel, relativeTime } from '../utils/format';

const IMPORTANCE_STYLE: Record<string, string> = {
  urgent: 'bg-red-500/20 text-red-300 border-red-500/30',
  high: 'bg-orange-500/20 text-orange-300 border-orange-500/30',
  medium: 'bg-yellow-500/20 text-yellow-300 border-yellow-500/30',
  low: 'bg-gray-500/20 text-gray-300 border-gray-500/30',
};

const HEAT_COLOR: Record<string, string> = {
  爆: 'text-red-400',
  热: 'text-orange-400',
  温: 'text-yellow-400',
  凉: 'text-blue-400',
  冷: 'text-gray-500',
};

function Badge({ text, className }: { text: string; className?: string }) {
  return (
    <span className={`rounded-md border px-1.5 py-0.5 text-xs ${className ?? ''}`}>{text}</span>
  );
}

export default function HotspotCard({ hotspot, keywordText }: { hotspot: Hotspot; keywordText?: string }) {
  const heat = hotspot.heatScore ?? calcHeatScore(hotspot);
  const level = heatLevel(heat);

  return (
    <div className="group rounded-xl border border-white/10 bg-white/5 p-4 transition hover:border-indigo-400/40 hover:bg-white/10">
      <div className="flex items-start justify-between gap-3">
        <div className="min-w-0">
          <a
            href={hotspot.url}
            target="_blank"
            rel="noreferrer"
            className="line-clamp-2 font-medium text-gray-100 hover:text-indigo-300"
          >
            {hotspot.title}
          </a>
        </div>
        <span className={`shrink-0 text-lg font-bold ${HEAT_COLOR[level] ?? ''}`}>{heat}</span>
      </div>

      {hotspot.summary && <p className="mt-2 text-sm text-gray-400 line-clamp-2">{hotspot.summary}</p>}

      <div className="mt-3 flex flex-wrap items-center gap-2 text-xs text-gray-400">
        <Badge text={hotspot.source} className="bg-indigo-500/20 text-indigo-300 border-indigo-500/30" />
        <Badge text={hotspot.importance} className={IMPORTANCE_STYLE[hotspot.importance] ?? IMPORTANCE_STYLE.low} />
        <span className="text-gray-500">相关 {hotspot.relevance}/100</span>
        {keywordText && <span className="text-gray-500">词: {keywordText}</span>}
        <span className="ml-auto">{relativeTime(hotspot.createdAt)}</span>
      </div>
    </div>
  );
}
