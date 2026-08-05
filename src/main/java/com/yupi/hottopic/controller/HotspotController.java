package com.yupi.hottopic.controller;

import com.yupi.hottopic.dto.HotspotQuery;
import com.yupi.hottopic.dto.PageResult;
import com.yupi.hottopic.dto.SearchResult;
import com.yupi.hottopic.entity.Hotspot;
import com.yupi.hottopic.service.HotspotService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 热点接口(需求 HP-1 ~ HP-7 / SR-1)
 */
@RestController
@RequestMapping("/api/hotspots")
public class HotspotController {

    private final HotspotService hotspotService;

    public HotspotController(HotspotService hotspotService) {
        this.hotspotService = hotspotService;
    }

    /** 热点列表:分页 + 筛选 + 排序 */
    @GetMapping
    public PageResult<Hotspot> list(HotspotQuery query) {
        return hotspotService.query(query);
    }

    /** 热点详情 */
    @GetMapping("/{id}")
    public ResponseEntity<?> detail(@PathVariable String id) {
        Hotspot hotspot = hotspotService.getById(id);
        if (hotspot == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(hotspot);
    }

    /** 全网搜索(不落库,聚合返回) */
    @PostMapping("/search")
    public ResponseEntity<?> search(@RequestBody Map<String, String> body) {
        String query = body == null ? null : body.get("query");
        if (query == null || query.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "query 不能为空"));
        }
        List<SearchResult> results = hotspotService.search(query.trim());
        return ResponseEntity.ok(results);
    }
}
