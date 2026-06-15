package com.backstage.system.task;

import com.backstage.system.service.homepage.IOshHomePageAnnouncementPushService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 首页公告推送定时任务
 * <p>
 * 执行逻辑：
 * 1. 定期推送首页公告数据（每隔1小时执行一次）
 * 2. 每个resource_type分别推送 channel=1 的4条系统通知、channel=2 的4条业务动态
 * 3. 已登录用户在 WebSocket 建连后立即收到一次首页公告数据
 * 4. 定时任务每小时通过 WebSocket 广播刷新一次首页公告数据
 * 5. 所有操作记录详细日志便于溯源
 * 
 * 用户Token处理：
 * - 定时任务作为系统级操作，代表系统用户执行
 * - 推送消息需要标记为requireAuth=true，仅推送给已登录用户
 * - WebSocket服务需要验证客户端连接的用户token有效性
 * 
 * @author jayTatum
 */
@Component
@ConditionalOnProperty(name = "homepage.announcement.push.enabled", havingValue = "true", matchIfMissing = false)
public class HomepageAnnouncementPushTask {
    
    private static final Logger log = LoggerFactory.getLogger(HomepageAnnouncementPushTask.class);
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    
    @Autowired
    private IOshHomePageAnnouncementPushService homePageAnnouncementPushService;
    
    /**
     * 定期推送首页公告数据给所有在线用户
     * 每隔1小时执行一次
     * 
     * Cron表达式说明：0 0 * * * ? 
     *   - 0     : 秒
     *   - 0     : 分钟（每隔1小时的第0分钟）
     *   - *     : 小时（每个小时）
     *   - *     : 日期
     *   - *     : 月份
     *   - ?     : 星期
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void pushAllModulesAnnouncementsPeriodically() {
        // 生成可溯源的请求ID
        String requestId = generateRequestId();
        // 设置MDC用于日志追踪
        MDC.put("requestId", requestId);
        
        String startTime = LocalDateTime.now().format(TIMESTAMP_FORMATTER);
        String threadName = Thread.currentThread().getName();
        String threadId = String.valueOf(Thread.currentThread().getId());
        
        try {
            log.info("┌─────────────────────────────────────────────────────────────────┐");
            log.info("│ 【首页公告定时推送任务 - 开始执行】                               │");
            log.info("├─────────────────────────────────────────────────────────────────┤");
            log.info("│ 请求ID      : {} │", requestId);
            log.info("│ 执行线程    : {} (ID: {}) │", threadName, threadId);
            log.info("│ 开始时间    : {} │", startTime);
            log.info("│ 需要认证    : 是（requireAuth=true，仅推送给已登录用户）        │");
            log.info("│ 推送策略    : WebSocket优先 > HTTP降级（失败>5次触发）           │");
            log.info("└─────────────────────────────────────────────────────────────────┘");
            
            // 执行推送（内部包含WebSocket和降级逻辑）
            homePageAnnouncementPushService.pushAllModulesAnnouncementsData();
            
            String endTime = LocalDateTime.now().format(TIMESTAMP_FORMATTER);
            log.info("┌─────────────────────────────────────────────────────────────────┐");
            log.info("│ 【首页公告定时推送任务 - 执行成功】                               │");
            log.info("├─────────────────────────────────────────────────────────────────┤");
            log.info("│ 请求ID      : {} │", requestId);
            log.info("│ 结束时间    : {} │", endTime);
            log.info("│ 状态        : ✓ 成功                                              │");
            log.info("└─────────────────────────────────────────────────────────────────┘");
            
        } catch (Exception e) {
            String endTime = LocalDateTime.now().format(TIMESTAMP_FORMATTER);
            log.error("┌─────────────────────────────────────────────────────────────────┐");
            log.error("│ 【首页公告定时推送任务 - 执行失败】                               │");
            log.error("├─────────────────────────────────────────────────────────────────┤");
            log.error("│ 请求ID      : {} │", requestId);
            log.error("│ 结束时间    : {} │", endTime);
            log.error("│ 错误信息    : {} │", e.getMessage());
            log.error("│ 状态        : ✗ 失败                                              │");
            log.error("└─────────────────────────────────────────────────────────────────┘", e);
            
        } finally {
            // 清理MDC
            MDC.remove("requestId");
        }
    }
    
    /**
     * 生成可溯源的请求ID
     * 格式: HPAB-{timestamp}-{uuid}
     */
    private String generateRequestId() {
        long timestamp = System.currentTimeMillis();
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        return String.format("HPAB-%d-%s", timestamp, uuid);
    }
}
