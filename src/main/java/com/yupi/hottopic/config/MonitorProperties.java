package com.yupi.hottopic.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 热点巡检参数(app.monitor.*,对应 application.yml)
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.monitor")
public class MonitorProperties {

    /** 定时巡检 cron 表达式,默认每 30 分钟 */
    private String cron = "0 */30 * * * *";

    /** 新鲜度:保留多少小时内的内容 */
    private int maxAgeHours = 168;

    /** Twitter 每轮最多 AI 分析条数 */
    private int twitterQuota = 15;

    /** 其他来源合计配额 */
    private int otherQuota = 10;

    /** 相关性过滤阈值(低于丢弃) */
    private int relevanceThreshold = 50;

    /** 未提及关键词时的严格阈值 */
    private int strictThreshold = 65;

    /** 关键词之间的请求间隔(毫秒) */
    private long requestIntervalMs = 2000;
}
