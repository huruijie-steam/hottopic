package com.yupi.hottopic.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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

    /** 未读数量 */
    public long countUnread() {
        return notificationMapper.selectCount(
                new LambdaQueryWrapper<Notification>().eq(Notification::getIsRead, false));
    }
}
