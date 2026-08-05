package com.yupi.hottopic.service.hotspot;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yupi.hottopic.config.MonitorProperties;
import com.yupi.hottopic.dto.AIAnalysis;
import com.yupi.hottopic.dto.SearchResult;
import com.yupi.hottopic.entity.Hotspot;
import com.yupi.hottopic.entity.Keyword;
import com.yupi.hottopic.entity.Notification;
import com.yupi.hottopic.mapper.HotspotMapper;
import com.yupi.hottopic.service.NotificationService;
import com.yupi.hottopic.service.ai.AiClient;
import com.yupi.hottopic.service.collect.AccountDetector;
import com.yupi.hottopic.service.collect.CollectService;
import com.yupi.hottopic.service.mail.EmailService;
import com.yupi.hottopic.util.HotspotResultUtils;
import com.yupi.hottopic.util.KeywordUtils;
import com.yupi.hottopic.ws.HotspotWebSocketHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 热点巡检编排(需求 HC-1 ~ HC-9 / AI-7 ~ AI-9),对应原项目 hotspotChecker.ts:
 *
 * 激活关键词 → 查询扩展 → 多源聚合 → 配额控制 → AI 分析 → 三重过滤 → 幂等入库 → 站内通知
 */
@Service
public class HotspotChecker {

    private static final Logger log = LoggerFactory.getLogger(HotspotChecker.class);

    private final CollectService collectService;
    private final AiClient aiClient;
    private final AccountDetector accountDetector;
    private final HotspotMapper hotspotMapper;
    private final NotificationService notificationService;
    private final EmailService emailService;
    private final HotspotWebSocketHandler webSocketHandler;
    private final MonitorProperties props;

    public HotspotChecker(CollectService collectService,
                          AiClient aiClient,
                          AccountDetector accountDetector,
                          HotspotMapper hotspotMapper,
                          NotificationService notificationService,
                          EmailService emailService,
                          HotspotWebSocketHandler webSocketHandler,
                          MonitorProperties props) {
        this.collectService = collectService;
        this.aiClient = aiClient;
        this.accountDetector = accountDetector;
        this.hotspotMapper = hotspotMapper;
        this.notificationService = notificationService;
        this.emailService = emailService;
        this.webSocketHandler = webSocketHandler;
        this.props = props;
    }

    /** 巡检全部激活关键词 */
    public void checkAll(List<Keyword> keywords) {
        if (keywords == null || keywords.isEmpty()) {
            log.info("没有激活的监控关键词,跳过巡检");
            return;
        }
        log.info("开始巡检 {} 个关键词", keywords.size());
        int newHotspots = 0;
        for (Keyword keyword : keywords) {
            try {
                newHotspots += checkKeyword(keyword);
                // 避免过快请求(需求 §4.2 爬虫频率控制)
                Thread.sleep(props.getRequestIntervalMs());
            } catch (Exception e) {
                log.error("巡检关键词 \"{}\" 失败: {}", keyword.getText(), e.getMessage(), e);
            }
        }
        log.info("巡检完成,新增 {} 个热点", newHotspots);
    }

