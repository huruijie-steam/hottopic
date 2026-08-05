package com.yupi.hottopic.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yupi.hottopic.entity.Setting;
import com.yupi.hottopic.mapper.SettingMapper;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * KV 设置(需求 ST-1 ~ ST-3)
 */
@Service
public class SettingService {

    private final SettingMapper settingMapper;

    public SettingService(SettingMapper settingMapper) {
        this.settingMapper = settingMapper;
    }

    /** 全部设置 */
    public Map<String, String> getAll() {
        Map<String, String> map = new LinkedHashMap<>();
        List<Setting> settings = settingMapper.selectList(null);
        for (Setting s : settings) {
            map.put(s.getKey(), s.getValue());
        }
        return map;
    }

    /** 读取单个设置 */
    public String get(String key, String defaultValue) {
        Setting setting = settingMapper.selectOne(
                new LambdaQueryWrapper<Setting>().eq(Setting::getKey, key));
        return setting == null ? defaultValue : setting.getValue();
    }

    /** 写入/更新设置 */
    public void set(String key, String value) {
        Setting existing = settingMapper.selectOne(
                new LambdaQueryWrapper<Setting>().eq(Setting::getKey, key));
        if (existing == null) {
            Setting setting = new Setting();
            setting.setKey(key);
            setting.setValue(value);
            settingMapper.insert(setting);
        } else {
            existing.setValue(value);
            settingMapper.updateById(existing);
        }
    }
}
