package com.backstage.system.task;

import com.backstage.system.domain.seckill.OshSeckillActivity;
import com.backstage.system.domain.seckill.OshSeckillActivityItem;
import com.backstage.system.mapper.seckill.OshSeckillActivityItemMapper;
import com.backstage.system.mapper.seckill.OshSeckillActivityMapper;
import com.backstage.system.mapper.seckill.OshSeckillGoodsTagMapper;
import com.backstage.system.service.OutboxEventService;
import com.backstage.system.service.seckill.SeckillItemIndexDeleteMessage;
import com.backstage.system.service.seckill.SeckillItemIndexEventType;
import com.backstage.system.service.seckill.SeckillItemIndexUpsertMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 秒杀活动状态自动流转定时任务
 *
 * 每分钟执行一次：
 * - 未开始(1) → 进行中(2)：start_time <= now，触发 ES upsert（activityStatus=2，Flink 写入 ES）
 * - 进行中(2) → 已结束(3)：end_time <= now，触发 ES delete（从 ES 移除）
 */
@Component
public class SeckillActivityStatusTask {

    private static final Logger logger = LoggerFactory.getLogger(SeckillActivityStatusTask.class);

    @Autowired
    private OshSeckillActivityMapper activityMapper;

    @Autowired
    private OshSeckillActivityItemMapper itemMapper;

    @Autowired
    private OshSeckillGoodsTagMapper seckillGoodsTagMapper;

    @Autowired
    private OutboxEventService outboxEventService;

    /**
     * 每分钟执行一次，处理活动状态流转
     */
    @Scheduled(cron = "0 * * * * ?")
    public void autoTransitionStatus() {
        startActivities();
        endActivities();
    }

    /**
     * 未开始(1) → 进行中(2)
     * 批量更新数据库，然后对每个活动的明细发 UPSERT 事件
     * Flink 收到 activityStatus=2 的消息后写入 ES，用户可以搜索到
     */
    private void startActivities() {
        List<OshSeckillActivity> toStart = activityMapper.selectActivitiesToStart();
        if (toStart == null || toStart.isEmpty()) {
            return;
        }
        int updated = activityMapper.updateToOngoing();
        logger.info("【活动状态流转】未开始→进行中，更新数量={}", updated);

        for (OshSeckillActivity activity : toStart) {
            OshSeckillActivity latest = activityMapper.selectActivityById(activity.getId());
            if (latest == null) continue;
            List<OshSeckillActivityItem> items = itemMapper.selectItemsByActivityId(activity.getId());
            if (items == null || items.isEmpty()) continue;
            for (OshSeckillActivityItem item : items) {
                outboxEventService.saveSeckillItemIndexEvent(
                        item.getId(),
                        buildUpsertMessage(item, latest, SeckillItemIndexEventType.SECKILL_ITEM_INDEX_UPDATE),
                        "status-task");
            }
            logger.info("【活动状态流转】活动ID={} 已发送 ES upsert 事件，明细数={}",
                    activity.getId(), items.size());
        }
    }

    /**
     * 进行中(2) → 已结束(3)
     * 批量更新数据库，然后对每个活动的明细发 DELETE 事件
     */
    private void endActivities() {
        List<OshSeckillActivity> toEnd = activityMapper.selectActivitiesToEnd();
        if (toEnd == null || toEnd.isEmpty()) {
            return;
        }
        int updated = activityMapper.updateToFinished();
        logger.info("【活动状态流转】进行中→已结束，更新数量={}", updated);

        for (OshSeckillActivity activity : toEnd) {
            List<OshSeckillActivityItem> items = itemMapper.selectItemsByActivityId(activity.getId());
            if (items == null || items.isEmpty()) continue;
            for (OshSeckillActivityItem item : items) {
                outboxEventService.saveSeckillItemIndexDeleteEvent(
                        item.getId(),
                        new SeckillItemIndexDeleteMessage(item.getId()),
                        "status-task");
            }
            logger.info("【活动状态流转】活动ID={} 已发送 ES delete 事件，明细数={}",
                    activity.getId(), items.size());
        }
    }

    private SeckillItemIndexUpsertMessage buildUpsertMessage(
            OshSeckillActivityItem item, OshSeckillActivity activity, String eventType) {
        SeckillItemIndexUpsertMessage msg = new SeckillItemIndexUpsertMessage();
        msg.setEventType(eventType);
        msg.setId(item.getId());
        msg.setActivityId(activity.getId());
        msg.setActivityStatus(activity.getStatus());
        msg.setGoodsId(item.getGoodsId());
        msg.setGoodsType(item.getGoodsType());
        msg.setTitle(item.getTitle());
        msg.setCover(item.getCover());
        msg.setOriginPrice(item.getOriginPrice());
        msg.setSeckillPrice(item.getSeckillPrice());
        msg.setTotalStock(item.getTotalStock());
        msg.setAvailableStock(item.getAvailableStock());
        msg.setSoldCount(item.getSoldCount() != null ? item.getSoldCount() : 0);
        msg.setLimitPerUser(item.getLimitPerUser());
        msg.setSort(item.getSort());
        msg.setActivityTitle(activity.getTitle());
        msg.setPayTimeoutMin(activity.getPayTimeoutMin());
        msg.setStartTime(activity.getStartTime());
        msg.setEndTime(activity.getEndTime());
        msg.setDeleteFlag(item.getDeleteFlag() != null ? item.getDeleteFlag() : 0);
        msg.setCreateTime(item.getCreateTime());
        msg.setUpdateTime(item.getUpdateTime());
        // 写入标签
        if (item.getSeckillGoodsId() != null) {
            List<String> tagNames = seckillGoodsTagMapper.selectTagNamesBySeckillGoodsId(item.getSeckillGoodsId());
            msg.setTagNames(tagNames);
            msg.setTagNamesText(tagNames == null || tagNames.isEmpty() ? "" : String.join(" ", tagNames));
        }
        return msg;
    }
}
