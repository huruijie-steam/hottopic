package com.yupi.hottopic.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.yupi.hottopic.dto.KeywordVO;
import com.yupi.hottopic.entity.Hotspot;
import com.yupi.hottopic.entity.Keyword;
import com.yupi.hottopic.mapper.HotspotMapper;
import com.yupi.hottopic.mapper.KeywordMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 关键词管理(需求 KW-1 ~ KW-5)
 */
@Service
public class KeywordService {

    private final KeywordMapper keywordMapper;
    private final HotspotMapper hotspotMapper;

    public KeywordService(KeywordMapper keywordMapper, HotspotMapper hotspotMapper) {
        this.keywordMapper = keywordMapper;
        this.hotspotMapper = hotspotMapper;
    }

    /** 全部关键词列表 */
    public List<Keyword> listAll() {
        return keywordMapper.selectList(
                new LambdaQueryWrapper<Keyword>().orderByDesc(Keyword::getCreatedAt));
    }

    /** 关键词 + 关联热点数 */
    public List<KeywordVO> listWithCounts() {
        List<Keyword> keywords = listAll();
        List<KeywordVO> result = new ArrayList<>();
        for (Keyword k : keywords) {
            KeywordVO vo = new KeywordVO();
            vo.setKeyword(k);
            Long count = hotspotMapper.selectCount(
                    new LambdaQueryWrapper<Hotspot>().eq(Hotspot::getKeywordId, k.getId()));
            vo.setHotspotCount(count == null ? 0 : count);
            result.add(vo);
        }
        return result;
    }

    /**
     * 添加关键词(需求 KW-1,text 去重)。
     *
     * @throws IllegalArgumentException 已存在时
     */
    public Keyword create(String text, String category) {
        String trimmed = text == null ? "" : text.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("关键词不能为空");
        }
        Long exists = keywordMapper.selectCount(
                new LambdaQueryWrapper<Keyword>().eq(Keyword::getText, trimmed));
        if (exists != null && exists > 0) {
            throw new IllegalArgumentException("关键词已存在: " + trimmed);
        }
        Keyword keyword = new Keyword();
        keyword.setText(trimmed);
        keyword.setCategory(category);
        keyword.setIsActive(true);
        keywordMapper.insert(keyword);
        return keyword;
    }

    /**
     * 更新关键词(需求 KW-3/KW-4)。
     *
     * @throws IllegalArgumentException 关键词不存在
     */
    public Keyword update(String id, String text, String category, Boolean isActive) {
        Keyword keyword = keywordMapper.selectById(id);
        if (keyword == null) {
            throw new IllegalArgumentException("关键词不存在: " + id);
        }
        if (text != null && !text.isBlank() && !text.trim().equals(keyword.getText())) {
            String trimmed = text.trim();
            Long exists = keywordMapper.selectCount(
                    new LambdaQueryWrapper<Keyword>().eq(Keyword::getText, trimmed).ne(Keyword::getId, id));
            if (exists != null && exists > 0) {
                throw new IllegalArgumentException("关键词已存在: " + trimmed);
            }
            keyword.setText(trimmed);
        }
        if (category != null) {
            keyword.setCategory(category);
        }
        if (isActive != null) {
            keyword.setIsActive(isActive);
        }
        keywordMapper.updateById(keyword);
        return keywordMapper.selectById(id);
    }

    /**
     * 删除关键词(需求 KW-4:关联热点保留,keywordId 置空)。
     */
    @Transactional
    public void delete(String id) {
        // UpdateWrapper.set 显式置空(null 字段不会被默认策略忽略)
        hotspotMapper.update(null, new LambdaUpdateWrapper<Hotspot>()
                .eq(Hotspot::getKeywordId, id)
                .set(Hotspot::getKeywordId, null));
        keywordMapper.deleteById(id);
    }
}
