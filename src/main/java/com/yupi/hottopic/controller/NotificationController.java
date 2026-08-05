package com.yupi.hottopic.controller;

import com.yupi.hottopic.dto.PageResult;
import com.yupi.hottopic.entity.Notification;
import com.yupi.hottopic.service.NotificationService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 通知接口(需求 NT-2)
 */
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /** 通知列表(分页,按时间倒序) */
    @GetMapping
    public PageResult<Notification> list(@RequestParam(defaultValue = "1") long page,
                                         @RequestParam(defaultValue = "20") long size) {
        return notificationService.page(page, size);
    }

    /** 未读数量 */
    @GetMapping("/unread-count")
    public Map<String, Long> unreadCount() {
        return Map.of("count", notificationService.countUnread());
    }

    /** 标记已读;body 为空标记全部 */
    @PostMapping("/read")
    public Map<String, String> markRead(@RequestBody(required = false) Map<String, String> body) {
        String id = body == null ? null : body.get("id");
        notificationService.markRead(id);
        return Map.of("message", "ok");
    }
}
