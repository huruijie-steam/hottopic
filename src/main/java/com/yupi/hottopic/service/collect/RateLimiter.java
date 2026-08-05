package com.yupi.hottopic.service.collect;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 单源频率限制器(需求 §5 数据源规格:每个源独立限流,避免被封)
 */
public class RateLimiter {

    private static final Logger log = LoggerFactory.getLogger(RateLimiter.class);

    private volatile long lastRequestTime = 0;
    private final long minIntervalMs;

    public RateLimiter(long minIntervalMs) {
        this.minIntervalMs = minIntervalMs;
    }

    /** 等待直到满足最小请求间隔 */
    public void waitIfNeeded() {
        long elapsed = System.currentTimeMillis() - lastRequestTime;
        if (elapsed < minIntervalMs) {
            try {
                Thread.sleep(minIntervalMs - elapsed);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("RateLimiter interrupted", e);
            }
        }
        lastRequestTime = System.currentTimeMillis();
    }
}
