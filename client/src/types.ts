// 与后端一致的共享类型(对应 server 的 entity/dto)

export interface Keyword {
  id: string;
  text: string;
  category?: string | null;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface KeywordVO {
  keyword: Keyword;
  hotspotCount: number;
}

export interface Hotspot {
  id: string;
  title: string;
  content?: string | null;
  url: string;
  source: string;
  sourceId?: string | null;
  isReal: boolean;
  relevance: number;
  relevanceReason?: string | null;
  keywordMentioned?: boolean | null;
  importance: string; // low/medium/high/urgent
  summary?: string | null;
  viewCount?: number | null;
  likeCount?: number | null;
  retweetCount?: number | null;
  replyCount?: number | null;
  commentCount?: number | null;
  quoteCount?: number | null;
  danmakuCount?: number | null;
  authorName?: string | null;
  authorUsername?: string | null;
  authorFollowers?: number | null;
  publishedAt?: string | null;
  createdAt: string;
  keywordId?: string | null;
  heatScore?: number;
}

export interface Notification {
  id: string;
  type: string;
  title: string;
  content?: string | null;
  isRead: boolean;
  hotspotId?: string | null;
  createdAt: string;
}

export interface PageResult<T> {
  records: T[];
  total: number;
  page: number;
  size: number;
}

export interface HotspotQuery {
  page: number;
  size: number;
  source?: string;
  importance?: string;
  keywordId?: string;
  timeRange?: string;
  isReal?: boolean;
  sortBy?: string;
}

export interface SearchResult {
  title: string;
  content?: string;
  url: string;
  source: string;
  sourceId?: string;
  publishedAt?: string | null;
  viewCount?: number | null;
  likeCount?: number | null;
  commentCount?: number | null;
  authorName?: string | null;
  score?: number | null;
}

// WebSocket 推送消息
export interface WsMessage<T = unknown> {
  type: string; // hotspot:new | notification
  data: T;
}
