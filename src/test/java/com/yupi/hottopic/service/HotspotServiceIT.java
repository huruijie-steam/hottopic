package com.yupi.hottopic.service;

import com.yupi.hottopic.dto.HotspotQuery;
import com.yupi.hottopic.dto.PageResult;
import com.yupi.hottopic.entity.Hotspot;
import com.yupi.hottopic.mapper.HotspotMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 热点查询服务集成测试(真实 H2 数据库:筛选/排序/分页/热度)
 */
@SpringBootTest
@ActiveProfiles("test")
class HotspotServiceIT {

    @Autowired
    private HotspotService hotspotService;

    @Autowired
    private HotspotMapper hotspotMapper;

    private String twitterId;
    private String bingId;
    private String lowId;

    @BeforeEach
    void setUp() {
        hotspotMapper.delete(null);

        twitterId = insert("Twitter 热门", "twitter", "urgent", 90, 1000L, 500L, 200L);
        bingId = insert("Bing 文章", "bing", "medium", 70, 100L, 10L, 5L);
        lowId = insert("低相关", "bing", "low", 30, 0L, 0L, 0L);
    }

    private String insert(String title, String source, String importance, int relevance,
                          Long like, Long retweet, Long comment) {
        Hotspot h = new Hotspot();
        h.setTitle(title);
        h.setUrl("https://example.com/" + title);
        h.setSource(source);
        h.setImportance(importance);
        h.setRelevance(relevance);
        h.setIsReal(true);
        h.setLikeCount(like);
        h.setRetweetCount(retweet);
        h.setCommentCount(comment);
        h.setCreatedAt(LocalDateTime.now());
        hotspotMapper.insert(h);
        return h.getId();
    }

    @Test
    void 无条件查询_全量返回并带热度分() {
        PageResult<Hotspot> result = hotspotService.query(new HotspotQuery());
        assertEquals(3, result.getTotal());
        assertTrue(result.getRecords().stream().allMatch(h -> h.getHeatScore() != null));
    }

    @Test
    void 按来源筛选() {
        HotspotQuery q = new HotspotQuery();
        q.setSource("twitter");
        PageResult<Hotspot> result = hotspotService.query(q);
        assertEquals(1, result.getTotal());
        assertEquals(twitterId, result.getRecords().get(0).getId());
    }

    @Test
    void 按重要度筛选() {
        HotspotQuery q = new HotspotQuery();
        q.setImportance("low");
        PageResult<Hotspot> result = hotspotService.query(q);
        assertEquals(1, result.getTotal());
        assertEquals(lowId, result.getRecords().get(0).getId());
    }

    @Test
    void 按重要性排序_urgent在前() {
        HotspotQuery q = new HotspotQuery();
        q.setSortBy("importance");
        PageResult<Hotspot> result = hotspotService.query(q);
        assertEquals("urgent", result.getRecords().get(0).getImportance());
        assertEquals("low", result.getRecords().get(2).getImportance());
    }

    @Test
    void 按热度排序_互动多的在前() {
        HotspotQuery q = new HotspotQuery();
        q.setSortBy("heat");
        PageResult<Hotspot> result = hotspotService.query(q);
        // twitter 互动最多(like 1000)
        assertEquals("Twitter 热门", result.getRecords().get(0).getTitle());
        assertEquals("低相关", result.getRecords().get(2).getTitle());
    }

    @Test
    void 按相关性排序() {
        HotspotQuery q = new HotspotQuery();
        q.setSortBy("relevance");
        PageResult<Hotspot> result = hotspotService.query(q);
        assertEquals(90, result.getRecords().get(0).getRelevance());
    }

    @Test
    void 分页() {
        HotspotQuery q = new HotspotQuery();
        q.setPage(2);
        q.setSize(2);
        PageResult<Hotspot> result = hotspotService.query(q);
        assertEquals(3, result.getTotal());
        assertEquals(1, result.getRecords().size());
    }

    @Test
    void 时间范围筛选() {
        // 把一条数据改为 2 天前,timeRange=d7 应全部返回,h1 应排除它
        Hotspot old = hotspotMapper.selectById(lowId);
        old.setCreatedAt(LocalDateTime.now().minusDays(2));
        hotspotMapper.updateById(old);

        HotspotQuery q7 = new HotspotQuery();
        q7.setTimeRange("d7");
        assertEquals(3, hotspotService.query(q7).getTotal());

        HotspotQuery q1 = new HotspotQuery();
        q1.setTimeRange("h1");
        assertEquals(2, hotspotService.query(q1).getTotal());
    }

    @Test
    void 详情_返回热度分() {
        Hotspot detail = hotspotService.getById(twitterId);
        assertNotNull(detail);
        assertTrue(detail.getHeatScore() > 0);
        assertNull(hotspotService.getById("not-exist"));
    }
}
