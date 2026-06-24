package com.backstage.system.service.assistant.impl;

import com.backstage.common.core.domain.AjaxResult;
import com.backstage.common.enums.AnnouncementModuleEnum;
import com.backstage.common.exception.ServiceException;
import com.backstage.system.domain.assistant.AssistantFeedback;
import com.backstage.system.domain.assistant.AssistantFeedbackFavorite;
import com.backstage.system.mapper.assistant.AssistantFeedbackFavoriteMapper;
import com.backstage.system.mapper.assistant.AssistantFeedbackMapper;
import com.backstage.system.service.announcement.AnnouncementRefreshBroadcaster;
import com.backstage.system.service.assistant.IAssistantFeedbackFavoriteService;
import com.backstage.system.util.FeedbackHotScoreCalculator;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 反馈收藏服务实现
 *
 * @author backstage
 */
@Service
public class AssistantFeedbackFavoriteServiceImpl implements IAssistantFeedbackFavoriteService {

    private static final Logger log = LoggerFactory.getLogger(AssistantFeedbackFavoriteServiceImpl.class);

    public AssistantFeedbackFavoriteServiceImpl(AssistantFeedbackMapper assistantFeedbackMapper,
                                                AssistantFeedbackFavoriteMapper assistantFeedbackFavoriteMapper,
                                                AnnouncementRefreshBroadcaster announcementRefreshBroadcaster) {
        this.assistantFeedbackMapper = assistantFeedbackMapper;
        this.assistantFeedbackFavoriteMapper = assistantFeedbackFavoriteMapper;
        this.announcementRefreshBroadcaster = announcementRefreshBroadcaster;
    }

    private final AssistantFeedbackMapper assistantFeedbackMapper;
    private final AssistantFeedbackFavoriteMapper assistantFeedbackFavoriteMapper;
    private final AnnouncementRefreshBroadcaster announcementRefreshBroadcaster;

    /**
     * 收藏（事务保证原子性）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AjaxResult favorite(Long feedbackId, Long userId) {
        // 1. 忽略重复插入，避免 MyBatis 包装唯一索引异常后进入全局 500。
        int inserted = assistantFeedbackFavoriteMapper.insertIgnore(feedbackId, userId, LocalDateTime.now());
        if (inserted == 0) {
            log.warn("用户 {} 重复收藏反馈 {}，已忽略重复请求", userId, feedbackId);
            return AjaxResult.error("您已经收藏过了");
        }

        // 2. 原子更新冗余字段（使用 SQL 自增，避免读取-修改-写入）
        String hotScoreSql = FeedbackHotScoreCalculator.buildHotScoreSql(0, 1, 0);
        boolean updated = updateFeedbackMetrics(
                feedbackId,
                FeedbackHotScoreCalculator.buildCountSql("favorite_count", 1),
                hotScoreSql
        );

        if (!updated) {
            throw new ServiceException("反馈不存在");
        }

        log.info("用户 {} 收藏反馈 {}", userId, feedbackId);
        announcementRefreshBroadcaster.broadcastRefresh(AnnouncementModuleEnum.FEEDBACK);
        return AjaxResult.success("收藏成功");
    }

    /**
     * 取消收藏
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AjaxResult unfavorite(Long feedbackId, Long userId) {
        // 1. 删除收藏记录
        boolean removed = Db.lambdaUpdate(AssistantFeedbackFavorite.class)
                .eq(AssistantFeedbackFavorite::getFeedbackId, feedbackId)
                .eq(AssistantFeedbackFavorite::getUserId, userId)
                .remove();

        if (!removed) {
            return AjaxResult.error("您还未收藏");
        }

        // 2. 原子更新冗余字段（使用 SQL 自减，防止负数）
        String hotScoreSql = FeedbackHotScoreCalculator.buildHotScoreSql(0, -1, 0);
        updateFeedbackMetrics(
                feedbackId,
                FeedbackHotScoreCalculator.buildCountSql("favorite_count", -1),
                hotScoreSql
        );

        log.info("用户 {} 取消收藏反馈 {}", userId, feedbackId);
        announcementRefreshBroadcaster.broadcastRefresh(AnnouncementModuleEnum.FEEDBACK);
        return AjaxResult.success("已取消收藏");
    }

    /**
     * 查询用户是否已收藏
     */
    @Override
    public boolean isFavorited(Long feedbackId, Long userId) {
        return Db.lambdaQuery(AssistantFeedbackFavorite.class)
                .eq(AssistantFeedbackFavorite::getFeedbackId, feedbackId)
                .eq(AssistantFeedbackFavorite::getUserId, userId)
                .exists();
    }

    @Override
    public Set<Long> listFavoriteFeedbackIds(Long userId) {
        if (userId == null) {
            return Collections.emptySet();
        }
        return Db.lambdaQuery(AssistantFeedbackFavorite.class)
                .eq(AssistantFeedbackFavorite::getUserId, userId)
                .list()
                .stream()
                .map(AssistantFeedbackFavorite::getFeedbackId)
                .collect(Collectors.toSet());
    }

    /**
     * 使用 Wrapper + Mapper 更新，规避 MP 3.5.3.1 下 LambdaUpdateChainWrapper#setSql 的兼容性问题。
     *
     * @param feedbackId 反馈 ID
     * @param favoriteCountSql 收藏数更新表达式
     * @param hotScoreSql 热度分更新表达式
     * @return 是否更新成功
     */
    private boolean updateFeedbackMetrics(Long feedbackId, String favoriteCountSql, String hotScoreSql) {
        LambdaUpdateWrapper<AssistantFeedback> updateWrapper = Wrappers.lambdaUpdate(AssistantFeedback.class)
                .setSql("favorite_count = " + favoriteCountSql)
                .setSql("hot_score = " + hotScoreSql)
                .eq(AssistantFeedback::getDeleteFlag, (byte) 0)
                .eq(AssistantFeedback::getId, feedbackId);
        return assistantFeedbackMapper.update(new AssistantFeedback(), updateWrapper) > 0;
    }
}
