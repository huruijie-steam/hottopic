package com.yupi.hottopic.dto;

import lombok.Data;

/**
 * AI 内容分析结果(需求 AI-2 ~ AI-6,输出协议见需求文档 §6.1)
 */
@Data
public class AIAnalysis {

    /** 是否真实内容(排除标题党/假新闻/营销软文) */
    private Boolean isReal;

    /** 相关性评分 0-100 */
    private Integer relevance;

    /** 相关性打分理由 */
    private String relevanceReason;

    /** 内容中是否直接提及关键词 */
    private Boolean keywordMentioned;

    /** 重要程度:low / medium / high / urgent */
    private String importance;

    /** 摘要:内容与关键词的关联 */
    private String summary;
}
