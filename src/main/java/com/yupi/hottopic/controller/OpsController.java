package com.yupi.hottopic.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yupi.hottopic.entity.Keyword;
import com.yupi.hottopic.mapper.KeywordMapper;
import com.yupi.hottopic.service.hotspot.HotspotChecker;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 运维接口(需求 §7.1:健康检查 + 手动触发巡检 HC-2)
 */
@RestController
@RequestMapping("/api")
public class OpsController {

    private final HotspotChecker hotspotChecker;
    private final KeywordMapper keywordMapper;

    public OpsController(HotspotChecker hotspotChecker, KeywordMapper keywordMapper) {
        this.hotspotChecker = hotspotChecker;
        this.keywordMapper = keywordMapper;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "ok");
        result.put("timestamp", LocalDateTime.now());
        return result;
    }

    @PostMapping("/check-hotspots")
    public Map<String, Object> checkHotspots() {
        List<Keyword> keywords = keywordMapper.selectList(
                new LambdaQueryWrapper<Keyword>().eq(Keyword::getIsActive, true));
        hotspotChecker.checkAll(keywords);
        Map<String, Object> result = new HashMap<>();
        result.put("message", "Hotspot check completed");
        result.put("checkedKeywords", keywords.size());
        return result;
    }
}
