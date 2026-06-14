package com.backstage.system.handler.seckill;

import com.backstage.common.constant.SeckillCacheConstants;
import com.backstage.system.domain.order.enums.ProductTypeEnum;
import com.backstage.system.domain.seckill.OshSeckillOrder;
import com.backstage.system.domain.user.OshUser;
import com.backstage.system.mapper.seckill.OshSeckillOrderMapper;
import com.backstage.system.mapper.user.OshUserMapper;
import com.backstage.system.service.announcement.ISeckillAnnouncementService;
import com.backstage.system.service.order.OrderPaidHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class SeckillPaidHandler implements OrderPaidHandler {

    private static final Logger logger = LoggerFactory.getLogger(SeckillPaidHandler.class);

    @Autowired
    private OshSeckillOrderMapper seckillOrderMapper;

    @Autowired
    private OshUserMapper userMapper;

    @Autowired
    private ISeckillAnnouncementService seckillAnnouncementService;

    @Autowired
    private RedisTemplate<Object, Object> redisTemplate;

    /**
     * 获取业务类型标识。
     *
     * @return 业务类型标识
     */
    @Override
    public String bizType() {
        return ProductTypeEnum.SECKILL.getName();
    }

    /**
     * 处理秒杀订单支付成功
     * 传入的 orderNo 是统一订单号，需通过 order_no 字段查秒杀单
     */
    @Override
    public void handle(String orderNo) {
        // 按统一订单号查秒杀单（不能用 seckillNo，支付回调传的是 orderNo）
        OshSeckillOrder order = seckillOrderMapper.selectOrderByOrderNo(orderNo);
        if (order == null) {
            logger.warn("【支付回调】秒杀订单不存在，orderNo={}", orderNo);
            return;
        }
        if (order.getStatus() == 1) {
            logger.info("【支付回调】订单已支付，跳过重复处理，orderNo={}", orderNo);
            return;
        }
        if (order.getStatus() != 0) {
            logger.warn("【支付回调】订单状态异常（非待支付），status={}, orderNo={}", order.getStatus(), orderNo);
            return;
        }

        int updated = seckillOrderMapper.updateOrderStatusWithCheck(
                order.getId(),
                0,
                1,
                new Date(),
                null,
                null
        );
        if (updated == 0) {
            logger.warn("【支付回调】订单状态更新失败，可能已被取消或超时，orderNo={}", orderNo);
            return;
        }

        // 支付成功后释放流程状态 Key，允许用户在限购数量内继续购买
        // orderKey 的职责是防并发重复提交，不应跨越订单生命周期
        try {
            String orderKey = SeckillCacheConstants.SECKILL_ORDER_KEY
                    + order.getActivityId() + ":" + order.getItemId() + ":" + order.getUserId();
            redisTemplate.delete(orderKey);
            logger.info("【支付回调】已释放秒杀流程状态 Key，orderNo={}", orderNo);
        } catch (Exception e) {
            logger.warn("【支付回调】释放秒杀流程状态 Key 失败，orderNo={}, error={}", orderNo, e.getMessage());
        }

        // 支付成功后写入秒杀动态到 osh_announcement
        try {
            String maskedUsername = buildMaskedUsername(order.getUserId());
            seckillAnnouncementService.insertSeckillDynamic(
                    maskedUsername, order.getGoodsTitle(), order.getGoodsId());
        } catch (Exception e) {
            logger.error("【支付回调】写入秒杀动态失败，orderNo={}, error={}", orderNo, e.getMessage());
        }

        logger.info("【支付回调】秒杀订单支付成功，seckillNo={}, orderNo={}", order.getSeckillNo(), orderNo);
    }

    /**
     * 根据 userId 查询用户，构建脱敏用户名
     * 当前实现使用登录名，取前2位 + **
     */
    private String buildMaskedUsername(Long userId) {
        if (userId == null) {
            return "某用户";
        }
        try {
            OshUser user = userMapper.selectUserById(userId);
            if (user == null) {
                return "某用户";
            }
            String name = user.getUsername();
            if (name == null || name.isEmpty()) {
                return "某用户";
            }
            int len = name.length();
            return len <= 2 ? name.charAt(0) + "**" : name.substring(0, 2) + "**";
        } catch (Exception e) {
            logger.warn("【支付回调】获取用户信息失败，userId={}", userId);
            return "某用户";
        }
    }
}
