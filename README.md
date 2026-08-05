# HotTopic 🔥 热点监控工具

> 输入监控关键词,系统自动从 **6+ 信息源** 聚合抓取热点,用 **AI(DeepSeek)** 做真假识别、相关性评分与智能摘要,通过 **WebSocket 实时推送 + 邮件** 通知你,第一时间发现最新动态。
>
> 参考项目:[yupi-hot-monitor](https://github.com/liyupi/yupi-hot-monitor)(Node/TS 实现),本仓库为 Java 版复刻。

## ✨ 核心能力

| 能力 | 说明 |
|------|------|
| 🔑 关键词监控 | 添加/启停/删除监控词,每 30 分钟自动巡检 |
| 🌐 多源聚合 | Bing、HackerNews、搜狗、B站、微博 6 源并行抓取,单源故障不影响整体 |
| 🎯 账号检测 | 关键词是 B 站 UP 主时,直接拉取其最新视频 |
| 🤖 AI 分析 | 查询扩展 → 真假识别 → 相关性 0-100 → 重要度四档 → 智能摘要(失败自动降级) |
| 📊 多维检索 | 5 维筛选(来源/重要度/关键词/时间/真实性)+ 5 种排序 + 热度五级 + 分页 |
| ⚡ 实时通知 | WebSocket 按关键词订阅推送 + 站内通知 + high/urgent 邮件提醒 |
| 🔎 全网搜索 | 任意关键词多源聚合搜索,不落库 |

## 🛠 技术栈

**后端**:Java 17 · Spring Boot 4.0.7 · MyBatis-Plus 3.5.17 · MySQL 8 · Spring AI 2.0(DeepSeek)· WebSocket · Jsoup · Caffeine
**前端**:React 19 · TypeScript · Vite 7 · Tailwind CSS 4
**测试**:JUnit 5 · Mockito · H2(49+ 单元测试 + 集成测试)

## 🚀 快速启动

### 前置条件

- JDK 17+、Maven(wrapper 已含)、Node.js ≥ 20
- MySQL 8:创建数据库 `hottopic`(表自动创建)
- DeepSeek API Key(不配也能跑,AI 分析自动降级为文本预匹配)

### 1. 配置环境变量

```powershell
# DeepSeek(必配,用于 AI 分析)
setx DEEPSEEK_API_KEY "sk-你的key"

# 邮件通知(可选)
setx MAIL_HOST "smtp.qq.com"
setx MAIL_PORT "465"
setx MAIL_USERNAME "你的邮箱"
setx MAIL_PASSWORD "SMTP授权码"
setx MAIL_TO "收件人邮箱"
setx MAIL_ENABLED "true"
```

数据库连接在 `src/main/resources/application.yml` 修改(`spring.datasource.username/password`,默认 `root/root`,可用 `DB_PASSWORD` 环境变量覆盖)。

### 2. 启动后端(:3001)

```powershell
.\mvnw.cmd spring-boot:run
```

或打包运行:

```powershell
.\mvnw.cmd package
java -jar target/HotTopic-0.0.1-SNAPSHOT.jar
```

### 3. 启动前端(:5173)

```powershell
cd client
npm install
npm run dev
```

浏览器打开 **http://localhost:5173** → 添加监控词 → 点「立即扫描」体验全流程。

### 4. 单端口部署(可选)

前端构建产物可直接由后端托管:

```powershell
cd client; npm run build
# 将 client/dist/* 复制到 src/main/resources/static/
# 然后启动后端,直接访问 http://localhost:3001
```

## 🔌 API 概览

| 分组 | 端点 | 说明 |
|------|------|------|
| 关键词 | `GET/POST /api/keywords`、`PUT/DELETE /api/keywords/{id}` | 管理监控词(含热点数统计) |
| 热点 | `GET /api/hotspots` | 分页 + 5 维筛选 + 5 种排序 |
| | `GET /api/hotspots/{id}` | 详情(含热度分) |
| | `POST /api/hotspots/search` | 全网搜索 |
| 通知 | `GET /api/notifications`、`GET /unread-count`、`POST /read` | 分页/未读数/已读 |
| 设置 | `GET/PUT /api/settings` | KV 设置(敏感值脱敏) |
| 运维 | `GET /api/health`、`POST /api/check-hotspots` | 健康检查/手动巡检 |

WebSocket 端点:`/ws`(消息协议见 `docs/REQUIREMENTS.md` §7.2)

## 📁 目录结构

```
├── src/main/java/com/yupi/hottopic/
│   ├── controller/       # REST 接口 + 全局异常
│   ├── service/          # 业务:ai(AI)/collect(采集)/hotspot(巡检)/mail(邮件)
│   ├── entity/ mapper/   # MyBatis-Plus 实体与 Mapper
│   ├── job/              # @Scheduled 定时巡检
│   ├── ws/               # WebSocket 推送
│   ├── config/ util/ dto/
├── client/               # React 前端
├── docs/                 # REQUIREMENTS.md 需求文档 / TECH_STACK.md 技术选型
└── scripts/              # 部署与运行脚本
```

## 📚 文档

- [需求文档](docs/REQUIREMENTS.md) — 功能规格、接口设计、验收标准
- [技术选型文档](docs/TECH_STACK.md) — 架构、选型理由、排障记录

## 🧪 测试

```powershell
.\mvnw.cmd test        # 单元 + 集成测试(测试用 H2 内存库,不依赖本机 MySQL)
```

## 📌 已知限制

- 微博仅匹配热搜榜(公开 API);完整微博搜索需登录态
- Twitter 源预留接口,接入需 twitterapi.io API Key
- 单机部署,无用户体系(本地工具定位)
