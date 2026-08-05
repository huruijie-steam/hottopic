package com.yupi.hottopic.dto;

import lombok.Data;

/**
 * 热点列表查询条件(需求 HP-3/HP-4)
 */
@Data
public class HotspotQuery {

    private long page = 1;
    private long size = 20;

    /** 来源:twitter/bing/hackernews/... */
    private String source;

    /** 重要程度:low/medium/high/urgent */
    private String importance;

    /** 关联关键词 id */
    private String keywordId;

    /** 时间范围:h1 / h24 / d7 / d30(按发现时间) */
    private String timeRange;

    /** 真实性:true 真实 / false 疑似虚假 */
    private Boolean isReal;

    /** 排序:createdAt(默认) / publishedAt / importance / relevance / heat */
    private String sortBy;
}
