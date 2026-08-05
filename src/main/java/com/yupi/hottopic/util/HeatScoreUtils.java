package com.yupi.hottopic.util;

import com.yupi.hottopic.entity.Hotspot;

/**
 * 热度计算(需求 HP-5:互动量加权后 log10 压缩到 0-100,分五级)
 */
public final class HeatScoreUtils {

    private HeatScoreUtils() {
    }

    /**
     * 计算综合热度分(0-100):
     * 原始加权 = 点赞×2 + 转发×3 + 评论×5 + 弹幕×2 + 浏览×0.01
     * 再经 log10 压缩映射到 0-100(log10(1+x) 单调保序,仅用于展示分)。
     */
    public static int calcHeatScore(Hotspot h) {
        double weighted = weightedValue(h);
        if (weighted <= 0) {
            return 0;
        }
        double logValue = Math.log10(1 + weighted);
        // 原始项目:log10 后线性映射到 0-100(约 log10(10^10)=10 → 100)
        return (int) Math.min(100, Math.round(logValue * 10));
    }

    /**
     * 排序用原始加权值(log10 单调,排序直接用原始值即可,避免 null 计算)
     */
    public static double weightedValue(Hotspot h) {
        if (h == null) {
            return 0;
        }
        return nvl(h.getLikeCount()) * 2
                + nvl(h.getRetweetCount()) * 3
                + nvl(h.getCommentCount()) * 5
                + nvl(h.getDanmakuCount()) * 2
                + nvl(h.getViewCount()) * 0.01;
    }

    /** 热度等级:爆/热/温/凉/冷 */
    public static String heatLevel(int score) {
        if (score >= 90) {
            return "爆";
        }
        if (score >= 70) {
            return "热";
        }
        if (score >= 50) {
            return "温";
        }
        if (score >= 30) {
            return "凉";
        }
        return "冷";
    }

    private static long nvl(Long v) {
        return v == null ? 0 : v;
    }
}
