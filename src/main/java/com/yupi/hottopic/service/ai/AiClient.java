package com.yupi.hottopic.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.yupi.hottopic.dto.AIAnalysis;
import com.yupi.hottopic.util.KeywordUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AI 能力封装(需求 AI-1 ~ AI-9),基于 Spring AI + DeepSeek。
 * 设计要点:查询扩展带缓存;所有 AI 调用失败均降级为预匹配兜底,流程永不中断。
 */
@Service
public class AiClient {

    private static final Logger log = LoggerFactory.getLogger(AiClient.class);

    private static final Pattern JSON_OBJECT_PATTERN = Pattern.compile("\\{[\\s\\S]*}");
    private static final Pattern JSON_ARRAY_PATTERN = Pattern.compile("\\[[\\s\\S]*]");

    private static final List<String> IMPORTANCE_LEVELS = List.of("low", "medium", "high", "urgent");

    /** 未配置时的占位 key(与 application.yml 默认值一致;不以 sk- 开头,避免被 secret 扫描误报) */
    private static final String PLACEHOLDER_API_KEY = "your-deepseek-api-key-here";

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;

    /** 查询扩展结果缓存:同一关键词不重复调用 AI */
    private final Cache<String, List<String>> expansionCache = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofHours(24))
            .build();

    public AiClient(ChatClient.Builder builder,
                    @Value("${spring.ai.deepseek.api-key:}") String apiKey) {
        this.chatClient = builder.build();
        this.objectMapper = new ObjectMapper();
        this.apiKey = apiKey;
    }

    // ========== Query Expansion(查询扩展,AI-1) ==========

    /**
     * 将关键词扩展为多个变体,用于文本预过滤。带缓存,无 key 时降级为纯文本拆词。
     */
    public List<String> expandKeyword(String keyword) {
        List<String> cached = expansionCache.getIfPresent(keyword);
        if (cached != null) {
            return cached;
        }

        List<String> coreTerms = KeywordUtils.extractCoreTerms(keyword);
        if (!hasApiKey()) {
            List<String> result = mergeUnique(keyword, coreTerms);
            expansionCache.put(keyword, result);
            return result;
        }

        try {
            String prompt = """
                    你是一个搜索查询扩展专家。给定一个监控关键词,生成该关键词的变体和相关检索词,用于文本匹配。

                    规则:
                    1. 包含原始关键词的各种写法(大小写、空格、连字符变体)
                    2. 包含关键词的核心组成词(拆分后的各个有意义的词)
                    3. 包含常见别称、缩写、中英文对照
                    4. 不要加入泛化词(比如关键词是"Claude Sonnet 4.6",不要加"AI模型"这种泛化词)
                    5. 总数控制在 5-15 个

                    输出 JSON 数组,只输出 JSON,不要有其他内容。
                    示例输入:"Claude Sonnet 4.6"
                    示例输出:["Claude Sonnet 4.6", "Claude Sonnet", "Sonnet 4.6", "claude-sonnet-4.6", "Claude 4.6", "Anthropic Sonnet"]
                    """;
            String response = chatClient.prompt()
                    .system(prompt)
                    .user(keyword)
                    .call()
                    .content();
            List<String> parsed = parseExpansion(response);
            if (!parsed.isEmpty()) {
                List<String> result = mergeUnique(keyword, coreTerms, parsed);
                expansionCache.put(keyword, result);
                log.info("Query expansion for \"{}\": {} variants", keyword, result.size());
                return result;
            }
        } catch (Exception e) {
            log.warn("Query expansion failed for \"{}\": {}", keyword, e.getMessage());
        }

        List<String> fallback = mergeUnique(keyword, coreTerms);
        expansionCache.put(keyword, fallback);
        return fallback;
    }

    // ========== AI 内容分析(AI-2 ~ AI-6,关键词感知) ==========

    /**
     * 分析内容是否与监控关键词相关。
     *
     * @param content      待分析文本
     * @param keyword      监控关键词
     * @param preMatch     预匹配结果(命中→宽松审核;未命中→严格审核)
     */
    public AIAnalysis analyzeContent(String content, String keyword, KeywordUtils.PreMatchResult preMatch) {
        KeywordUtils.PreMatchResult match = preMatch != null ? preMatch : new KeywordUtils.PreMatchResult(false, List.of());

        if (!hasApiKey()) {
            log.warn("DeepSeek API key 未配置,使用默认分数");
            return fallbackAnalysis(match, 50, 20);
        }

        try {
            String prompt = buildAnalysisPrompt(keyword, match);
            String response = chatClient.prompt()
                    .system(prompt)
                    .user(content.length() > 2000 ? content.substring(0, 2000) : content)
                    .call()
                    .content();
            return parseAnalysis(response);
        } catch (Exception e) {
            log.warn("AI analysis failed: {}", e.getMessage());
            return fallbackAnalysis(match, 30, 10);
        }
    }

    private String buildAnalysisPrompt(String keyword, KeywordUtils.PreMatchResult match) {
        String matchHint = match.matched()
                ? "注意:文本预匹配发现内容中包含以下关键词变体:" + String.join("、", match.matchedTerms())
                : "注意:文本预匹配发现内容中未直接提及关键词\"" + keyword + "\"的任何变体,请特别严格审核相关性。";

        return """
                你是一个热点内容精准匹配专家。你的任务是判断一段内容是否与指定的监控关键词【%s】直接相关。

                %s

                分析要点:
                1. 判断是否为真实有价值的信息(排除标题党、假新闻、营销软文)
                2. 判断内容是否【直接】涉及关键词"%s"。注意:
                   - 仅仅属于同一领域但未提及关键词的内容,相关性应低于 40 分
                   - 内容必须直接讨论、提及或与"%s"有实质关联才能获得 60 分以上
                   - 只是间接沾边(如同类产品、同领域但不同主题)应给 30-50 分
                3. 判断内容中是否直接提及了"%s"或其等价表述(keywordMentioned)
                4. 评估热点的重要程度(对关注"%s"的人来说有多重要)
                5. 用一句话说明此内容与"%s"的关系(不是介绍内容本身,而是说"此内容与关键词的关联是什么")
                6. 用一句话解释你的相关性打分理由

                请以 JSON 格式输出:
                {
                  "isReal": true/false,
                  "relevance": 0-100,
                  "relevanceReason": "相关性打分理由...",
                  "keywordMentioned": true/false,
                  "importance": "low/medium/high/urgent",
                  "summary": "此内容与【%s】的关联:..."
                }

                只输出 JSON,不要有其他内容。
                """.formatted(keyword, matchHint, keyword, keyword, keyword, keyword, keyword, keyword);
    }

    // ========== JSON 解析(与模型解耦,宽容处理) ==========

    /** 解析 AI 分析 JSON(包内可见便于测试) */
    AIAnalysis parseAnalysis(String response) throws Exception {
        Matcher m = JSON_OBJECT_PATTERN.matcher(response == null ? "" : response);
        if (!m.find()) {
            throw new IllegalArgumentException("AI 响应中没有 JSON 对象");
        }
        JsonNode node = objectMapper.readTree(m.group());
        AIAnalysis analysis = new AIAnalysis();
        analysis.setIsReal(node.path("isReal").asBoolean(true));
        analysis.setRelevance(Math.max(0, Math.min(100, node.path("relevance").asInt(0))));
        analysis.setRelevanceReason(truncate(node.path("relevanceReason").asText(""), 200));
        analysis.setKeywordMentioned(node.has("keywordMentioned") ? node.path("keywordMentioned").asBoolean(false) : null);
        String importance = node.path("importance").asText("low");
        analysis.setImportance(IMPORTANCE_LEVELS.contains(importance) ? importance : "low");
        analysis.setSummary(truncate(node.path("summary").asText(""), 150));
        return analysis;
    }

    /** 解析扩展关键词 JSON 数组(包内可见便于测试) */
    List<String> parseExpansion(String response) {
        if (response == null) {
            return List.of();
        }
        Matcher m = JSON_ARRAY_PATTERN.matcher(response);
        if (!m.find()) {
            return List.of();
        }
        try {
            JsonNode array = objectMapper.readTree(m.group());
            List<String> result = new ArrayList<>();
            if (array.isArray()) {
                for (JsonNode item : array) {
                    String s = item.asText("").trim();
                    if (!s.isEmpty()) {
                        result.add(s);
                    }
                }
            }
            return result;
        } catch (Exception e) {
            log.warn("解析扩展关键词失败: {}", e.getMessage());
            return List.of();
        }
    }

    // ========== 兜底 ==========

    private AIAnalysis fallbackAnalysis(KeywordUtils.PreMatchResult match, int matchedScore, int unmatchedScore) {
        AIAnalysis analysis = new AIAnalysis();
        analysis.setIsReal(true);
        analysis.setRelevance(match.matched() ? matchedScore : unmatchedScore);
        analysis.setRelevanceReason("AI 服务不可用,使用默认分数");
        analysis.setKeywordMentioned(match.matched());
        analysis.setImportance("low");
        analysis.setSummary("");
        return analysis;
    }

    private boolean hasApiKey() {
        // 占位 key 视为未配置,直接走降级,避免无意义的失败调用
        return apiKey != null && !apiKey.isBlank() && !PLACEHOLDER_API_KEY.equals(apiKey);
    }

    private List<String> mergeUnique(String keyword, List<String>... extras) {
        Set<String> set = new LinkedHashSet<>();
        set.add(keyword);
        for (List<String> extra : extras) {
            set.addAll(extra);
        }
        return new ArrayList<>(set);
    }

    private String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max);
    }
}
