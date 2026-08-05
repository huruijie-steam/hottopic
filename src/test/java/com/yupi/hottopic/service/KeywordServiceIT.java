package com.yupi.hottopic.service;

import com.yupi.hottopic.dto.KeywordVO;
import com.yupi.hottopic.entity.Hotspot;
import com.yupi.hottopic.entity.Keyword;
import com.yupi.hottopic.mapper.HotspotMapper;
import com.yupi.hottopic.mapper.KeywordMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 关键词服务集成测试(真实 H2 数据库)
 */
@SpringBootTest
@ActiveProfiles("test")
class KeywordServiceIT {

    @Autowired
    private KeywordService keywordService;

    @Autowired
    private KeywordMapper keywordMapper;

    @Autowired
    private HotspotMapper hotspotMapper;

    @BeforeEach
    void clean() {
        hotspotMapper.delete(null);
        keywordMapper.delete(null);
    }

    @Test
    void 创建关键词_列表可见且默认激活() {
        Keyword k = keywordService.create("Spring Boot 4", "后端");
        assertNotNull(k.getId());
        assertTrue(k.getIsActive());

        List<KeywordVO> list = keywordService.listWithCounts();
        assertEquals(1, list.size());
        assertEquals("Spring Boot 4", list.get(0).getKeyword().getText());
        assertEquals(0, list.get(0).getHotspotCount());
    }

    @Test
    void 重复关键词_抛异常() {
        keywordService.create("AI 编程", null);
        assertThrows(IllegalArgumentException.class, () -> keywordService.create("AI 编程", null));
    }

    @Test
    void 空关键词_抛异常() {
        assertThrows(IllegalArgumentException.class, () -> keywordService.create("  ", null));
    }

    @Test
    void 更新启停() {
        Keyword k = keywordService.create("Claude", null);
        Keyword updated = keywordService.update(k.getId(), null, null, false);
        assertFalse(updated.getIsActive());
        Keyword updated2 = keywordService.update(k.getId(), "Claude Sonnet", null, true);
        assertEquals("Claude Sonnet", updated2.getText());
    }

    @Test
    void 删除关键词_关联热点保留且keywordId置空() {
        Keyword k = keywordService.create("Claude", null);
        Hotspot h = new Hotspot();
        h.setTitle("t");
        h.setUrl("https://example.com/1");
        h.setSource("bing");
        h.setKeywordId(k.getId());
        hotspotMapper.insert(h);

        keywordService.delete(k.getId());

        assertNull(keywordMapper.selectById(k.getId()));
        Hotspot remaining = hotspotMapper.selectById(h.getId());
        assertNotNull(remaining); // 热点保留
        assertNull(remaining.getKeywordId()); // keywordId 置空
    }
}
