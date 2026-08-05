package com.yupi.hottopic.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 监控关键词(需求 KW-1 ~ KW-5)
 */
@Data
@TableName("keyword")
public class Keyword {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    /** 关键词文本,唯一 */
    private String text;

    /** 分类标签,如 AI/前端/后端 */
    private String category;

    /** 是否激活,false 时巡检跳过该词 */
    private Boolean isActive;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