    /** 巡检单个关键词,返回新增热点数 */
    int checkKeyword(Keyword keyword) {
        String kw = keyword.getText();
        log.info("📎 检查关键词: \"{}\"", kw);

        // 1. 查询扩展(AI-1)
        List<String> expanded = aiClient.expandKeyword(kw);
        log.info("  扩展为 {} 个变体: {}", expanded.size(), String.join(", ", expanded.subList(0, Math.min(5, expanded.size()))));

        // 1.5 账号检测(HC-4):关键词是 B 站账号时直接拉取最新视频
        AccountDetector.DetectResult accountResult = accountDetector.detectAndFetch(kw);

        // 2. 多源聚合抓取(HC-3):账号内容优先,再合并多源结果
        List<SearchResult> merged = new ArrayList<>();
        if (!accountResult.results().isEmpty()) {
            log.info("  账号内容 {} 条优先加入", accountResult.results().size());
            merged.addAll(accountResult.results());
        }
        merged.addAll(collectService.collect(kw));
        List<SearchResult> results = HotspotResultUtils.prioritize(
                HotspotResultUtils.filterByFreshness(
                        HotspotResultUtils.deduplicate(merged), props.getMaxAgeHours()));
        log.info("  聚合结果: {} 条(去重新鲜后)", results.size());

        // 3. 配额处理(Twitter 优先多给,HC-8)
        int twitterProcessed = 0;
        int otherProcessed = 0;
        int newCount = 0;
        for (SearchResult item : results) {
            boolean isTwitter = "twitter".equals(item.getSource());
            if (isTwitter && twitterProcessed >= props.getTwitterQuota()) {
                continue;
            }
            if (!isTwitter && otherProcessed >= props.getOtherQuota()) {
                continue;
            }
            if (twitterProcessed + otherProcessed >= props.getTwitterQuota() + props.getOtherQuota()) {
                break;
            }

            // 4. 幂等检查(HC-9,(url, source) 唯一)
            if (exists(item.getUrl(), item.getSource())) {
                continue;
            }

            // 5. AI 分析(预匹配增强,AI-7)
            String fullText = (item.getTitle() == null ? "" : item.getTitle())
                    + "\n" + (item.getContent() == null ? "" : item.getContent());
            KeywordUtils.PreMatchResult preMatch = KeywordUtils.preMatchKeyword(fullText, expanded);
            AIAnalysis analysis = aiClient.analyzeContent(fullText, kw, preMatch);

            // 6. 三重过滤(AI-8):账号检测内容豁免"未提及关键词"的严格阈值(HC-4 语义:账号本身就是关键词)
            if (!Boolean.TRUE.equals(analysis.getIsReal())) {
                log.info("  ❌ 过滤疑似虚假内容: {}", truncate(item.getTitle()));
                continue;
            }
            if (analysis.getRelevance() == null || analysis.getRelevance() < props.getRelevanceThreshold()) {
                log.info("  ⏭ 相关性不足({}): {}", analysis.getRelevance(), truncate(item.getTitle()));
                continue;
            }
            if (!item.isAccountContent() && !Boolean.TRUE.equals(analysis.getKeywordMentioned())
                    && analysis.getRelevance() < props.getStrictThreshold()) {
                log.info("  ⏭ 未提及关键词且相关性 < {} ({}): {}",
                        props.getStrictThreshold(), analysis.getRelevance(), truncate(item.getTitle()));
                continue;
            }

            // 7. 入库(HC-9)
            Hotspot hotspot = saveHotspot(item, keyword, analysis);
            newCount++;

            // 8. 站内通知(NT-2)
            Notification notification = notificationService.create(
                    "hotspot",
                    "发现新热点: " + truncate(hotspot.getTitle(), 50),
                    analysis.getSummary() == null || analysis.getSummary().isBlank()
                            ? truncate(hotspot.getContent(), 100)
                            : analysis.getSummary(),
                    hotspot.getId());
            log.info("  ✅ 新热点 [{}] 重要度={} 相关性={}: {}",
                    hotspot.getSource(), hotspot.getImportance(), hotspot.getRelevance(), truncate(hotspot.getTitle()));

            // 9. WebSocket 实时推送(需求 NT-1):按关键词订阅推送 + 全局广播通知
            webSocketHandler.sendToKeywordSubscribers(kw, "hotspot:new", hotspot);
            Map<String, Object> notificationPayload = new java.util.HashMap<>();
            notificationPayload.put("type", "hotspot");
            notificationPayload.put("title", "发现新热点");
            notificationPayload.put("content", hotspot.getTitle());
            notificationPayload.put("hotspotId", hotspot.getId());
            notificationPayload.put("importance", hotspot.getImportance());
            webSocketHandler.broadcast("notification", notificationPayload);

            // 10. 邮件通知(需求 NT-3:仅 high/urgent 级别)
            if (List.of("high", "urgent").contains(analysis.getImportance())) {
                emailService.sendHotspotEmail(hotspot);
            }

            if (isTwitter) {
                twitterProcessed++;
            } else {
                otherProcessed++;
            }
        }
        return newCount;
    }

    private boolean exists(String url, String source) {
        Long count = hotspotMapper.selectCount(new LambdaQueryWrapper<Hotspot>()
                .eq(Hotspot::getUrl, url)
                .eq(Hotspot::getSource, source));
        return count != null && count > 0;
    }

    private Hotspot saveHotspot(SearchResult item, Keyword keyword, AIAnalysis analysis) {
        Hotspot h = new Hotspot();
        h.setTitle(item.getTitle());
        h.setContent(item.getContent());
        h.setUrl(item.getUrl());
        h.setSource(item.getSource());
        h.setSourceId(item.getSourceId());
        h.setIsReal(analysis.getIsReal());
        h.setRelevance(analysis.getRelevance());
        h.setRelevanceReason(analysis.getRelevanceReason());
        h.setKeywordMentioned(analysis.getKeywordMentioned());
        h.setImportance(analysis.getImportance());
        h.setSummary(analysis.getSummary());
        h.setViewCount(item.getViewCount());
        h.setLikeCount(item.getLikeCount());
        h.setRetweetCount(item.getRetweetCount());
        h.setReplyCount(item.getReplyCount());
        h.setCommentCount(item.getCommentCount());
        h.setQuoteCount(item.getQuoteCount());
        h.setDanmakuCount(item.getDanmakuCount());
        h.setAuthorName(item.getAuthorName());
        h.setAuthorUsername(item.getAuthorUsername());
        h.setAuthorAvatar(item.getAuthorAvatar());
        h.setAuthorFollowers(item.getAuthorFollowers());
        h.setAuthorVerified(item.getAuthorVerified());
        h.setPublishedAt(item.getPublishedAt());
        h.setKeywordId(keyword.getId());
        hotspotMapper.insert(h);
        return h;
    }

    private String truncate(String s) {
        return truncate(s, 40);
    }

    private String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }
}
