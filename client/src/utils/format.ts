import type { Hotspot } from '../types';

// 热度计算(与后端 HeatScoreUtils 公式一致)
export function calcHeatScore(h: Hotspot): number {
  const weighted =
    (h.likeCount ?? 0) * 2 +
    (h.retweetCount ?? 0) * 3 +
    (h.commentCount ?? 0) * 5 +
    (h.danmakuCount ?? 0) * 2 +
    (h.viewCount ?? 0) * 0.01;
  if (weighted <= 0) return 0;
  return Math.min(100, Math.round(Math.log10(1 + weighted) * 10));
}

export function heatLevel(score: number): string {
  if (score >= 90) return '爆';
  if (score >= 70) return '热';
  if (score >= 50) return '温';
  if (score >= 30) return '凉';
  return '冷';
}

// 相对时间显示
export function relativeTime(iso: string | null | undefined): string {
  if (!iso) return '';
  const time = new Date(iso).getTime();
  const diff = Date.now() - time;
  if (diff < 60_000) return '刚刚';
  if (diff < 3_600_000) return `${Math.floor(diff / 60_000)} 分钟前`;
  if (diff < 86_400_000) return `${Math.floor(diff / 3_600_000)} 小时前`;
  return `${Math.floor(diff / 86_400_000)} 天前`;
}
