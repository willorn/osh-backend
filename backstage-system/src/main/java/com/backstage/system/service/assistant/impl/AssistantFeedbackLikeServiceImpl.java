package com.backstage.system.service.assistant.impl;

import com.backstage.common.core.domain.AjaxResult;
import com.backstage.common.enums.AnnouncementModuleEnum;
import com.backstage.common.exception.ServiceException;
import com.backstage.system.domain.assistant.AssistantFeedback;
import com.backstage.system.domain.assistant.AssistantFeedbackLike;
import com.backstage.system.mapper.assistant.AssistantFeedbackLikeMapper;
import com.backstage.system.mapper.assistant.AssistantFeedbackMapper;
import com.backstage.system.service.announcement.AnnouncementRefreshBroadcaster;
import com.backstage.system.service.assistant.IAssistantFeedbackLikeService;
import com.backstage.system.util.FeedbackHotScoreCalculator;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 反馈点赞服务实现
 *
 * @author backstage
 */
@Service
public class AssistantFeedbackLikeServiceImpl implements IAssistantFeedbackLikeService {

    private static final Logger log = LoggerFactory.getLogger(AssistantFeedbackLikeServiceImpl.class);

    public AssistantFeedbackLikeServiceImpl(AssistantFeedbackMapper assistantFeedbackMapper,
                                            AssistantFeedbackLikeMapper assistantFeedbackLikeMapper,
                                            AnnouncementRefreshBroadcaster announcementRefreshBroadcaster) {
        this.assistantFeedbackMapper = assistantFeedbackMapper;
        this.assistantFeedbackLikeMapper = assistantFeedbackLikeMapper;
        this.announcementRefreshBroadcaster = announcementRefreshBroadcaster;
    }

    private final AssistantFeedbackMapper assistantFeedbackMapper;
    private final AssistantFeedbackLikeMapper assistantFeedbackLikeMapper;
    private final AnnouncementRefreshBroadcaster announcementRefreshBroadcaster;

    /**
     * 点赞（事务保证原子性）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AjaxResult like(Long feedbackId, Long userId) {
        // 1. 忽略重复插入，避免 MyBatis 包装唯一索引异常后进入全局 500。
        int inserted = assistantFeedbackLikeMapper.insertIgnore(feedbackId, userId, LocalDateTime.now());
        if (inserted == 0) {
            log.warn("用户 {} 重复点赞反馈 {}，已忽略重复请求", userId, feedbackId);
            return AjaxResult.error("您已经点赞过了");
        }

        // 2. 原子更新冗余字段（使用 SQL 自增，避免读取-修改-写入）
        String hotScoreSql = FeedbackHotScoreCalculator.buildHotScoreSql(1, 0, 0);
        boolean updated = updateFeedbackMetrics(
                feedbackId,
                FeedbackHotScoreCalculator.buildCountSql("like_count", 1),
                hotScoreSql
        );

        if (!updated) {
            throw new ServiceException("反馈不存在");
        }

        log.info("用户 {} 点赞反馈 {}", userId, feedbackId);
        announcementRefreshBroadcaster.broadcastRefresh(AnnouncementModuleEnum.FEEDBACK);
        return AjaxResult.success("点赞成功");
    }

    /**
     * 取消点赞
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AjaxResult unlike(Long feedbackId, Long userId) {
        // 1. 删除点赞记录
        boolean removed = Db.lambdaUpdate(AssistantFeedbackLike.class)
                .eq(AssistantFeedbackLike::getFeedbackId, feedbackId)
                .eq(AssistantFeedbackLike::getUserId, userId)
                .remove();

        if (!removed) {
            return AjaxResult.error("您还未点赞");
        }

        // 2. 原子更新冗余字段（使用 SQL 自减，防止负数）
        String hotScoreSql = FeedbackHotScoreCalculator.buildHotScoreSql(-1, 0, 0);
        updateFeedbackMetrics(
                feedbackId,
                FeedbackHotScoreCalculator.buildCountSql("like_count", -1),
                hotScoreSql
        );

        log.info("用户 {} 取消点赞反馈 {}", userId, feedbackId);
        announcementRefreshBroadcaster.broadcastRefresh(AnnouncementModuleEnum.FEEDBACK);
        return AjaxResult.success("已取消点赞");
    }

    /**
     * 查询用户是否已点赞
     */
    @Override
    public boolean isLiked(Long feedbackId, Long userId) {
        return Db.lambdaQuery(AssistantFeedbackLike.class)
                .eq(AssistantFeedbackLike::getFeedbackId, feedbackId)
                .eq(AssistantFeedbackLike::getUserId, userId)
                .exists();
    }

    /**
     * 使用 Wrapper + Mapper 更新，规避 MP 3.5.3.1 下 LambdaUpdateChainWrapper#setSql 的兼容性问题。
     *
     * @param feedbackId 反馈 ID
     * @param likeCountSql 点赞数更新表达式
     * @param hotScoreSql 热度分更新表达式
     * @return 是否更新成功
     */
    private boolean updateFeedbackMetrics(Long feedbackId, String likeCountSql, String hotScoreSql) {
        LambdaUpdateWrapper<AssistantFeedback> updateWrapper = Wrappers.lambdaUpdate(AssistantFeedback.class)
                .setSql("like_count = " + likeCountSql)
                .setSql("hot_score = " + hotScoreSql)
                .eq(AssistantFeedback::getDeleteFlag, (byte) 0)
                .eq(AssistantFeedback::getId, feedbackId);
        return assistantFeedbackMapper.update(new AssistantFeedback(), updateWrapper) > 0;
    }
}
