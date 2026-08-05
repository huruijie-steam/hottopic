import type { Hotspot, Notification, WsMessage } from '../types';

// 原生 WebSocket 封装:同源 /ws,自动重连
type Handler<T> = (data: T) => void;

class HotTopicSocket {
  private socket: WebSocket | null = null;
  private handlers = new Map<string, Set<(data: unknown) => void>>();
  private subscribed = new Set<string>();
  private reconnectTimer: ReturnType<typeof setTimeout> | null = null;
  private closedByUser = false;

  connect() {
    this.closedByUser = false;
    const protocol = window.location.protocol === 'https:' ? 'wss' : 'ws';
    this.socket = new WebSocket(`${protocol}://${window.location.host}/ws`);

    this.socket.onopen = () => {
      // 重连后恢复订阅
      if (this.subscribed.size > 0) {
        this.emit({ action: 'subscribe', keywords: [...this.subscribed] });
      }
    };

    this.socket.onmessage = (event) => {
      try {
        const message = JSON.parse(event.data) as WsMessage;
        const set = this.handlers.get(message.type);
        if (set) {
          set.forEach((fn) => fn(message.data));
        }
      } catch {
        // 忽略无法解析的消息
      }
    };

    this.socket.onclose = () => {
      if (!this.closedByUser) {
        this.reconnectTimer = setTimeout(() => this.connect(), 3000);
      }
    };
  }

  private emit(payload: object) {
    if (this.socket?.readyState === WebSocket.OPEN) {
      this.socket.send(JSON.stringify(payload));
    }
  }

  subscribe(keywords: string[]) {
    keywords.forEach((kw) => this.subscribed.add(kw));
    this.emit({ action: 'subscribe', keywords });
  }

  unsubscribe(keywords: string[]) {
    keywords.forEach((kw) => this.subscribed.delete(kw));
    this.emit({ action: 'unsubscribe', keywords });
  }

  onHotspotNew(fn: Handler<Hotspot>) {
    return this.on('hotspot:new', fn);
  }

  onNotification(fn: Handler<Notification>) {
    return this.on('notification', fn);
  }

  private on<T>(type: string, fn: Handler<T>): () => void {
    if (!this.handlers.has(type)) {
      this.handlers.set(type, new Set());
    }
    this.handlers.get(type)!.add(fn as (data: unknown) => void);
    return () => this.handlers.get(type)?.delete(fn as (data: unknown) => void);
  }

  disconnect() {
    this.closedByUser = true;
    if (this.reconnectTimer) clearTimeout(this.reconnectTimer);
    this.socket?.close();
  }
}

export const socket = new HotTopicSocket();
