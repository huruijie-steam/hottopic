package com.yupi.hottopic.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yupi.hottopic.dto.HotspotQuery;
import com.yupi.hottopic.dto.PageResult;
import com.yupi.hottopic.dto.SearchResult;
import com.yupi.hottopic.entity.Hotspot;
import com.yupi.hottopic.mapper.HotspotMapper;
import com.yupi.hottopic.service.collect.AccountDetector;
import com.yupi.hottopic.service.collect.CollectService;
import com.yupi.hottopic.util.HeatScoreUtils;
import com.yupi.hottopic.util.HotspotResultUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 热点查询(需求 HP-1 ~ HP-7 / SR-1)
 */
@Service
public class HotspotService {

    private final HotspotMapper hotspotMapper;
    private final CollectService collectService;
    private final AccountDetector accountDetector;

    public HotspotService(HotspotMapper hotspotMapper,
                          CollectService collectService,
                          AccountDetector accountDetector) {
        this.hotspotMapper = hotspotMapper;
        this.collectService = collectService;
        this.accountDetector = accountDetector;
    }

    /** 分页查询:多条件筛选 + 多维度排序(需求 HP-3/HP-4/HP-6) */
    public PageResult<Hotspot> query(HotspotQuery q) {
        long page = q.getPage() <= 0 ? 1 : q.getPage();
        long size = q.getSize() <= 0 ? 20 : Math.min(q.getSize(), 100);

        QueryWrapper<Hotspot> wrapper = new QueryWrapper<>();
        if (q.getSource() != null && !q.getSource().isBlank()) {
            wrapper.eq("source", q.getSource());
        }
        if (q.getImportance() != null && !q.getImportance().isBlank()) {
            wrapper.eq("importance", q.getImportance());
        }
        if (q.getKeywordId() != null && !q.getKeywordId().isBlank()) {
            wrapper.eq("keyword_id", q.getKeywordId());
        }
        if (q.getIsReal() != null) {
            wrapper.eq("is_real", q.getIsReal());
        }
        if (q.getTimeRange() != null && !q.getTimeRange().isBlank()) {
            LocalDateTime cutoff = timeRangeCutoff(q.getTimeRange());
            if (cutoff != null) {
                wrapper.ge("created_at", cutoff);
            }
        }
        applySort(wrapper, q.getSortBy());

        Page<Hotspot> pageResult = hotspotMapper.selectPage(new Page<>(page, size), wrapper);
        List<Hotspot> records = pageResult.getRecords();
        // 填充热度分(HP-5)
        records.forEach(h -> h.setHeatScore(HeatScoreUtils.calcHeatScore(h)));
        return PageResult.of(records, pageResult.getTotal(), page, size);
    }

    /** 热点详情(需求 HP-7) */
    public Hotspot getById(String id) {
        Hotspot hotspot = hotspotMapper.selectById(id);
        if (hotspot != null) {
            hotspot.setHeatScore(HeatScoreUtils.calcHeatScore(hotspot));
        }
        return hotspot;
    }

    /**
     * 全网搜索(需求 SR-1:账号检测 + 多源聚合 + 去重,不落库)。
     * 输入 UP 主名字时,优先返回其最新内容。
     */
    public List<SearchResult> search(String query) {
        List<SearchResult> merged = new ArrayList<>();
        // 账号检测:关键词是 B 站/微博账号时拉取最新内容(需求 HC-4 的搜索场景复用)
        AccountDetector.DetectResult accountResult = accountDetector.detectAndFetch(query);
        if (!accountResult.results().isEmpty()) {
            merged.addAll(accountResult.results());
        }
        merged.addAll(collectService.collect(query));
        return HotspotResultUtils.prioritize(HotspotResultUtils.deduplicate(merged));
    }

    private void applySort(QueryWrapper<Hotspot> wrapper, String sortBy) {
        String sort = sortBy == null ? "" : sortBy;
        switch (sort) {
            case "publishedAt" -> wrapper.orderByDesc("published_at");
            case "importance" -> wrapper.last("ORDER BY CASE importance WHEN 'urgent' THEN 4" +
                    " WHEN 'high' THEN 3 WHEN 'medium' THEN 2 ELSE 1 END DESC, created_at DESC");
            case "relevance" -> wrapper.orderByDesc("relevance");
            case "heat" -> wrapper.last("ORDER BY (COALESCE(like_count,0)*2 + COALESCE(retweet_count,0)*3" +
                    " + COALESCE(comment_count,0)*5 + COALESCE(danmaku_count,0)*2 + COALESCE(view_count,0)*0.01) DESC");
            default -> wrapper.orderByDesc("created_at");
        }
    }

    private LocalDateTime timeRangeCutoff(String timeRange) {
        LocalDateTime now = LocalDateTime.now();
        return switch (timeRange) {
            case "h1" -> now.minusHours(1);
            case "h24" -> now.minusHours(24);
            case "d7" -> now.minusDays(7);
            case "d30" -> now.minusDays(30);
            default -> null;
        };
    }
}
