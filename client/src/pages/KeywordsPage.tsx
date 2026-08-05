import { useState } from 'react';
import { Plus, Trash2, Power } from 'lucide-react';
import type { KeywordVO } from '../types';
import { keywordsApi } from '../services/api';

interface Props {
  items: KeywordVO[];
  onChange: () => void;
}

export default function KeywordsPage({ items, onChange }: Props) {
  const [text, setText] = useState('');
  const [category, setCategory] = useState('');
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  const add = async () => {
    if (!text.trim()) return;
    setBusy(true);
    setError('');
    try {
      await keywordsApi.create(text.trim(), category.trim() || undefined);
      setText('');
      setCategory('');
      onChange();
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setBusy(false);
    }
  };

  const toggle = async (vo: KeywordVO) => {
    await keywordsApi.update(vo.keyword.id, { isActive: !vo.keyword.isActive });
    onChange();
  };

  const remove = async (vo: KeywordVO) => {
    if (!confirm(`删除监控词「${vo.keyword.text}」?(已收集的热点会保留)`)) return;
    await keywordsApi.remove(vo.keyword.id);
    onChange();
  };

  return (
    <div className="space-y-4">
      <div className="rounded-xl border border-white/10 bg-white/5 p-4">
        <div className="flex flex-wrap gap-2">
          <input
            className="flex-1 min-w-48 rounded-lg border border-white/10 bg-white/5 px-3 py-2 text-sm text-gray-200 outline-none focus:border-indigo-400"
            placeholder="输入要监控的关键词,如: Claude Sonnet 4.6"
            value={text}
            onChange={(e) => setText(e.target.value)}
            onKeyDown={(e) => e.key === 'Enter' && add()}
          />
          <input
            className="w-32 rounded-lg border border-white/10 bg-white/5 px-3 py-2 text-sm text-gray-200 outline-none focus:border-indigo-400"
            placeholder="分类(可选)"
            value={category}
            onChange={(e) => setCategory(e.target.value)}
          />
          <button
            className="flex items-center gap-1 rounded-lg bg-indigo-500 px-4 py-2 text-sm font-medium text-white hover:bg-indigo-400 disabled:opacity-50"
            onClick={add}
            disabled={busy || !text.trim()}
          >
            <Plus className="h-4 w-4" /> 添加
          </button>
        </div>
        {error && <div className="mt-2 text-sm text-red-300">{error}</div>}
      </div>

      {items.length === 0 ? (
        <div className="py-16 text-center text-gray-500">还没有监控词,添加一个开始监控吧 👆</div>
      ) : (
        <div className="grid gap-3 md:grid-cols-2">
          {items.map((vo) => (
            <div
              key={vo.keyword.id}
              className={`rounded-xl border p-4 ${
                vo.keyword.isActive ? 'border-white/10 bg-white/5' : 'border-white/5 bg-white/2 opacity-60'
              }`}
            >
              <div className="flex items-center justify-between">
                <div className="min-w-0">
                  <div className="font-medium text-gray-100">{vo.keyword.text}</div>
                  <div className="mt-1 text-xs text-gray-500">
                    {vo.keyword.category && <span className="mr-2">分类: {vo.keyword.category}</span>}
                    热点 {vo.hotspotCount} 条
                  </div>
                </div>
                <div className="flex gap-1">
                  <button
                    title={vo.keyword.isActive ? '暂停' : '激活'}
                    className="rounded-lg p-2 text-gray-400 hover:bg-white/10"
                    onClick={() => toggle(vo)}
                  >
                    <Power className={`h-4 w-4 ${vo.keyword.isActive ? 'text-emerald-400' : ''}`} />
                  </button>
                  <button
                    title="删除"
                    className="rounded-lg p-2 text-gray-400 hover:bg-red-500/20 hover:text-red-300"
                    onClick={() => remove(vo)}
                  >
                    <Trash2 className="h-4 w-4" />
                  </button>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
