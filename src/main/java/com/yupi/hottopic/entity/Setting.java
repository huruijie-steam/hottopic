package com.yupi.hottopic.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * KV 设置(需求 ST-1 ~ ST-3)
 */
@Data
@TableName("setting")
public class Setting {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    private String key;

    private String value;
}
