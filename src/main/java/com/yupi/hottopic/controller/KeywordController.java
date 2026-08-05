package com.yupi.hottopic.controller;

import com.yupi.hottopic.dto.KeywordRequest;
import com.yupi.hottopic.dto.KeywordVO;
import com.yupi.hottopic.entity.Keyword;
import com.yupi.hottopic.service.KeywordService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 关键词管理接口(需求 KW-1 ~ KW-5)
 */
@RestController
@RequestMapping("/api/keywords")
public class KeywordController {

    private final KeywordService keywordService;

    public KeywordController(KeywordService keywordService) {
        this.keywordService = keywordService;
    }

    /** 关键词列表(含热点数) */
    @GetMapping
    public List<KeywordVO> list() {
        return keywordService.listWithCounts();
    }

    /** 添加关键词 */
    @PostMapping
    public ResponseEntity<?> create(@RequestBody KeywordRequest request) {
        try {
            Keyword keyword = keywordService.create(request.getText(), request.getCategory());
            return ResponseEntity.ok(keyword);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** 编辑/激活/暂停 */
    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable String id, @RequestBody KeywordRequest request) {
        try {
            Keyword keyword = keywordService.update(id, request.getText(), request.getCategory(), request.getIsActive());
            return ResponseEntity.ok(keyword);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** 删除(关联热点保留) */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable String id) {
        keywordService.delete(id);
        return ResponseEntity.ok(Map.of("message", "deleted"));
    }
}
