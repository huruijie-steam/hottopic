package com.yupi.hottopic.dto;

import lombok.Data;

/**
 * 关键词请求(需求 KW-1/KW-3/KW-4)
 */
@Data
public class KeywordRequest {

    /** 关键词文本(创建必填) */
    private String text;

    /** 分类标签 */
    private String category;

    /** 激活状态(更新时可选) */
    private Boolean isActive;
}
