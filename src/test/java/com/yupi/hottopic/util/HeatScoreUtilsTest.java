package com.yupi.hottopic.util;

import com.yupi.hottopic.entity.Hotspot;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HeatScoreUtilsTest {

    private Hotspot hotspot(long like, long retweet, long comment, Long view) {
        Hotspot h = new Hotspot();
        h.setLikeCount(like);
        h.setRetweetCount(retweet);
        h.setCommentCount(comment);
        h.setViewCount(view);
        return h;
    }

    @Test
    void 无互动_得分为0() {
        assertEquals(0, HeatScoreUtils.calcHeatScore(hotspot(0, 0, 0, null)));
        assertEquals(0, HeatScoreUtils.calcHeatScore(new Hotspot()));
    }

    @Test
    void 高互动_得分高() {
        // 1万赞 + 5千转 + 2千评
        int score = HeatScoreUtils.calcHeatScore(hotspot(10000, 5000, 2000, null));
        // log10(1 + 10000*2+5000*3+2000*5) = log10(1+45000) ≈ 4.65 → 46
        assertEquals(47, score);
    }

    @Test
    void 权重_评论大于转发大于点赞() {
        Hotspot a = hotspot(100, 0, 0, null); // 200
        Hotspot b = hotspot(0, 0, 100, null); // 500
        Hotspot c = hotspot(0, 100, 0, null); // 300
        assertTrue(HeatScoreUtils.weightedValue(b) > HeatScoreUtils.weightedValue(c));
        assertTrue(HeatScoreUtils.weightedValue(c) > HeatScoreUtils.weightedValue(a));
    }

    @Test
    void 热度等级分级() {
        assertEquals("爆", HeatScoreUtils.heatLevel(95));
        assertEquals("热", HeatScoreUtils.heatLevel(75));
        assertEquals("温", HeatScoreUtils.heatLevel(55));
        assertEquals("凉", HeatScoreUtils.heatLevel(35));
        assertEquals("冷", HeatScoreUtils.heatLevel(10));
    }

    @Test
    void null互动按0处理() {
        Hotspot h = new Hotspot();
        h.setLikeCount(5L);
        // retweet/comment/view 保持 null
        assertEquals(10, HeatScoreUtils.weightedValue(h), 0.001);
    }
}
