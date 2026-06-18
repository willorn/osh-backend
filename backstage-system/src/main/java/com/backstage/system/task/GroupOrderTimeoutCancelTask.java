package com.backstage.system.task;

import com.backstage.system.domain.servergroup.OshGroupOrder;
import com.backstage.system.domain.servergroup.OshGroupUserInitiated;
import com.backstage.system.mapper.servergroup.OshGroupServerMapper;
import com.backstage.system.mapper.servergroup.OshGroupUserInitiatedMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 拼团订单超时取消定时任务
 * 
 * 功能：
 * - 每5分钟执行一次
 * - 查询超时未支付的订单（默认15分钟）
 * - 取消订单并释放参团名额
 * 
 * 使用方式：
 * 1. 添加 @Scheduled(cron = "0 0/5 * * * ?") 注解
 * 2. 或集成到 Quartz 定时任务管理系统
 *
 * @author system
 * @date 2026-05-14
 */
@Component
public class GroupOrderTimeoutCancelTask {

    private static final Logger log = LoggerFactory.getLogger(GroupOrderTimeoutCancelTask.class);

    /** 订单超时时间（分钟） */
    private static final int TIMEOUT_MINUTES = 15;

    @Autowired
    private OshGroupServerMapper groupServerMapper;

    @Autowired
    private OshGroupUserInitiatedMapper userInitiatedMapper;

    /**
     * 取消超时未支付的拼团订单
     * 
     * TODO: 启用定时任务
     * 建议执行时间：每5分钟
     * Cron 表达式：0 0/5 * * * ?
     */
    // @Scheduled(cron = "0 0/5 * * * ?")
    public void cancelTimeoutOrders() {
        log.info("========== 开始取消超时拼团订单 ==========");
        
        long startTime = System.currentTimeMillis();
        int totalCount = 0;
        int cancelledCount = 0;
        int failedCount = 0;
        
        try {
            // 计算超时时间点
            LocalDateTime timeoutThreshold = LocalDateTime.now().minusMinutes(TIMEOUT_MINUTES);
            
            // 查询超时未支付的订单
            List<OshGroupOrder> timeoutOrders = groupServerMapper.selectTimeoutOrders(timeoutThreshold);
            
            if (timeoutOrders == null || timeoutOrders.isEmpty()) {
                log.info("没有超时订单需要处理");
                return;
            }
            
            totalCount = timeoutOrders.size();
            log.info("发现 {} 个超时订单", totalCount);
            
            for (OshGroupOrder order : timeoutOrders) {
                try {
                    cancelOrder(order);
                    cancelledCount++;
                } catch (Exception e) {
                    failedCount++;
                    log.error("取消订单失败，订单号: {}", order.getOrderNo(), e);
                }
            }
            
        } catch (Exception e) {
            log.error("取消超时订单任务执行失败", e);
        }
        
        long endTime = System.currentTimeMillis();
        log.info("========== 取消超时订单完成 ==========");
        log.info("总计检查: {} 个订单", totalCount);
        log.info("成功取消: {} 个", cancelledCount);
        log.info("失败数量: {} 个", failedCount);
        log.info("耗时: {} ms", endTime - startTime);
    }

    /**
     * 按订单号取消待支付拼团订单，并复用原有名额释放逻辑。
     *
     * @param orderNo 拼团订单号
     */
    public void cancelPendingOrderByOrderNo(String orderNo) {
        OshGroupOrder order = groupServerMapper.selectGroupOrderByOrderNo(orderNo);
        if (order == null) {
            log.warn("拼团订单不存在，跳过取消，订单号: {}", orderNo);
            return;
        }
        if (!"pending".equals(order.getStatus())) {
            log.info("拼团订单不是待支付状态，跳过取消，订单号: {}, status={}", orderNo, order.getStatus());
            return;
        }
        cancelOrder(order);
    }

    /**
     * 取消单个订单并释放名额
     */
    private void cancelOrder(OshGroupOrder order) {
        log.info("开始取消订单: {}, 用户ID: {}", order.getOrderNo(), order.getUserId());
        
        // 1. 更新订单状态为已取消
        int updateOrderResult = groupServerMapper.updateOrderStatus(
            order.getId(), 
            "cancelled",
            LocalDateTime.now()
        );
        
        if (updateOrderResult <= 0) {
            throw new RuntimeException("更新订单状态失败");
        }
        
        // 2. 更新参团记录状态为已取消
        if (order.getGroupWorkId() != null) {
            groupServerMapper.updateGroupWorkStatus(order.getGroupWorkId(), 2); // 2-已取消
        }
        
        // 3. 释放拼团活动人数
        if (order.getGroupActivityId() != null) {
            releaseActivityQuota(order.getGroupActivityId());
        }
        
        // 4. 释放用户发起拼团人数
        if (order.getGroupWorkId() != null) {
            releaseUserInitiatedQuota(order.getGroupWorkId());
        }
        
        log.info("订单取消成功: {}", order.getOrderNo());
    }

    /**
     * 释放系统活动拼团名额
     */
    private void releaseActivityQuota(Long activityId) {
        // 查询当前人数
        com.backstage.system.domain.servergroup.OshGroupActivity activity = groupServerMapper.selectGroupActivityById(activityId);
        if (activity == null) {
            log.warn("拼团活动不存在，ID: {}", activityId);
            return;
        }
        
        // 减少人数（至少为0）
        int newCurrentNum = Math.max(0, activity.getCurrentNum() - 1);
        
        int updateResult = groupServerMapper.incrementGroupActivityCurrentNum(
            activityId,
            newCurrentNum,
            activity.getStatus(),
            LocalDateTime.now()
        );
        
        if (updateResult > 0) {
            log.info("释放系统活动拼团名额成功，活动ID: {}, 当前人数: {}", activityId, newCurrentNum);
        }
    }

    /**
     * 释放用户发起拼团名额
     */
    private void releaseUserInitiatedQuota(Long initiatedId) {
        // 尝试查询用户发起记录
        OshGroupUserInitiated initiated = userInitiatedMapper.selectById(initiatedId);
        if (initiated == null) {
            // 可能不是用户发起的拼团，忽略
            return;
        }
        
        // 减少人数（至少为0）
        int newCurrentNum = Math.max(0, initiated.getCurrentNum() - 1);
        
        int updateResult = userInitiatedMapper.updateCurrentNum(initiatedId, newCurrentNum);
        
        if (updateResult > 0) {
            log.info("释放用户发起拼团名额成功，记录ID: {}, 当前人数: {}", initiatedId, newCurrentNum);
            
            // 如果人数低于最低成团人数，更新状态为招募中
            if (newCurrentNum < initiated.getMinNum() && initiated.getGroupStatus() == 1) {
                userInitiatedMapper.updateGroupStatus(initiatedId, 0); // 0-招募中
                log.info("拼团人数低于最低要求，状态更新为招募中，记录ID: {}", initiatedId);
            }
        }
    }
}
