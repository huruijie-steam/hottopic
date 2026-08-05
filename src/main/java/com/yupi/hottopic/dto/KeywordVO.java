package com.yupi.hottopic.dto;

import com.yupi.hottopic.entity.Keyword;
import lombok.Data;

/**
 * 关键词列表项(含热点统计)
 */
@Data
public class KeywordVO {

    private Keyword keyword;

    /** 关联热点总数 */
    private long hotspotCount;
}
