package com.yupi.hottopic.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 热点(需求 HC-1 ~ HC-9 / HP-1 ~ HP-7)
 * (url, source) 唯一,保证重复巡检不重复入库
 */
@Data
@TableName("hotspot")
public class Hotspot {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    private String title;

    private String content;

    private String url;

    /** 来源:twitter / bing / google / duckduckgo / hackernews / sogou / bilibili / weibo */
    private String source;

    /** 原始平台 ID(推文 ID、bvid 等) */
    private String sourceId;

    /** AI 判定:是否真实内容 */
    private Boolean isReal;

    /** AI 相关性评分 0-100 */
    private Integer relevance;

    /** AI 相关性打分理由 */
    private String relevanceReason;

    /** 内容中是否直接提及关键词 */
    private Boolean keywordMentioned;

    /** 重要程度:low / medium / high / urgent */
    private String importance;

    /** AI 摘要(内容与关键词的关联) */
    private String summary;

    private Long viewCount;
    private Long likeCount;
    private Long retweetCount;
    private Long replyCount;
    private Long commentCount;
    private Long quoteCount;
    private Long danmakuCount;

    private String authorName;
    private String authorUsername;
    private String authorAvatar;
    private Long authorFollowers;
    private Boolean authorVerified;

    private LocalDateTime publishedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 关联关键词;关键词删除后置空(需求 KW-4) */
    private String keywordId;
}
