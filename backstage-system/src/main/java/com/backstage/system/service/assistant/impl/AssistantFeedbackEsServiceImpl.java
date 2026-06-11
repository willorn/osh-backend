package com.backstage.system.service.assistant.impl;

import cn.hutool.core.util.StrUtil;
import com.backstage.common.annotation.DistributeLock;
import com.backstage.common.core.redis.RedisCache;
import com.backstage.common.response.PageResponse;
import com.backstage.system.domain.assistant.AssistantFeedback;
import com.backstage.system.domain.assistant.AssistantFeedbackCategory;
import com.backstage.system.domain.assistant.AssistantFeedbackFavorite;
import com.backstage.system.domain.assistant.AssistantTicketStatus;
import com.backstage.system.domain.assistant.dto.AssistantFeedbackPageDTO;
import com.backstage.system.domain.assistant.es.AssistantFeedbackEsDocument;
import com.backstage.system.domain.assistant.vo.AssistantFeedbackTagVO;
import com.backstage.system.mapper.assistant.AssistantFeedbackEsMapper;
import com.backstage.system.mapper.assistant.AssistantFeedbackMapper;
import com.backstage.system.service.assistant.IAssistantFeedbackCategoryService;
import com.backstage.system.service.assistant.IAssistantFeedbackEsService;
import com.backstage.system.service.assistant.IAssistantFeedbackTagService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class AssistantFeedbackEsServiceImpl implements IAssistantFeedbackEsService {

    private static final Logger log = LoggerFactory.getLogger(AssistantFeedbackEsServiceImpl.class);
    private static final int PAGE_SIZE = 200;
    private static final String QUERY_MODE_MINE = "mine";
    private static final String QUERY_MODE_FAVORITE = "favorite";
    private static final String REBUILD_TARGET_INDEX_CACHE_KEY = "assistant:feedback:es:rebuild:target-index";
    private static final int REBUILD_TARGET_INDEX_CACHE_MINUTES = 30;

    private final AssistantFeedbackEsMapper assistantFeedbackEsMapper;
    private final AssistantFeedbackMapper assistantFeedbackMapper;
    private final IAssistantFeedbackCategoryService categoryService;
    private final IAssistantFeedbackTagService feedbackTagService;
    private final RedisCache redisCache;

    public AssistantFeedbackEsServiceImpl(AssistantFeedbackEsMapper assistantFeedbackEsMapper,
                                          AssistantFeedbackMapper assistantFeedbackMapper,
                                          IAssistantFeedbackCategoryService categoryService,
                                          IAssistantFeedbackTagService feedbackTagService,
                                          RedisCache redisCache) {
        this.assistantFeedbackEsMapper = assistantFeedbackEsMapper;
        this.assistantFeedbackMapper = assistantFeedbackMapper;
        this.categoryService = categoryService;
        this.feedbackTagService = feedbackTagService;
        this.redisCache = redisCache;
    }

    @Override
    public PageResponse<AssistantFeedback> searchFeedbacks(AssistantFeedbackPageDTO dto) {
        AssistantFeedbackPageDTO request = normalizeRequest(dto);
        String queryMode = normalizeQueryMode(request.getQueryMode());
        if ((QUERY_MODE_MINE.equals(queryMode) || QUERY_MODE_FAVORITE.equals(queryMode)) && request.getUserId() == null) {
            return PageResponse.of(Collections.emptyList(), 0L, request.getPageNum(), request.getPageSize());
        }
        List<Long> favoriteFeedbackIds = QUERY_MODE_FAVORITE.equals(queryMode) ? listFavoriteFeedbackIds(request.getUserId()) : null;
        try {
            return assistantFeedbackEsMapper.searchFeedbacks(request, favoriteFeedbackIds);
        } catch (Exception exception) {
            throw new IllegalStateException("search feedbacks from es failed", exception);
        }
    }

    @Override
    public int syncAllFeedbacksToEs() {
        try {
            assistantFeedbackEsMapper.deleteAllFeedbacks();
            return syncAllFeedbacksFromMysql(null);
        } catch (Exception exception) {
            throw new IllegalStateException("sync feedbacks to es failed", exception);
        }
    }

    @Override
    public void upsertFeedbackById(Long feedbackId) {
        if (feedbackId == null) {
            return;
        }
        AssistantFeedback feedback = assistantFeedbackMapper.selectById(feedbackId);
        if (feedback == null || feedback.getDeleteFlag() != null && feedback.getDeleteFlag() != (byte) 0) {
            deleteFeedbackById(feedbackId);
            return;
        }
        try {
            Map<Long, AssistantFeedbackCategory> categoryMap = buildCategoryMap(Collections.singleton(feedback.getCategoryId()));
            Map<Long, List<AssistantFeedbackTagVO>> tagMap = feedbackTagService.mapFeedbackTags(Collections.singleton(feedbackId));
            upsertFeedbackWithRebuildMirror(buildDocument(feedback, categoryMap, tagMap));
        } catch (Exception exception) {
            throw new IllegalStateException("upsert feedback to es failed", exception);
        }
    }

    @Override
    public void deleteFeedbackById(Long feedbackId) {
        if (feedbackId == null) {
            return;
        }
        try {
            deleteFeedbackWithRebuildMirror(feedbackId);
        } catch (Exception exception) {
            throw new IllegalStateException("delete feedback from es failed", exception);
        }
    }

    /**
     * 重建 ES 索引（删除旧索引 → 创建新索引 → 全量同步数据）
     * <p>
     * 适用场景：
     * 1. mapping 变更（如字段类型从 text 改为 keyword）
     * 2. 分片数、副本数等 settings 调整
     * 3. 分析器配置变更
     * </p>
     * 执行期间 ES 查询会降级到 MySQL，业务不中断。
     * <p>
     * 使用分布式锁（expireTime=10min）防止多实例并发触发：
     * waitTime=0 表示获取锁失败立即返回，而不是阻塞等待，
     * 避免第二个请求排队等候第一个执行完再重复执行一次。
     * </p>
     *
     * @return 同步的文档总数
     */
    @DistributeLock(
            scene = "feedback:es",
            key = "rebuild",
            includeUserId = false,
            waitTime = 0,
            expireTime = 10 * 60 * 1000,
            releaseImmediately = true
    )
    @Override
    public int rebuildIndex() {
        String oldIndexName = null;
        String targetIndexName = null;
        boolean aliasSwitched = false;
        try {
            // 1. 解析当前在线索引，创建下一代物理索引，并标记本次重建目标。
            log.info("[feedback-es] start rebuild index with zero-downtime alias switch");
            String indexDefinitionJson = AssistantFeedbackEsMapper.loadMappingJson();
            oldIndexName = assistantFeedbackEsMapper.resolveCurrentPhysicalIndex();
            log.info("[feedback-es] current physical index: {}", oldIndexName == null ? "none" : oldIndexName);
            targetIndexName = assistantFeedbackEsMapper.buildNextPhysicalIndexName();
            if (targetIndexName.equals(oldIndexName)) {
                throw new IllegalStateException("feedback rebuild target index conflicts with current online index");
            }
            assistantFeedbackEsMapper.createPhysicalIndex(targetIndexName, indexDefinitionJson);
            markRebuildTargetIndex(targetIndexName);
            log.info("[feedback-es] new physical index created: {}", targetIndexName);

            // 2. 从 MySQL 全量同步反馈数据到新索引，确保切换前数据完整。
            int total = syncAllFeedbacksFromMysql(targetIndexName);
            log.info("[feedback-es] data synced to new index, total: {}", total);

            // 3. 将读写别名切换到新索引，旧索引暂时保留，便于回滚与排查。
            assistantFeedbackEsMapper.switchAlias(oldIndexName, targetIndexName);
            aliasSwitched = true;
            log.info("[feedback-es] alias switched to {}, old index retained: {}", targetIndexName,
                    oldIndexName == null ? "none" : oldIndexName);
            return total;
        } catch (Exception exception) {
            if (!aliasSwitched) {
                cleanupFailedRebuildTarget(oldIndexName, targetIndexName);
            }
            log.error("[feedback-es] rebuild index failed", exception);
            throw new IllegalStateException("rebuild feedback es index failed", exception);
        } finally {
            clearRebuildTargetIndex();
        }
    }

    /**
     * 从 MySQL 全量同步数据。
     *
     * @param targetIndex 目标索引名，null 时写入读别名（数据修复场景），非 null 时写入指定物理索引（重建场景）
     */
    private int syncAllFeedbacksFromMysql(String targetIndex) throws Exception {
        int pageNum = 1;
        int total = 0;
        while (true) {
            Page<AssistantFeedback> page = assistantFeedbackMapper.selectPage(
                    new Page<>(pageNum, PAGE_SIZE),
                    Wrappers.<AssistantFeedback>lambdaQuery()
                            .eq(AssistantFeedback::getDeleteFlag, (byte) 0)
                            .orderByAsc(AssistantFeedback::getId));
            List<AssistantFeedback> records = page.getRecords();
            if (records == null || records.isEmpty()) {
                break;
            }
            List<AssistantFeedbackEsDocument> documents = buildDocuments(records);
            total += targetIndex == null
                    ? assistantFeedbackEsMapper.bulkUpsertFeedbacks(documents)
                    : assistantFeedbackEsMapper.bulkUpsertFeedbacksToIndex(documents, targetIndex);
            log.info("[feedback-es] synced batch {}, count: {}, total: {}", pageNum, records.size(), total);
            if (records.size() < PAGE_SIZE) {
                break;
            }
            pageNum++;
        }
        return total;
    }

    private AssistantFeedbackPageDTO normalizeRequest(AssistantFeedbackPageDTO dto) {
        AssistantFeedbackPageDTO request = dto == null ? new AssistantFeedbackPageDTO() : dto;
        request.setPageNum(request.getPageNum() == null || request.getPageNum() < 1 ? 1 : request.getPageNum());
        request.setPageSize(request.getPageSize() == null || request.getPageSize() < 1 ? 10 : request.getPageSize());
        request.setQueryMode(normalizeQueryMode(request.getQueryMode()));
        request.setCategoryCode(StrUtil.isBlank(request.getCategoryCode()) ? null : request.getCategoryCode().trim());
        request.setKeyword(StrUtil.isBlank(request.getKeyword()) ? null : request.getKeyword().trim());
        request.setSortType(StrUtil.isBlank(request.getSortType()) ? null : request.getSortType().trim().toLowerCase());
        return request;
    }

    private List<Long> listFavoriteFeedbackIds(Long userId) {
        if (userId == null) {
            return Collections.emptyList();
        }
        return Db.lambdaQuery(AssistantFeedbackFavorite.class)
                .eq(AssistantFeedbackFavorite::getUserId, userId)
                .list()
                .stream()
                .map(AssistantFeedbackFavorite::getFeedbackId)
                .collect(Collectors.toList());
    }

    private List<AssistantFeedbackEsDocument> buildDocuments(List<AssistantFeedback> feedbackList) {
        Map<Long, AssistantFeedbackCategory> categoryMap = buildCategoryMap(feedbackList.stream()
                .map(AssistantFeedback::getCategoryId)
                .collect(Collectors.toSet()));
        Map<Long, List<AssistantFeedbackTagVO>> tagMap = feedbackTagService.mapFeedbackTags(feedbackList.stream()
                .map(AssistantFeedback::getId)
                .collect(Collectors.toSet()));
        return feedbackList.stream()
                .map(feedback -> buildDocument(feedback, categoryMap, tagMap))
                .collect(Collectors.toList());
    }

    private Map<Long, AssistantFeedbackCategory> buildCategoryMap(Set<Long> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return categoryService.lambdaQuery()
                .in(AssistantFeedbackCategory::getId, categoryIds)
                .list()
                .stream()
                .collect(Collectors.toMap(AssistantFeedbackCategory::getId, item -> item));
    }

    private AssistantFeedbackEsDocument buildDocument(AssistantFeedback feedback,
                                                      Map<Long, AssistantFeedbackCategory> categoryMap,
                                                      Map<Long, List<AssistantFeedbackTagVO>> tagMap) {
        AssistantFeedbackCategory category = categoryMap.get(feedback.getCategoryId());
        List<AssistantFeedbackTagVO> tags = tagMap.getOrDefault(feedback.getId(), Collections.emptyList());
        AssistantFeedbackEsDocument document = new AssistantFeedbackEsDocument();
        document.setId(feedback.getId());
        document.setUserId(feedback.getUserId());
        document.setCategoryId(feedback.getCategoryId());
        document.setCategoryCode(category == null ? null : category.getCode());
        document.setTicketNo(feedback.getTicketNo());
        document.setTitle(feedback.getTitle());
        document.setContent(feedback.getContent());
        document.setStatus(AssistantTicketStatus.normalize(feedback.getStatus()));
        document.setIsPinned(feedback.getIsPinned() == null ? 0 : feedback.getIsPinned());
        document.setPinOrder(feedback.getPinOrder() == null ? 0 : feedback.getPinOrder());
        document.setCommentCount(feedback.getCommentCount() == null ? 0 : feedback.getCommentCount());
        document.setViewCount(feedback.getViewCount() == null ? 0 : feedback.getViewCount());
        document.setLikeCount(feedback.getLikeCount() == null ? 0 : feedback.getLikeCount());
        document.setFavoriteCount(feedback.getFavoriteCount() == null ? 0 : feedback.getFavoriteCount());
        document.setHotScore(feedback.getHotScore() == null ? 0 : feedback.getHotScore());
        document.setTagIds(tags.stream().map(AssistantFeedbackTagVO::getId).collect(Collectors.toList()));
        document.setMineStatusPriority(AssistantTicketStatus.PENDING_CONFIRM.getCode().equals(document.getStatus()) ? 0 : 1);
        document.setCreateTime(feedback.getCreateTime());
        document.setUpdateTime(feedback.getUpdateTime());
        document.setDeleteFlag(feedback.getDeleteFlag() == null ? 0 : feedback.getDeleteFlag().intValue());
        return document;
    }

    private String normalizeQueryMode(String queryMode) {
        if (StrUtil.isBlank(queryMode)) {
            return "all";
        }
        return queryMode.trim().toLowerCase();
    }

    private void upsertFeedbackWithRebuildMirror(AssistantFeedbackEsDocument document) throws Exception {
        assistantFeedbackEsMapper.upsertFeedback(document);
        String rebuildTargetIndex = getRebuildTargetIndex();
        if (StrUtil.isNotBlank(rebuildTargetIndex)) {
            assistantFeedbackEsMapper.upsertFeedbackToIndex(document, rebuildTargetIndex);
        }
    }

    private void deleteFeedbackWithRebuildMirror(Long feedbackId) throws Exception {
        assistantFeedbackEsMapper.deleteFeedback(feedbackId);
        String rebuildTargetIndex = getRebuildTargetIndex();
        if (StrUtil.isNotBlank(rebuildTargetIndex)) {
            assistantFeedbackEsMapper.deleteFeedbackFromIndex(feedbackId, rebuildTargetIndex);
        }
    }

    private void markRebuildTargetIndex(String targetIndexName) {
        redisCache.setCacheObject(REBUILD_TARGET_INDEX_CACHE_KEY, targetIndexName,
                REBUILD_TARGET_INDEX_CACHE_MINUTES, TimeUnit.MINUTES);
    }

    private String getRebuildTargetIndex() {
        Object cacheValue = redisCache.getCacheObject(REBUILD_TARGET_INDEX_CACHE_KEY);
        return cacheValue == null ? null : String.valueOf(cacheValue);
    }

    private void clearRebuildTargetIndex() {
        redisCache.deleteObject(REBUILD_TARGET_INDEX_CACHE_KEY);
    }

    private void cleanupFailedRebuildTarget(String oldIndexName, String targetIndexName) {
        if (StrUtil.isBlank(targetIndexName) || targetIndexName.equals(oldIndexName)) {
            return;
        }
        try {
            assistantFeedbackEsMapper.deletePhysicalIndex(targetIndexName);
        } catch (Exception cleanupException) {
            log.warn("[feedback-es] cleanup failed rebuild target index failed, targetIndex={}", targetIndexName, cleanupException);
        }
    }
}
