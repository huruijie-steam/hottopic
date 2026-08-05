package com.yupi.hottopic.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 站内通知(需求 NT-2)
 */
@Data
@TableName("notification")
public class Notification {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    /** 类型:hotspot / alert */
    private String type;

    private String title;

    private String content;

    private Boolean isRead;

    private String hotspotId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
