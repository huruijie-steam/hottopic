# HotTopic 热点监控工具 —— 技术选型文档

> 版本:v1.0
> 状态:评审中
> 对应需求:docs/REQUIREMENTS.md
> 参考项目:[yupi-hot-monitor](https://github.com/liyupi/yupi-hot-monitor)(Node/TS 实现)

---

## 1. 现状分析

当前 `D:\IDEA JAVA\HotTopic` 骨架已确定:

| 项 | 现状 |
|----|------|
| 构建 | Maven(mvnw wrapper 已就绪) |
| 语言 | Java 17 |
| 框架 | Spring Boot **4.0.7**(注意:Boot 4 起 `spring-boot-starter-web` 更名为 `spring-boot-starter-webmvc`) |
| 已引入依赖 | webmvc、mysql-connector-j(runtime)、lombok、webmvc-test |
| 代码 | 仅启动类 `HotTopicApplication` + 空测试 + `application.properties` |

**待补充**:数据访问层、WebSocket、邮件、定时任务、AI 客户端、HTML 解析爬虫、缓存、参数校验、配置管理。

---

## 2. 总体架构

```
┌──────────────────────────── 前端(Vite 开发服务器 :5173) ────────────────────────────┐
│  React 19 + Tailwind CSS 4  页面:仪表盘 / 监控词 / 搜索 / 设置                          │
└───────────────┬──────────────────────────────────────┬───────────────────────────────┘
                │ REST (axios/fetch, /api 代理)          │ WebSocket(STOMP/SockJS)
┌───────────────▼──────────────────────────────────────▼───────────────────────────────┐
│  Spring Boot 4 后端 :3001                                                             │
│  ┌──────────────┐ ┌──────────────┐ ┌─────────────────┐ ┌───────────────────────────┐  │
│  │ Controller 层 │→│ Service 层    │→│ Repository(JPA) │→│ MySQL 8                   │  │
│  │ REST + WS     │ │ 业务编排      │ │                  │ │ 表:keyword/hotspot/       │  │
│  └──────────────┘ └──┬───────────┘ └─────────────────┘ │    notification/setting    │  │
│                      │                                 └───────────────────────────┘  │
│        ┌─────────────┼─────────────────────────────────────┐                          │
│        ▼             ▼             ▼             ▼          ▼                          │
│  ┌───────────┐ ┌───────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐                  │
│  │ AI 服务    │ │ 采集服务    │ │ 通知服务   │ │ 缓存      │ │ 定时任务   │                  │
│  │ DeepSeek/ │ │ 8 数据源   │ │ WS+邮件   │ │ Caffeine │ │ @Scheduled│                  │
│  │ OpenRouter │ │ 爬虫+限流  │ │          │ │ 查询扩展   │ │ 30min 巡检 │                  │
│  └───────────┘ └───────────┘ └──────────┘ └──────────┘ └──────────┘                  │
└───────────────────────────────────────────────────────────────────────────────────────┘
```

**分层约定**:`controller → service → repository`;领域逻辑放 service;爬虫、AI、邮件各自独立 service 类;工具函数放 `util`。

---

## 3. 后端技术选型

> 表格中"✅ 推荐"为默认方案;★ 标记的决策项若你有不同偏好,可调整后再开工。

### 3.1 基础框架

| 领域 | ✅ 推荐 | 备选 | 理由 |
|------|---------|------|------|
| 语言/构建 | Java 17 + Maven | Gradle | 已定,不讨论 |
| Web 框架 | Spring Boot 4.0.7(webmvc) | — | 已定;Boot 4 原生支持虚拟线程、模块化 starter |
| ★ ORM | **MyBatis-Plus** | Spring Data JPA | 已确认采用 MP;国内生态熟悉、API 直观。⚠️ 注意:MP 官方 starter 主要面向 Boot 2/3,接入 Boot 4 需验证兼容版本(见 §8 风险),开工第一步先验证依赖可编译 |
| 数据库 | MySQL 8.x | SQLite(原项目)/ H2(测试) | 骨架已引入 mysql-connector-j;测试环境用 H2 内存库 |
| 连接池 | HikariCP(Spring Boot 默认) | — | 开箱即用 |
| 迁移工具 | 无(用 `schema.sql` + MyBatis-Plus 自动建表/手动初始化) | Flyway | 个人项目 SQL 初始化足够;正式化可上 Flyway |

### 3.2 业务能力

| 领域 | ✅ 推荐 | 备选 | 理由 |
|------|---------|------|------|
| ★ AI 调用 | **Spring AI(`spring-ai-openai-starter`)+ DeepSeek 官方 API** | 直接 OkHttp 调 OpenAI 兼容接口 / OpenRouter SDK | Spring AI 1.0 已 GA,提供 `ChatClient` 流式/结构化输出(BeanOutputConverter 直接解析 JSON schema),与 Boot 4 集成好;DeepSeek 国内直连、便宜、模型 `deepseek-chat` 即 v3 系。接口抽象成 `AiClient` 便于日后换 OpenRouter/GPT |
| 定时任务 | Spring `@Scheduled` + `@EnableScheduling` | Quartz | 单机定时够用;Quartz 支持集群持久化,未来多实例再升级 |
| WebSocket | **`spring-boot-starter-websocket`(原生 WS,`TextWebSocketHandler` + 房间 Map)** | STOMP + SockJS | 原项目用 socket.io 协议,Java 侧无官方实现(netty-socketio 是第三方)。原生 WebSocket 协议简单、前端 `WebSocket` API 直接用;STOMP 方案可加。**前端 socket.io-client 需替换** |
| 邮件 | `spring-boot-starter-mail`(JavaMailSender) | — | 标准方案,支持 HTML 模板 |
| 爬虫 | JDK `java.net.http.HttpClient` + **Jsoup** | Apache HttpClient + HtmlUnit | Boot 4 自带虚拟线程,HttpClient 天然适配;Jsoup 对标 cheerio,选择器语法几乎一致,迁移成本低 |
| 缓存 | Spring Cache 抽象 + **Caffeine** | Redis | 仅缓存"查询扩展结果"与设置,本地进程缓存足够;未来多实例换 Redis 只改注解 |
| 参数校验 | `spring-boot-starter-validation`(jakarta.validation) | 手动校验 | 关键词/搜索入参统一校验 |
| JSON | Jackson(Boot 自带) | — | 无需额外依赖 |

### 3.3 工具与质量

| 领域 | ✅ 推荐 | 说明 |
|------|---------|------|
| 配置管理 | `application.yml` + `@ConfigurationProperties`(AI、邮件、爬虫配额分组) | 敏感值用 `${ENV_VAR}` 注入,不落库不提交 |
| 测试 | JUnit 5 + Mockito + `spring-boot-starter-webmvc-test`(已引入) | AI/爬虫全部 mock;排序、过滤、AI 输出解析写纯单测 |
| 日志 | SLF4J + Logback(Boot 自带),业务日志打点 | 巡检过程逐关键词日志,便于排查 |
| Lombok | 已引入(骨架已配 annotationProcessor) | 实体/DTO 用 `@Data` |
| 代码结构 | 按功能分包:`entity / repository / service / controller / dto / config / job / util / client` | 见 §5 |

### 3.4 依赖清单(pom.xml 待添加)

```xml
<!-- 数据访问:MyBatis-Plus(必须配 starter-jdbc 激活 DataSource;已实测 3.5.17 + Boot 4.0.7) -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-jdbc</artifactId>
</dependency>
<dependency>
    <groupId>com.baomidou</groupId>
    <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
</dependency>
<!-- MyBatis-Plus 3.5.9+ 分页插件拆分到独立模块 -->
<dependency>
    <groupId>com.baomidou</groupId>
    <artifactId>mybatis-plus-jsqlparser</artifactId>
</dependency>
<!-- WebSocket -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-websocket</artifactId>
</dependency>
<!-- 邮件 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-mail</artifactId>
</dependency>
<!-- 校验 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
<!-- 缓存 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-cache</artifactId>
</dependency>
<dependency>
    <groupId>com.github.ben-manes.caffeine</groupId>
    <artifactId>caffeine</artifactId>
</dependency>
<!-- AI:Spring AI + DeepSeek 官方 starter(已实测 2.0.0 与 Boot 4.0.7 兼容;1.1.x 存在 HttpHeaders 二进制不兼容) -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-model-deepseek</artifactId>
</dependency>

<!-- JSON 解析(Spring AI 2.x 不再传递引入,需显式声明) -->
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
</dependency>
<!-- HTML 解析爬虫 -->
<dependency>
    <groupId>org.jsoup</groupId>
    <artifactId>jsoup</artifactId>
    <version>1.23.1</version>
</dependency>
<!-- 测试用 H2(内存库,避免测试依赖本机 MySQL) -->
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>test</scope>
</dependency>
```

> ⚠️ Spring AI 需引入 BOM(`spring-ai-bom`,版本 **2.0.0**);DeepSeek 用官方 starter(`spring-ai-starter-model-deepseek`),配置 `spring.ai.deepseek.api-key`。

---

## 4. 前端技术选型

> 当前骨架**没有前端目录**。以下为建议方案(与原项目一致),可整体替换为你熟悉的技术栈。

| 领域 | ✅ 推荐 | 备选 | 理由 |
|------|---------|------|------|
| 框架 | React 19 + TypeScript | Vue 3 + TS | 与原项目一致,教程资源多 |
| 构建 | Vite 7 | Webpack | 快,开发体验好 |
| 样式 | Tailwind CSS 4 + framer-motion | Element Plus / Ant Design | 还原"赛博朋克科技感"UI |
| 状态 | React hooks(useState/useCallback)+ 组件内状态 | Zustand | 页面状态简单,无需全局库 |
| HTTP | fetch 封装(统一错误处理) | axios | 简单够用 |
| 实时 | **原生 WebSocket**(`new WebSocket('/ws')`) | STOMP.js + SockJS | 替换 socket.io-client;同源由 Vite proxy 转发 `/ws` |
| 图标 | lucide-react | — | 与原项目一致 |

**前端目录规划**:
```
client/
├── src/
│   ├── App.tsx                  # 主页面(三个 Tab:仪表盘/监控词/搜索/设置)
│   ├── components/
│   │   ├── HotspotCard.tsx      # 热点卡片(热度等级、来源徽标、互动数据)
│   │   ├── FilterSortBar.tsx    # 筛选排序栏(5 维筛选 + 4 种排序)
│   │   ├── StatCards.tsx        # 统计卡片
│   │   └── ui/                  # Aceternity 风格特效组件
│   ├── services/
│   │   ├── api.ts               # REST 封装
│   │   └── socket.ts            # WebSocket 封装(订阅/事件)
│   ├── utils/
│   │   └── heatScore.ts         # 热度计算(与后端公式一致)
│   └── types.ts                 # 共享类型
```

---

## 5. 后端目录结构规划

```
src/main/java/com/yupi/hottopic/
├── HotTopicApplication.java
├── config/                  # WebSocketConfig、CacheConfig、AsyncConfig、OpenAiConfig
├── entity/                  # Keyword、Hotspot、Notification、Setting(MyBatis-Plus @TableName)
├── mapper/                  # 四个 BaseMapper + 自定义 SQL
├── dto/                     # 请求/响应对象(KeywordRequest、HotspotQuery、AIAnalysis…)
├── service/
│   ├── KeywordService.java
│   ├── HotspotService.java      # 入库、查询、筛选排序、热度计算
│   ├── NotificationService.java
│   ├── SettingService.java
│   ├── ai/
│   │   ├── AiClient.java        # 抽象:分析内容/查询扩展(可切换供应商)
│   │   └── AiPromptBuilder.java # prompt 构造 + JSON 解析
│   ├── collect/
│   │   ├── SourceFetcher.java   # 接口:fetch(query) → List<SearchResult>
│   │   ├── BingFetcher.java / GoogleFetcher.java / DuckDuckGoFetcher.java
│   │   ├── HackerNewsFetcher.java
│   │   ├── SogouFetcher.java / BilibiliFetcher.java / WeiboFetcher.java
│   │   └── TwitterFetcher.java
│   ├── collect/RateLimiter.java # 每源独立限流
│   ├── mail/EmailService.java
│   └── hotspot/HotspotChecker.java  # 巡检编排(对应原 hotspotChecker.ts)
├── job/                     # HotspotCheckJob(@Scheduled 入口)
├── controller/              # KeywordController、HotspotController、NotificationController、SettingController、OpsController
├── ws/                      # HotspotWebSocketHandler(房间管理)
└── util/                    # HeatScoreCalculator、UrlNormalizer、KeywordUtils
```

---

## 6. 关键技术决策与理由

| # | 决策 | 说明 |
|---|------|------|
| D1 | **用 Spring AI 替代裸 HTTP 调 AI** | 结构化输出(`BeanOutputConverter`)直接映射 AIAnalysis JSON,省去手写 JSON 解析与容错;`temperature=0.2` 可配置 |
| D2 | **AI 供应商抽象为 AiClient 接口** | DeepSeek(默认)/ OpenRouter / 任意 OpenAI 兼容端点可插拔,配 `spring.ai.openai.base-url` 即切换 |
| D3 | **原生 WebSocket 而非 socket.io 协议** | socket.io 的 Java 服务端无官方实现;原生 WS + 房间 Map(ConcurrentHashMap<String, Set<String>>)完全覆盖"按关键词订阅推送"需求 |
| D4 | **巡检编排用虚拟线程并行抓源** | Boot 4 + JDK 17 下可用 `@Async` 或直接 `CompletableFuture`/虚拟线程并行 8 源,Promise.allSettled → 等所有结果各自兜底(单源异常不影响整体,与原项目语义一致) |
| D5 | **配额与过滤规则放配置而非硬编码** | twitterQuota=15 / otherQuota=10 / maxAgeHours=168 / relevanceThreshold=50 全部进 `application.yml`,可热调 |
| D6 | **热度公式前后端共享一份** | 后端 Java 计算入库兜底,前端 TS 计算展示;公式文档化,避免两边漂移 |
| D7 | **幂等靠 (url, source) 唯一索引** | 与建表 SQL 的 UNIQUE 约束一致;重复巡检插入冲突时捕获异常跳过(MyBatis-Plus 插入前先按 url+source 查重) |
| D8 | **敏感配置只走环境变量** | `OPENROUTER_API_KEY` / `DEEPSEEK_API_KEY` / SMTP 密码均 `${VAR}` 注入,`setting` 表只存非敏感项(如收件人、巡检间隔) |

---

## 7. 环境要求

| 项 | 要求 |
|----|------|
| JDK | 17+(骨架已定) |
| Maven | 3.9+(wrapper 已就绪) |
| MySQL | 8.x(本地或 Docker);测试可用 H2 |
| Node.js | ≥ 20(仅前端开发) |
| AI Key | DeepSeek 官方 API Key(推荐)或 OpenRouter Key(二选一,可在设置页切换) |
| 网络 | 可访问 Bing/搜狗/B站等抓取源;若部署在国外 VPS,国内源需注意可达性 |

---

## 8. 风险与备选方案

| 风险 | 影响 | 缓解/备选 |
|------|------|-----------|
| **MyBatis-Plus 与 Boot 4 自动配置排序** | Mapper 无 SqlSessionFactory | ✅ 已解决(实测):① 必须加 `spring-boot-starter-jdbc`(否则 DataSource 不激活,MP 的 `@ConditionalOnSingleCandidate(DataSource)` 不满足);② Boot 4 下自动配置排序仍可能评估失败,用显式 `DataSourceConfig`(DataSourceBuilder + `@ConfigurationProperties`)提供 DataSource bean 即可。3.5.17 实测可用 |
| **Spring AI 与 Boot 4.0.7 版本匹配** | 启动 NoSuchMethodError(HttpHeaders.addAll) | ✅ 已解决(实测):1.1.x 与 Boot 4.0.7 二进制不兼容,必须用 **2.0.0**;且 2.x 不传递 jackson-databind,需显式声明 |
| 微博/搜狗反爬升级 | 单源失效 | 抓取失败静默降级,不影响整体;源实现按 `SourceFetcher` 接口隔离,随时可换实现 |
| 无前端骨架 | 交付不完整 | 前端单独建 `client/` 目录(Vite 独立工程),与后端解耦;或先用后端接口 + 简单页面验证 |
| socket.io 客户端历史代码 | 前端改造量 | 前端本就新建,直接用原生 WebSocket,无历史包袱 |

---

## 9. 开发顺序建议(里程碑)

| 阶段 | 内容 | 依赖 |
|------|------|------|
| M1 基建 | 补 pom 依赖、application.yml、JPA 实体 + 4 表、健康检查 | — |
| M2 AI 服务 | AiClient + prompt + JSON 解析 + 单测 | M1 |
| M3 采集 | RateLimiter + Bing/HN 两个源 + 去重/新鲜度过滤 | M1 |
| M4 巡检闭环 | HotspotChecker + @Scheduled + 入库 + 站内通知 | M2+M3 |
| M5 通知 | WebSocket 房间推送 + 邮件 | M4 |
| M6 API 完善 | 关键词/热点/通知/设置全部接口 + 筛选排序 | M4 |
| M7 前端 | React 页面(仪表盘/监控词/搜索/设置)+ WS 接入 | M6 |
| M8 增强 | 国内源(搜狗/B站/微博)+ 账号检测 + 查询扩展缓存 + 配额 | M4 |
| M9 打磨 | 测试补全、日志、部署、Agent Skills 技能包(可选) | 全部 |

> 建议每完成一个里程碑即 `git commit` 并推送,保持可回滚。
