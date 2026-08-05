package com.yupi.hottopic.service.collect;

import com.yupi.hottopic.dto.SearchResult;

import java.util.List;

/**
 * 数据源采集器统一接口(需求 HC-3,单源故障不影响整体)
 */
public interface SourceFetcher {

    /** 来源标识,与 hotspot.source 一致 */
    String source();

    /** 按关键词抓取,失败返回空列表(不抛异常) */
    List<SearchResult> fetch(String query);
}
