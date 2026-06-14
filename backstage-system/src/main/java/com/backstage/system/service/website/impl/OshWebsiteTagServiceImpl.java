package com.backstage.system.service.website.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.backstage.common.utils.StringUtils;
import com.backstage.system.domain.website.OshWebsiteTag;
import com.backstage.system.domain.website.OshWebsiteTagRel;
import com.backstage.system.mapper.website.OshWebsiteTagMapper;
import com.backstage.system.mapper.website.OshWebsiteTagRelMapper;
import com.backstage.system.service.website.OshWebsiteTagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 实用网站标签 Service 实现
 * 参考课程模块 CourseManageServiceImpl 中的标签处理逻辑
 */
@Service
public class OshWebsiteTagServiceImpl extends ServiceImpl<OshWebsiteTagMapper, OshWebsiteTag>
        implements OshWebsiteTagService {

    @Autowired
    private OshWebsiteTagMapper websiteTagMapper;

    @Autowired
    private OshWebsiteTagRelMapper websiteTagRelMapper;

    // ==================== 标签查询 ====================

    @Override
    public List<Map<String, Object>> getAllTags() {
        return websiteTagMapper.selectAllTags();
    }

    @Override
    public List<Map<String, Object>> searchTags(String keyword) {
        return websiteTagMapper.selectTagsByKeyword(keyword);
    }

    // ==================== 标签解析（核心逻辑，参考课程模块） ====================

    /**
     * 解析标签：存在则返回，不存在则创建。
     * 含 DuplicateKeyException 并发安全兜底，与课程模块 resolveCourseTag 逻辑一致。
     */
    @Override
    public OshWebsiteTag resolveTag(String tagName, String operator) {
        // 先查是否已存在
        OshWebsiteTag existing = websiteTagMapper.selectByTagName(tagName);
        if (existing != null) {
            return existing;
        }
        // 不存在则创建
        OshWebsiteTag tag = buildTagForCreate(tagName, operator);
        try {
            websiteTagMapper.insertWebsiteTag(tag);
            return tag;
        } catch (DuplicateKeyException ex) {
            // 并发场景：另一个线程刚好也在创建同名标签，重新查一次
            OshWebsiteTag retry = websiteTagMapper.selectByTagName(tagName);
            if (retry != null) {
                return retry;
            }
            throw ex;
        }
    }

    // ==================== 标签绑定 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void bindWebsiteTags(Long websiteId, List<String> tagNames, String operator) {
        List<String> normalized = normalizeTagNames(tagNames);
        if (normalized.isEmpty()) {
            return;
        }
        for (String tagName : normalized) {
            OshWebsiteTag tag = resolveTag(tagName, operator);
            insertTagRelation(websiteId, tag.getId());
            websiteTagMapper.increaseUseCount(tag.getId());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rebuildWebsiteTags(Long websiteId, List<String> tagNames, String operator) {
        // 先软删除旧关联
        websiteTagRelMapper.deleteByWebsiteId(websiteId);
        // 再绑定新标签
        bindWebsiteTags(websiteId, tagNames, operator);
    }

    // ==================== 私有工具方法 ====================

    /**
     * 清洗标签名称列表：去空白、去重，保持顺序。
     * 与课程模块 normalizeCourseTags 逻辑一致。
     */
    static List<String> normalizeTagNames(List<String> tagNames) {
        if (tagNames == null || tagNames.isEmpty()) {
            return Collections.emptyList();
        }
        // 用 LinkedHashMap 保序去重
        Map<String, String> seen = new LinkedHashMap<>();
        for (String name : tagNames) {
            String trimmed = StringUtils.trimToNull(name);
            if (trimmed != null) {
                seen.putIfAbsent(trimmed, trimmed);
            }
        }
        return new ArrayList<>(seen.values());
    }

    /**
     * 构建新标签对象
     */
    private OshWebsiteTag buildTagForCreate(String tagName, String operator) {
        OshWebsiteTag tag = new OshWebsiteTag();
        Date now = new Date();
        tag.setTagName(tagName);
        tag.setSortOrder(0);
        tag.setUseCount(0);
        tag.setDeleteFlag(0);
        tag.setCreateBy(operator);
        tag.setCreateTime(now);
        tag.setUpdateBy(operator);
        tag.setUpdateTime(now);
        return tag;
    }

    /**
     * 插入网站-标签关联记录
     */
    private void insertTagRelation(Long websiteId, Long tagId) {
        OshWebsiteTagRel rel = new OshWebsiteTagRel();
        Date now = new Date();
        rel.setWebsiteId(websiteId);
        rel.setTagId(tagId);
        rel.setDeleteFlag(0);
        rel.setCreateTime(now);
        rel.setUpdateTime(now);
        websiteTagRelMapper.insert(rel);
    }
}
