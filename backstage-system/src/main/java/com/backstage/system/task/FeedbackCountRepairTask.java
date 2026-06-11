package com.backstage.system.task;

import com.backstage.system.domain.assistant.AssistantFeedback;
import com.backstage.system.domain.assistant.AssistantFeedbackComment;
import com.backstage.system.domain.assistant.AssistantFeedbackFavorite;
import com.backstage.system.domain.assistant.AssistantFeedbackLike;
import com.backstage.system.util.FeedbackHotScoreCalculator;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;

/**
 * 反馈冗余字段修复定时任务
 * 
 * TODO: 集成到统一的定时任务方案后启用
 * 
 * 功能：保证最终一致性
 * - 每天凌晨3点执行
 * - 对比明细表和冗余字段
 * - 以明细表为准修复数据
 * 
 * 使用方式：
 * 1. 添加 @Scheduled(cron = "0 0 3 * * ?") 注解
 * 2. 或集成到统一的定时任务管理系统
 *
 * @author backstage
 */
public class FeedbackCountRepairTask {

    private static final Logger log = LoggerFactory.getLogger(FeedbackCountRepairTask.class);

    /**
     * 修复反馈冗余字段（保证最终一致性）
     * 
     * TODO: 启用定时任务
     * 建议执行时间：每天凌晨3点
     * Cron 表达式：0 0 3 * * ?
     */
    // @Scheduled(cron = "0 0 3 * * ?")
    public void repairFeedbackCounts() {
        log.info("========== 开始修复反馈冗余字段 ==========");
        
        long startTime = System.currentTimeMillis();
        int totalCount = 0;
        int repairedCount = 0;
        
        // 分页查询（避免内存溢出）
        int pageSize = 100;
        int pageNum = 1;
        
        while (true) {
            List<AssistantFeedback> feedbackList = Db.lambdaQuery(AssistantFeedback.class)
                    .eq(AssistantFeedback::getDeleteFlag, 0)
                    .last("LIMIT " + ((pageNum - 1) * pageSize) + ", " + pageSize)
                    .list();
            
            if (feedbackList.isEmpty()) {
                break;
            }
            
            for (AssistantFeedback feedback : feedbackList) {
                totalCount++;
                
                // 统计真实数量（以明细表为准）
                Long actualLikeCount = Db.lambdaQuery(AssistantFeedbackLike.class)
                        .eq(AssistantFeedbackLike::getFeedbackId, feedback.getId())
                        .count();
                
                Long actualFavoriteCount = Db.lambdaQuery(AssistantFeedbackFavorite.class)
                        .eq(AssistantFeedbackFavorite::getFeedbackId, feedback.getId())
                        .count();
                
                Long actualCommentCount = Db.lambdaQuery(AssistantFeedbackComment.class)
                        .eq(AssistantFeedbackComment::getFeedbackId, feedback.getId())
                        .eq(AssistantFeedbackComment::getDeleteFlag, 0)
                        .count();
                
                // 计算正确的热度分
                int viewCount = feedback.getViewCount() == null ? 0 : feedback.getViewCount();
                int correctHotScore = FeedbackHotScoreCalculator.calculate(
                        actualLikeCount.intValue(),
                        actualFavoriteCount.intValue(),
                        actualCommentCount.intValue(),
                        viewCount
                );

                // 检查是否需要修复（互动数或热度分不一致）
                boolean needRepair =
                        !actualLikeCount.equals(feedback.getLikeCount().longValue()) ||
                        !actualFavoriteCount.equals(feedback.getFavoriteCount().longValue()) ||
                        !actualCommentCount.equals(feedback.getCommentCount().longValue()) ||
                        correctHotScore != (feedback.getHotScore() == null ? 0 : feedback.getHotScore());

                if (needRepair) {
                    // 修复数据
                    Db.lambdaUpdate(AssistantFeedback.class)
                            .eq(AssistantFeedback::getId, feedback.getId())
                            .set(AssistantFeedback::getLikeCount, actualLikeCount.intValue())
                            .set(AssistantFeedback::getFavoriteCount, actualFavoriteCount.intValue())
                            .set(AssistantFeedback::getCommentCount, actualCommentCount.intValue())
                            .set(AssistantFeedback::getHotScore, correctHotScore)
                            .update();

                    repairedCount++;

                    log.warn("修复反馈 {}: 点赞 {} → {}, 收藏 {} → {}, 评论 {} → {}, 热度 {} → {}",
                            feedback.getId(),
                            feedback.getLikeCount(), actualLikeCount,
                            feedback.getFavoriteCount(), actualFavoriteCount,
                            feedback.getCommentCount(), actualCommentCount,
                            feedback.getHotScore(), correctHotScore
                    );
                }
            }
            
            pageNum++;
        }
        
        long endTime = System.currentTimeMillis();
        log.info("========== 修复完成 ==========");
        log.info("总计检查: {} 条", totalCount);
        log.info("修复数量: {} 条", repairedCount);
        log.info("耗时: {} ms", endTime - startTime);
        
        // TODO: 集成监控告警
        // 如果修复数量过多，发送告警
        if (repairedCount > 100) {
            log.error("⚠️ 数据不一致数量过多: {} 条，请检查代码逻辑！", repairedCount);
            // 发送告警通知（钉钉、邮件等）
            // alertService.sendAlert("反馈系统数据不一致", "修复数量: " + repairedCount);
        }
    }
    
    /**
     * 数据一致性监控（可选）
     * 
     * TODO: 启用监控任务
     * 建议执行时间：每小时
     * Cron 表达式：0 0 * * * ?
     */
    // @Scheduled(cron = "0 0 * * * ?")
    public void monitorDataConsistency() {
        log.info("========== 开始数据一致性监控 ==========");
        
        // 随机抽查 100 条反馈
        List<AssistantFeedback> samples = Db.lambdaQuery(AssistantFeedback.class)
                .eq(AssistantFeedback::getDeleteFlag, 0)
                .orderByDesc(AssistantFeedback::getCreateTime)
                .last("LIMIT 100")
                .list();
        
        int inconsistentCount = 0;
        
        for (AssistantFeedback feedback : samples) {
            Long actualLikeCount = Db.lambdaQuery(AssistantFeedbackLike.class)
                    .eq(AssistantFeedbackLike::getFeedbackId, feedback.getId())
                    .count();
            
            if (!actualLikeCount.equals(feedback.getLikeCount().longValue())) {
                inconsistentCount++;
                log.error("发现数据不一致: 反馈ID={}, 冗余字段={}, 实际值={}", 
                        feedback.getId(), feedback.getLikeCount(), actualLikeCount);
            }
        }
        
        double inconsistencyRate = inconsistentCount / 100.0;
        log.info("数据不一致率: {}%", inconsistencyRate * 100);
        
        // TODO: 集成监控告警
        // 告警阈值：不一致率 > 5%
        if (inconsistencyRate > 0.05) {
            log.error("⚠️ 数据不一致率过高: {}%，请立即检查！", inconsistencyRate * 100);
            // 发送告警通知
            // alertService.sendAlert("反馈系统数据不一致率过高", "不一致率: " + (inconsistencyRate * 100) + "%");
        }
        
        log.info("========== 监控完成 ==========");
    }
}
