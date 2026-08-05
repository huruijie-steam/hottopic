import { useEffect, useState } from 'react';
import type { Hotspot, HotspotQuery, Keyword } from '../types';
import { hotspotsApi } from '../services/api';
import FilterSortBar from '../components/FilterSortBar';
import { defaultFilters, type Filters } from '../components/filters';
import HotspotCard from '../components/HotspotCard';
import StatCards from '../components/StatCards';

interface Props {
  keywords: Keyword[];
  newHotspot: Hotspot | null;
}

export default function DashboardPage({ keywords, newHotspot }: Props) {
  const [filters, setFilters] = useState<Filters>(defaultFilters);
  const [records, setRecords] = useState<Hotspot[]>([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [stats, setStats] = useState({ totalHotspots: 0, todayNew: 0, urgent: 0, keywordCount: 0 });

  const keywordMap = new Map(keywords.map((k) => [k.id, k.text]));

  const load = async (query: HotspotQuery, isFirst = false) => {
    setLoading(true);
    try {
      const result = await hotspotsApi.list(query);
      setRecords(result.records);
      setTotal(result.total);
      if (isFirst) {
        setStats({
          totalHotspots: result.total,
          todayNew: 0,
          urgent: result.records.filter((r) => r.importance === 'urgent' || r.importance === 'high').length,
          keywordCount: keywords.length,
        });
      }
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setLoading(false);
    }
  };

  // 首次加载 + 筛选/排序/翻页变化
  useEffect(() => {
    const query: HotspotQuery = {
      page,
      size: 20,
      source: filters.source || undefined,
      importance: filters.importance || undefined,
      keywordId: filters.keywordId || undefined,
      timeRange: filters.timeRange || undefined,
      isReal: filters.isReal === '' ? undefined : filters.isReal === 'true',
      sortBy: filters.sortBy,
    };
    load(query, true);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [page, filters]);

  // WebSocket 新热点:插入列表头部并刷新统计
  useEffect(() => {
    if (newHotspot) {
      setRecords((prev) => [newHotspot, ...prev.filter((r) => r.id !== newHotspot.id)].slice(0, 20));
      setStats((s) => ({ ...s, totalHotspots: s.totalHotspots + 1, todayNew: s.todayNew + 1 }));
      setTotal((t) => t + 1);
    }
  }, [newHotspot]);

  const totalPages = Math.max(1, Math.ceil(total / 20));

  return (
    <div className="space-y-4">
      <StatCards stats={stats} />
      <FilterSortBar filters={filters} onChange={(f) => { setFilters(f); setPage(1); }} keywords={keywords} />

      {error && <div className="rounded-lg bg-red-500/10 p-3 text-sm text-red-300">{error}</div>}
      {loading && <div className="py-8 text-center text-gray-400">加载中...</div>}
      {!loading && records.length === 0 && (
        <div className="py-16 text-center text-gray-500">
          暂无热点 —— 添加监控词后,系统每 30 分钟自动巡检 🔍
        </div>
      )}

      <div className="space-y-3">
        {records.map((h) => (
          <HotspotCard key={h.id} hotspot={h} keywordText={h.keywordId ? keywordMap.get(h.keywordId) : undefined} />
        ))}
      </div>

      {totalPages > 1 && (
        <div className="flex items-center justify-center gap-3 py-4 text-sm text-gray-400">
          <button
            className="rounded-lg bg-white/10 px-3 py-1.5 disabled:opacity-30"
            disabled={page <= 1}
            onClick={() => setPage((p) => p - 1)}
          >
            上一页
          </button>
          <span>
            {page} / {totalPages}(共 {total} 条)
          </span>
          <button
            className="rounded-lg bg-white/10 px-3 py-1.5 disabled:opacity-30"
            disabled={page >= totalPages}
            onClick={() => setPage((p) => p + 1)}
          >
            下一页
          </button>
        </div>
      )}
    </div>
  );
}
