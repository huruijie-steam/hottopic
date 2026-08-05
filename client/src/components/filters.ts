// 筛选状态定义与默认值(独立文件,满足 react-refresh 组件单导出约束)
export interface Filters {
  source: string;
  importance: string;
  keywordId: string;
  timeRange: string;
  isReal: string; // '' | 'true' | 'false'
  sortBy: string;
}

export const defaultFilters: Filters = {
  source: '',
  importance: '',
  keywordId: '',
  timeRange: '',
  isReal: '',
  sortBy: 'createdAt',
};

export const SOURCES = ['twitter', 'weibo', 'bilibili', 'hackernews', 'sogou', 'bing', 'google', 'duckduckgo'];
export const IMPORTANCES = [
  { value: 'urgent', label: '🔴 紧急' },
  { value: 'high', label: '🟠 高' },
  { value: 'medium', label: '🟡 中' },
  { value: 'low', label: '⚪ 低' },
];
export const SORTS = [
  { value: 'createdAt', label: '最新发现' },
  { value: 'publishedAt', label: '最新发布' },
  { value: 'importance', label: '重要程度' },
  { value: 'relevance', label: '相关性' },
  { value: 'heat', label: '综合热度' },
];
