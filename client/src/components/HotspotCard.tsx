import { useState } from 'react';
import {
  ChevronDown,
  ChevronUp,
  ExternalLink,
  Eye,
  Flame,
  Heart,
  MessageCircle,
  Repeat2,
  ShieldCheck,
  ThumbsUp,
  User,
} from 'lucide-react';
import type { Hotspot } from '../types';
import { calcHeatScore, heatLevel, relativeTime } from '../utils/format';
import { MovingBorder } from './ui/moving-border';

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

const IMPORTANCE_LABEL: Record<string, string> = {
  urgent: '紧急',
  high: '高',
  medium: '中',
  low: '低',
};

function Badge({ text, className }: { text: string; className?: string }) {
  return <span className={`rounded-md border px-1.5 py-0.5 text-xs ${className ?? ''}`}>{text}</span>;
}

function StatItem({ icon: Icon, value, title }: { icon: typeof Eye; value?: number | null; title: string }) {
  if (!value) return null;
  return (
    <span title={title} className="flex items-center gap-1 text-gray-400">
      <Icon className="h-3.5 w-3.5" />
      {value >= 10000 ? `${(value / 10000).toFixed(1)}w` : value}
    </span>
  );
}

export default function HotspotCard({ hotspot, keywordText }: { hotspot: Hotspot; keywordText?: string }) {
  const [expanded, setExpanded] = useState(false);
  const heat = hotspot.heatScore ?? calcHeatScore(hotspot);
  const level = heatLevel(heat);
  const hasAuthor = hotspot.authorName || hotspot.authorUsername;
  const hasStats = hotspot.likeCount || hotspot.retweetCount || hotspot.commentCount ||
    hotspot.viewCount || hotspot.danmakuCount;

  return (
    <div className="group relative">
      <MovingBorder duration={4000} containerClassName="rounded-xl">
        <div className="w-full rounded-[11px] bg-gray-900/90 p-4">
          {/* 标题 + 热度 */}
          <div className="flex items-start justify-between gap-3">
            <a
              href={hotspot.url}
              target="_blank"
              rel="noreferrer"
              className="line-clamp-2 min-w-0 font-medium text-gray-100 hover:text-indigo-300"
            >
              {hotspot.title}
            </a>
            <span className={`flex shrink-0 items-center gap-1 text-lg font-bold ${HEAT_COLOR[level] ?? ''}`}>
              <Flame className="h-4 w-4" />
              {heat}
            </span>
          </div>

          {/* 摘要 */}
          {hotspot.summary && <p className="mt-2 text-sm text-gray-400 line-clamp-2">{hotspot.summary}</p>}

          {/* 来源/重要度/关键词/时间 */}
          <div className="mt-3 flex flex-wrap items-center gap-2 text-xs text-gray-400">
            <Badge text={hotspot.source} className="bg-indigo-500/20 text-indigo-300 border-indigo-500/30" />
            <Badge
              text={IMPORTANCE_LABEL[hotspot.importance] ?? hotspot.importance}
              className={IMPORTANCE_STYLE[hotspot.importance] ?? IMPORTANCE_STYLE.low}
            />
            <span className="text-gray-500">相关 {hotspot.relevance}/100</span>
            {keywordText && <span className="text-gray-500">词: {keywordText}</span>}
            <span className="ml-auto">{relativeTime(hotspot.createdAt)}</span>
          </div>

          {/* 展开详情:作者 + 互动数据 + AI 分析 */}
          {expanded && (
            <div className="mt-3 space-y-3 border-t border-white/10 pt-3 text-sm">
              {/* 作者信息 */}
              {hasAuthor && (
                <div className="flex items-center gap-3">
                  {hotspot.authorAvatar ? (
                    <img
                      src={hotspot.authorAvatar}
                      alt={hotspot.authorName ?? ''}
                      className="h-8 w-8 rounded-full object-cover"
                      referrerPolicy="no-referrer"
                    />
                  ) : (
                    <div className="flex h-8 w-8 items-center justify-center rounded-full bg-indigo-500/30">
                      <User className="h-4 w-4 text-indigo-300" />
                    </div>
                  )}
                  <div className="min-w-0">
                    <div className="flex items-center gap-1 font-medium text-gray-200">
                      {hotspot.authorName || '未知作者'}
                      {hotspot.authorVerified && <ShieldCheck className="h-3.5 w-3.5 text-sky-400" />}
                      {hotspot.authorUsername && (
                        <span className="text-xs text-gray-500">@{hotspot.authorUsername}</span>
                      )}
                    </div>
                    {hotspot.authorFollowers != null && (
                      <div className="text-xs text-gray-500">粉丝 {(hotspot.authorFollowers / 10000).toFixed(1)}w</div>
                    )}
                  </div>
                </div>
              )}

              {/* 互动数据 */}
              {hasStats && (
                <div className="flex flex-wrap gap-4">
                  <StatItem icon={Heart} value={hotspot.likeCount} title="点赞" />
                  <StatItem icon={Repeat2} value={hotspot.retweetCount} title="转发" />
                  <StatItem icon={MessageCircle} value={hotspot.commentCount} title="评论" />
                  <StatItem icon={Eye} value={hotspot.viewCount} title="浏览" />
                  <StatItem icon={ThumbsUp} value={hotspot.danmakuCount} title="弹幕" />
                </div>
              )}

              {/* AI 分析 */}
              {hotspot.relevanceReason && (
                <div className="rounded-lg bg-white/5 p-2.5 text-xs text-gray-400">
                  <span className="text-gray-500">AI 分析:</span> {hotspot.relevanceReason}
                </div>
              )}
              {hotspot.content && (
                <p className="line-clamp-6 text-gray-400">{hotspot.content}</p>
              )}

              {/* 原文链接 + 发布时间 */}
              <div className="flex items-center justify-between text-xs text-gray-500">
                <a href={hotspot.url} target="_blank" rel="noreferrer" className="flex items-center gap-1 text-indigo-400 hover:underline">
                  <ExternalLink className="h-3.5 w-3.5" /> 查看原文
                </a>
                {hotspot.publishedAt && <span>发布于 {relativeTime(hotspot.publishedAt)}</span>}
              </div>
            </div>
          )}

          {/* 展开按钮 */}
          <button
            className="mt-2 flex w-full items-center justify-center gap-1 rounded-lg py-1 text-xs text-gray-500 hover:bg-white/5 hover:text-gray-300"
            onClick={() => setExpanded((e) => !e)}
          >
            {expanded ? <ChevronUp className="h-3.5 w-3.5" /> : <ChevronDown className="h-3.5 w-3.5" />}
            {expanded ? '收起详情' : '展开详情'}
          </button>
        </div>
      </MovingBorder>
    </div>
  );
}
