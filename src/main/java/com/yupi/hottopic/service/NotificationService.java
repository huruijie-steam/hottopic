package com.yupi.hottopic.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yupi.hottopic.dto.PageResult;
import com.yupi.hottopic.entity.Notification;
import com.yupi.hottopic.mapper.NotificationMapper;
import org.springframework.stereotype.Service;

/**
 * 站内通知(需求 NT-2)
 */
@Service
public class NotificationService {

    private final NotificationMapper notificationMapper;

    public NotificationService(NotificationMapper notificationMapper) {
        this.notificationMapper = notificationMapper;
    }

    /** 创建通知并返回完整实体 */
    public Notification create(String type, String title, String content, String hotspotId) {
        Notification notification = new Notification();
        notification.setType(type);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setIsRead(false);
        notification.setHotspotId(hotspotId);
        notificationMapper.insert(notification);
        return notification;
    }

    /** 分页查询通知(按时间倒序) */
    public PageResult<Notification> page(long page, long size) {
        Page<Notification> result = notificationMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<Notification>().orderByDesc(Notification::getCreatedAt));
        return PageResult.of(result.getRecords(), result.getTotal(), page, size);
    }

    /** 标记已读;id 为空则全部标记 */
    public void markRead(String id) {
        if (id == null || id.isBlank()) {
            Notification update = new Notification();
            update.setIsRead(true);
            notificationMapper.update(update, new LambdaQueryWrapper<Notification>().eq(Notification::getIsRead, false));
        } else {
            Notification notification = notificationMapper.selectById(id);
            if (notification != null && !Boolean.TRUE.equals(notification.getIsRead())) {
                notification.setIsRead(true);
                notificationMapper.updateById(notification);
            }
        }
    }

    /** 未读数量 */
    public long countUnread() {
        return notificationMapper.selectCount(
                new LambdaQueryWrapper<Notification>().eq(Notification::getIsRead, false));
    }
}
