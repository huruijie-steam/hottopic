import { useCallback, useEffect, useRef, useState } from 'react';
import { Bell, Flame, RefreshCw, Search, Settings, Target } from 'lucide-react';
import type { Hotspot, KeywordVO, Notification } from './types';
import { keywordsApi, notificationsApi, opsApi } from './services/api';
import { socket } from './services/socket';
import DashboardPage from './pages/DashboardPage';
import KeywordsPage from './pages/KeywordsPage';
import SearchPage from './pages/SearchPage';
import SettingsPage from './pages/SettingsPage';
import { relativeTime } from './utils/format';

type Tab = 'dashboard' | 'keywords' | 'search' | 'settings';

const TABS: { id: Tab; label: string; icon: typeof Flame }[] = [
  { id: 'dashboard', label: '热点雷达', icon: Flame },
  { id: 'keywords', label: '监控词', icon: Target },
  { id: 'search', label: '搜索', icon: Search },
  { id: 'settings', label: '设置', icon: Settings },
];

export default function App() {
  const [tab, setTab] = useState<Tab>('dashboard');
  const [keywordVOs, setKeywordVOs] = useState<KeywordVO[]>([]);
  const [newHotspot, setNewHotspot] = useState<Hotspot | null>(null);
  const [notifications, setNotifications] = useState<Notification[]>([]);
  const [unread, setUnread] = useState(0);
  const [showNotif, setShowNotif] = useState(false);
  const [checking, setChecking] = useState(false);
  const [toast, setToast] = useState('');
  const notifRef = useRef<HTMLDivElement>(null);

  const loadKeywords = useCallback(async () => {
    try {
      setKeywordVOs(await keywordsApi.list());
    } catch {
      // 后端未启动时静默
    }
  }, []);

  const loadNotifications = useCallback(async () => {
    try {
      const [page, count] = await Promise.all([notificationsApi.list(1, 10), notificationsApi.unreadCount()]);
      setNotifications(page.records);
      setUnread(count.count);
    } catch {
      // 忽略
    }
  }, []);

  // 初始加载 + WebSocket 接入
  useEffect(() => {
    loadKeywords();
    loadNotifications();
    socket.connect();

    const offHotspot = socket.onHotspotNew((hotspot) => {
      setNewHotspot(hotspot);
      setToast(`🔥 新热点: ${hotspot.title.slice(0, 40)}`);
      setTimeout(() => setToast(''), 4000);
    });
    const offNotif = socket.onNotification(() => {
      setUnread((u) => u + 1);
      loadNotifications();
    });

    return () => {
      offHotspot();
      offNotif();
      socket.disconnect();
    };
  }, [loadKeywords, loadNotifications]);

  // 点击外部关闭通知下拉
  useEffect(() => {
    const onClick = (e: MouseEvent) => {
      if (notifRef.current && !notifRef.current.contains(e.target as Node)) {
        setShowNotif(false);
      }
    };
    document.addEventListener('mousedown', onClick);
    return () => document.removeEventListener('mousedown', onClick);
  }, []);

  const triggerCheck = async () => {
    setChecking(true);
    try {
      await opsApi.triggerCheck();
      setToast('✅ 巡检完成,已更新热点');
      setTimeout(() => setToast(''), 4000);
    } catch (e) {
      setToast(`❌ ${(e as Error).message}`);
    } finally {
      setChecking(false);
    }
  };

  const markAllRead = async () => {
    await notificationsApi.markRead();
    setUnread(0);
    setNotifications((prev) => prev.map((n) => ({ ...n, isRead: true })));
  };

  return (
    <div className="min-h-screen bg-gray-950 text-gray-100">
      {/* 背景光效 */}
      <div className="pointer-events-none fixed inset-0 overflow-hidden">
        <div className="absolute -top-40 left-1/4 h-96 w-96 rounded-full bg-indigo-600/20 blur-3xl" />
        <div className="absolute top-1/3 right-1/5 h-80 w-80 rounded-full bg-pink-600/10 blur-3xl" />
      </div>

      <div className="relative mx-auto max-w-6xl px-4 pb-16">
        {/* 顶栏 */}
        <header className="sticky top-0 z-20 flex items-center justify-between border-b border-white/10 bg-gray-950/80 py-3 backdrop-blur">
          <div className="flex items-center gap-2">
            <Flame className="h-6 w-6 text-orange-400" />
            <span className="text-lg font-bold tracking-wide">
              Hot<span className="text-orange-400">Topic</span>
            </span>
          </div>
          <div className="flex items-center gap-2">
            <button
              className="flex items-center gap-1 rounded-lg bg-indigo-500 px-3 py-1.5 text-sm text-white hover:bg-indigo-400 disabled:opacity-50"
              onClick={triggerCheck}
              disabled={checking}
            >
              <RefreshCw className={`h-4 w-4 ${checking ? 'animate-spin' : ''}`} /> 立即扫描
            </button>
            <div className="relative" ref={notifRef}>
              <button
                className="relative rounded-lg p-2 text-gray-300 hover:bg-white/10"
                onClick={() => setShowNotif((s) => !s)}
              >
                <Bell className="h-5 w-5" />
                {unread > 0 && (
                  <span className="absolute -top-0.5 -right-0.5 flex h-4 min-w-4 items-center justify-center rounded-full bg-red-500 px-1 text-[10px] font-bold text-white">
                    {unread > 99 ? '99+' : unread}
                  </span>
                )}
              </button>
              {showNotif && (
                <div className="absolute right-0 mt-2 w-80 rounded-xl border border-white/10 bg-gray-900 shadow-xl">
                  <div className="flex items-center justify-between border-b border-white/10 px-3 py-2">
                    <span className="text-sm font-medium">通知</span>
                    <button className="text-xs text-indigo-400 hover:underline" onClick={markAllRead}>
                      全部已读
                    </button>
                  </div>
                  <div className="max-h-80 overflow-y-auto">
                    {notifications.length === 0 && (
                      <div className="py-8 text-center text-sm text-gray-500">暂无通知</div>
                    )}
                    {notifications.map((n) => (
                      <div key={n.id} className={`border-b border-white/5 px-3 py-2 text-sm ${n.isRead ? 'text-gray-400' : 'text-gray-200'}`}>
                        <div className="line-clamp-1">{n.title}</div>
                        <div className="text-xs text-gray-500">{relativeTime(n.createdAt)}</div>
                      </div>
                    ))}
                  </div>
                </div>
              )}
            </div>
          </div>
        </header>

        {/* Tab 导航 */}
        <nav className="mt-4 flex gap-1 rounded-xl border border-white/10 bg-white/5 p-1">
          {TABS.map((t) => (
            <button
              key={t.id}
              className={`flex flex-1 items-center justify-center gap-1.5 rounded-lg py-2 text-sm transition ${
                tab === t.id ? 'bg-indigo-500/20 text-indigo-300' : 'text-gray-400 hover:text-gray-200'
              }`}
              onClick={() => setTab(t.id)}
            >
              <t.icon className="h-4 w-4" /> {t.label}
            </button>
          ))}
        </nav>

        {/* 页面内容 */}
        <main className="mt-4">
          {tab === 'dashboard' && (
            <DashboardPage keywords={keywordVOs.map((vo) => vo.keyword)} newHotspot={newHotspot} />
          )}
          {tab === 'keywords' && <KeywordsPage items={keywordVOs} onChange={loadKeywords} />}
          {tab === 'search' && <SearchPage />}
          {tab === 'settings' && <SettingsPage />}
        </main>
      </div>

      {/* Toast */}
      {toast && (
        <div className="fixed bottom-6 left-1/2 z-30 -translate-x-1/2 rounded-xl border border-white/10 bg-gray-900/95 px-4 py-2.5 text-sm shadow-xl">
          {toast}
        </div>
      )}
    </div>
  );
}
