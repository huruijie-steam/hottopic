package com.yupi.hottopic.controller;

import com.yupi.hottopic.service.SettingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 设置接口(需求 ST-1 ~ ST-3;敏感 key 脱敏返回)
 */
@RestController
@RequestMapping("/api/settings")
public class SettingController {

    /** 敏感 key 匹配规则:含 password/key/token 的值打码 */
    private static final java.util.regex.Pattern SENSITIVE_PATTERN =
            java.util.regex.Pattern.compile("(?i)(password|passwd|api[_-]?key|token|secret)");

    private final SettingService settingService;

    public SettingController(SettingService settingService) {
        this.settingService = settingService;
    }

    /** 全部设置(脱敏) */
    @GetMapping
    public Map<String, String> getAll() {
        Map<String, String> all = settingService.getAll();
        Map<String, String> safe = new LinkedHashMap<>();
        all.forEach((k, v) -> safe.put(k, SENSITIVE_PATTERN.matcher(k).find() ? mask(v) : v));
        return safe;
    }

    /** 更新设置 */
    @PutMapping
    public ResponseEntity<?> update(@RequestBody Map<String, String> body) {
        if (body == null || body.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "设置不能为空"));
        }
        body.forEach(settingService::set);
        return ResponseEntity.ok(Map.of("message", "saved"));
    }

    private String mask(String value) {
        if (value == null || value.length() < 8) {
            return "******";
        }
        return value.substring(0, 3) + "******" + value.substring(value.length() - 3);
    }
}
