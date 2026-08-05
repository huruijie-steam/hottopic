package com.yupi.hottopic.job;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yupi.hottopic.entity.Keyword;
import com.yupi.hottopic.mapper.KeywordMapper;
import com.yupi.hottopic.service.hotspot.HotspotChecker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 定时巡检任务(需求 HC-1:cron 见 application.yml app.monitor.cron,默认每 30 分钟)
 */
@Component
public class HotspotCheckJob {

    private static final Logger log = LoggerFactory.getLogger(HotspotCheckJob.class);

    private final HotspotChecker hotspotChecker;
    private final KeywordMapper keywordMapper;

    public HotspotCheckJob(HotspotChecker hotspotChecker, KeywordMapper keywordMapper) {
        this.hotspotChecker = hotspotChecker;
        this.keywordMapper = keywordMapper;
    }

    @Scheduled(cron = "${app.monitor.cron}")
    public void run() {
        log.info("🔄 开始定时巡检...");
        try {
            List<Keyword> keywords = keywordMapper.selectList(
                    new LambdaQueryWrapper<Keyword>().eq(Keyword::getIsActive, true));
            hotspotChecker.checkAll(keywords);
            log.info("✅ 定时巡检完成");
        } catch (Exception e) {
            log.error("❌ 定时巡检失败: {}", e.getMessage(), e);
        }
    }
}
