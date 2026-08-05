import type { Hotspot, HotspotQuery, Keyword, KeywordVO, Notification, PageResult, SearchResult } from '../types';

// 统一请求封装:解析错误、JSON 序列化
async function request<T>(path: string, options?: RequestInit): Promise<T> {
  const res = await fetch(`/api${path}`, {
    headers: { 'Content-Type': 'application/json' },
    ...options,
  });
  if (!res.ok) {
    let message = `请求失败 (${res.status})`;
    try {
      const body = await res.json();
      if (body?.error) message = body.error;
    } catch {
      // 忽略解析失败
    }
    throw new Error(message);
  }
  return res.json() as Promise<T>;
}

export const keywordsApi = {
  list: () => request<KeywordVO[]>('/keywords'),
  create: (text: string, category?: string) =>
    request<Keyword>('/keywords', { method: 'POST', body: JSON.stringify({ text, category }) }),
  update: (id: string, patch: { text?: string; category?: string; isActive?: boolean }) =>
    request<Keyword>(`/keywords/${id}`, { method: 'PUT', body: JSON.stringify(patch) }),
  remove: (id: string) => request<{ message: string }>(`/keywords/${id}`, { method: 'DELETE' }),
};

export const hotspotsApi = {
  list: (query: HotspotQuery) => {
    const params = new URLSearchParams();
    params.set('page', String(query.page));
    params.set('size', String(query.size));
    if (query.source) params.set('source', query.source);
    if (query.importance) params.set('importance', query.importance);
    if (query.keywordId) params.set('keywordId', query.keywordId);
    if (query.timeRange) params.set('timeRange', query.timeRange);
    if (query.isReal !== undefined) params.set('isReal', String(query.isReal));
    if (query.sortBy) params.set('sortBy', query.sortBy);
    return request<PageResult<Hotspot>>(`/hotspots?${params.toString()}`);
  },
  search: (query: string) =>
    request<SearchResult[]>('/hotspots/search', { method: 'POST', body: JSON.stringify({ query }) }),
};

export const notificationsApi = {
  list: (page = 1, size = 20) => request<PageResult<Notification>>(`/notifications?page=${page}&size=${size}`),
  unreadCount: () => request<{ count: number }>('/notifications/unread-count'),
  markRead: (id?: string) =>
    request<{ message: string }>('/notifications/read', {
      method: 'POST',
      body: JSON.stringify(id ? { id } : {}),
    }),
};

export const settingsApi = {
  getAll: () => request<Record<string, string>>('/settings'),
  update: (settings: Record<string, string>) =>
    request<{ message: string }>('/settings', { method: 'PUT', body: JSON.stringify(settings) }),
};

export const opsApi = {
  triggerCheck: () => request<{ message: string }>('/check-hotspots', { method: 'POST' }),
};
