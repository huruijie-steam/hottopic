import { Flame, TrendingUp, AlertTriangle, ListChecks } from 'lucide-react';

interface Stats {
  totalHotspots: number;
  todayNew: number;
  urgent: number;
  keywordCount: number;
}

export default function StatCards({ stats }: { stats: Stats }) {
  const cards = [
    { label: '总热点', value: stats.totalHotspots, icon: Flame, color: 'text-indigo-400' },
    { label: '今日新增', value: stats.todayNew, icon: TrendingUp, color: 'text-emerald-400' },
    { label: '紧急热点', value: stats.urgent, icon: AlertTriangle, color: 'text-red-400' },
    { label: '监控词数', value: stats.keywordCount, icon: ListChecks, color: 'text-purple-400' },
  ];

  return (
    <div className="grid grid-cols-2 gap-3 md:grid-cols-4">
      {cards.map((c) => (
        <div key={c.label} className="flex items-center gap-3 rounded-xl border border-white/10 bg-white/5 p-4">
          <c.icon className={`h-6 w-6 ${c.color}`} />
          <div>
            <div className="text-2xl font-bold text-gray-100">{c.value}</div>
            <div className="text-xs text-gray-400">{c.label}</div>
          </div>
        </div>
      ))}
    </div>
  );
}
