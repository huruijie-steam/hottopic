import { useState } from 'react';
import { Search } from 'lucide-react';
import type { SearchResult } from '../types';
import { hotspotsApi } from '../services/api';
import { relativeTime } from '../utils/format';

export default function SearchPage() {
  const [query, setQuery] = useState('');
  const [results, setResults] = useState<SearchResult[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [searched, setSearched] = useState(false);

  const doSearch = async () => {
    if (!query.trim()) return;
    setLoading(true);
    setError('');
    try {
      const list = await hotspotsApi.search(query.trim());
      setResults(list);
      setSearched(true);
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="space-y-4">
      <div className="flex gap-2">
        <input
          className="flex-1 rounded-lg border border-white/10 bg-white/5 px-4 py-2.5 text-gray-200 outline-none focus:border-indigo-400"
          placeholder="全网搜索任意关键词,如: GPT-5 发布"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          onKeyDown={(e) => e.key === 'Enter' && doSearch()}
        />
        <button
          className="flex items-center gap-2 rounded-lg bg-indigo-500 px-5 text-sm font-medium text-white hover:bg-indigo-400 disabled:opacity-50"
          onClick={doSearch}
          disabled={loading || !query.trim()}
        >
          <Search className="h-4 w-4" /> 搜索
        </button>
      </div>

      {error && <div className="rounded-lg bg-red-500/10 p-3 text-sm text-red-300">{error}</div>}
      {loading && <div className="py-8 text-center text-gray-400">聚合搜索中(Bing + HackerNews)...</div>}
      {!loading && searched && results.length === 0 && (
        <div className="py-16 text-center text-gray-500">没有找到结果</div>
      )}

      <div className="space-y-3">
        {results.map((r, i) => (
          <a
            key={i}
            href={r.url}
            target="_blank"
            rel="noreferrer"
            className="block rounded-xl border border-white/10 bg-white/5 p-4 transition hover:border-indigo-400/40"
          >
            <div className="font-medium text-gray-100 hover:text-indigo-300">{r.title}</div>
            {r.content && <div className="mt-1 text-sm text-gray-400 line-clamp-2">{r.content}</div>}
            <div className="mt-2 flex gap-3 text-xs text-gray-500">
              <span className="text-indigo-300">{r.source}</span>
              {r.score != null && <span>热度 {r.score}</span>}
              {r.publishedAt && <span>{relativeTime(r.publishedAt)}</span>}
            </div>
          </a>
        ))}
      </div>
    </div>
  );
}
