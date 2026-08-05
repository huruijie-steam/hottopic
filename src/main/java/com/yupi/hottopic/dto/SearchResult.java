package com.yupi.hottopic.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 采集统一结果(需求 HC-3),对应原项目 SearchResult
 */
@Data
public class SearchResult {

    private String title;

    private String content;

    private String url;

    /** 来源标识:twitter / bing / google / duckduckgo / hackernews / sogou / bilibili / weibo */
    private String source;

    /** 原始平台 ID(推文 ID、bvid 等) */
    private String sourceId;

    private LocalDateTime publishedAt;

    /** 互动数据(各源按可用性填充) */
    private Long viewCount;
    private Long likeCount;
    private Long retweetCount;
    private Long replyCount;
    private Long commentCount;
    private Long quoteCount;
    private Long danmakuCount;

    /** 作者信息 */
    private String authorName;
    private String authorUsername;
    private String authorAvatar;
    private Long authorFollowers;
    private Boolean authorVerified;

    /** 附加信息(如 HN 的 points) */
    private Integer score;
}
