import type { Keyword } from '../types';
import { SOURCES, IMPORTANCES, SORTS, defaultFilters, type Filters } from './filters';

interface Props {
  filters: Filters;
  onChange: (filters: Filters) => void;
  keywords: Keyword[];
}

function Select({ label, value, options, onChange }: {
  label: string;
  value: string;
  options: { value: string; label: string }[];
  onChange: (v: string) => void;
}) {
  return (
    <select
      className="rounded-lg border border-white/10 bg-white/5 px-3 py-1.5 text-sm text-gray-200 outline-none focus:border-indigo-400"
      value={value}
      onChange={(e) => onChange(e.target.value)}
    >
      <option value="">{label}</option>
      {options.map((o) => (
        <option key={o.value} value={o.value} className="bg-gray-900">
          {o.label}
        </option>
      ))}
    </select>
  );
}

export default function FilterSortBar({ filters, onChange, keywords }: Props) {
  const set = (patch: Partial<Filters>) => onChange({ ...filters, ...patch });
  const activeCount =
    [filters.source, filters.importance, filters.keywordId, filters.timeRange, filters.isReal].filter(Boolean).length;

  return (
    <div className="flex flex-wrap items-center gap-2 rounded-xl border border-white/10 bg-white/5 p-3">
      <Select label="来源" value={filters.source} options={SOURCES.map((s) => ({ value: s, label: s }))}
        onChange={(v) => set({ source: v })} />
      <Select label="重要程度" value={filters.importance} options={IMPORTANCES} onChange={(v) => set({ importance: v })} />
      <Select label="关键词" value={filters.keywordId}
        options={keywords.filter((k) => k.isActive).map((k) => ({ value: k.id, label: k.text }))}
        onChange={(v) => set({ keywordId: v })} />
      <Select label="时间范围" value={filters.timeRange}
        options={[
          { value: 'h1', label: '1 小时内' },
          { value: 'h24', label: '今天' },
          { value: 'd7', label: '7 天内' },
          { value: 'd30', label: '30 天内' },
        ]}
        onChange={(v) => set({ timeRange: v })} />
      <Select label="真实性" value={filters.isReal}
        options={[
          { value: 'true', label: '真实' },
          { value: 'false', label: '疑似虚假' },
        ]}
        onChange={(v) => set({ isReal: v })} />
      <div className="mx-1 h-6 w-px bg-white/10" />
      <Select label="排序" value={filters.sortBy} options={SORTS} onChange={(v) => set({ sortBy: v })} />
      {activeCount > 0 && (
        <button
          className="rounded-lg bg-white/10 px-3 py-1.5 text-sm text-gray-300 hover:bg-white/20"
          onClick={() => onChange(defaultFilters)}
        >
          重置 ({activeCount})
        </button>
      )}
    </div>
  );
}
