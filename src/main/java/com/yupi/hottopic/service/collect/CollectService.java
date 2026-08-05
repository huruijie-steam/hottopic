package com.yupi.hottopic.service.collect;

import com.yupi.hottopic.config.MonitorProperties;
import com.yupi.hottopic.dto.SearchResult;
import com.yupi.hottopic.util.HotspotResultUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 多源聚合采集(需求 HC-3/HC-5/HC-6/HC-7):
 * 并行抓取所有已注册 SourceFetcher,单源失败不影响整体,
 * 统一走 去重 → 新鲜度 → 来源优先级 管线。
 */
@Service
public class CollectService {

    private static final Logger log = LoggerFactory.getLogger(CollectService.class);

    private final List<SourceFetcher> fetchers;
    private final MonitorProperties props;

    public CollectService(List<SourceFetcher> fetchers, MonitorProperties props) {
        this.fetchers = fetchers;
        this.props = props;
    }

    /**
     * 并行抓取全部数据源并处理后返回。
     *
     * @return 去重、新鲜、按优先级排序后的结果
     */
    public List<SearchResult> collect(String query) {
        List<CompletableFuture<List<SearchResult>>> futures = new ArrayList<>();
        for (SourceFetcher fetcher : fetchers) {
            futures.add(CompletableFuture.supplyAsync(() -> fetcher.fetch(query)));
        }

        List<SearchResult> all = new ArrayList<>();
        for (int i = 0; i < fetchers.size(); i++) {
            SourceFetcher fetcher = fetchers.get(i);
            try {
                List<SearchResult> part = futures.get(i).join();
                log.info("{}: {} results", fetcher.source(), part.size());
                all.addAll(part);
            } catch (Exception e) {
                // 单源失败不影响整体(对应需求 HC-3)
                log.warn("{} fetch failed: {}", fetcher.source(), e.getMessage());
            }
        }

        List<SearchResult> unique = HotspotResultUtils.deduplicate(all);
        List<SearchResult> fresh = HotspotResultUtils.filterByFreshness(unique, props.getMaxAgeHours());
        List<SearchResult> sorted = HotspotResultUtils.prioritize(fresh);
        log.info("Aggregated \"{}\": {} raw → {} unique → {} fresh", query, all.size(), unique.size(), fresh.size());
        return sorted;
    }
}
