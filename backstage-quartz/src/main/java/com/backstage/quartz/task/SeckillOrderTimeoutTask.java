package com.backstage.quartz.task;

import com.backstage.common.constant.SeckillCacheConstants;
import com.backstage.system.domain.seckill.OshSeckillActivityItem;
import com.backstage.system.domain.seckill.OshSeckillOrder;
import com.backstage.system.mapper.seckill.OshSeckillActivityItemMapper;
import com.backstage.system.mapper.seckill.OshSeckillOrderMapper;
import com.backstage.system.service.order.OrderService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * 秒杀订单支付超时自动取消定时任务
 * 每分钟扫描一次 status=0 且 pay_expire_time < now 的订单：
 *   1. 更新订单状态为已取消（status=2）
 *   2. 归还 Redis 库存（seckill:stock）
 *   3. 减少用户已购数量（seckill:bought_cnt），允许用户重新下单
 *   4. 删除流程状态 Key（seckill:order），解除重复提交拦截
 *
 * xxl-job handler 名称：seckill-order-timeout
 * Cron：0 * * * * ?（每分钟执行一次）
 */
@Component
public class SeckillOrderTimeoutTask {

    private static final Logger logger = LoggerFactory.getLogger(SeckillOrderTimeoutTask.class);

    private static final String SECKILL_STOCK_KEY      = SeckillCacheConstants.SECKILL_STOCK_KEY;
    private static final String SECKILL_BOUGHT_CNT_KEY = SeckillCacheConstants.SECKILL_BOUGHT_CNT_KEY;
    private static final String SECKILL_ORDER_KEY      = SeckillCacheConstants.SECKILL_ORDER_KEY;

    @Autowired
    private OshSeckillOrderMapper orderMapper;

    @Autowired
    private OshSeckillActivityItemMapper itemMapper;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private OrderService orderService;

    /**
     * Lua 脚本：归还 Redis 库存时不超过 totalStock 上限（原子操作）
     * KEYS[1] = stockKey
     * ARGV[1] = 归还数量 qty
     * ARGV[2] = totalStock 上限
     */
    private static final DefaultRedisScript<Long> RETURN_STOCK_SCRIPT = new DefaultRedisScript<>(
            "local cur = tonumber(redis.call('GET', KEYS[1])) or 0 " +
            "local total = tonumber(ARGV[2]) " +
            "local qty   = tonumber(ARGV[1]) " +
            "local ttl   = redis.call('PTTL', KEYS[1]) " +
            "local newVal = math.min(cur + qty, total) " +
            "if ttl ~= nil and ttl > 0 then " +
            "  redis.call('PSETEX', KEYS[1], ttl, tostring(newVal)) " +
            "else " +
            "  redis.call('SET', KEYS[1], tostring(newVal)) " +
            "end " +
            "return newVal",
            Long.class
    );

    /**
     * 每分钟执行一次，取消超时未支付的秒杀订单
     * xxl-job handler 名称：seckill-order-timeout
     */
    @XxlJob("seckill-order-timeout")
    public void cancelTimeoutOrders() {
        List<OshSeckillOrder> timeoutOrders = orderMapper.selectTimeoutOrders();
        if (timeoutOrders == null || timeoutOrders.isEmpty()) {
            XxlJobHelper.log("【超时取消】无超时订单，跳过");
            return;
        }

        logger.info("【超时取消】扫描到 {} 个超时未支付订单，开始处理", timeoutOrders.size());
        XxlJobHelper.log("【超时取消】扫描到 {} 个超时未支付订单，开始处理", timeoutOrders.size());

        int success = 0, fail = 0;
        for (OshSeckillOrder order : timeoutOrders) {
            try {
                cancelTimeoutOrder(order, true);
                success++;

            } catch (Exception e) {
                fail++;
                logger.error("【超时取消】处理订单异常，seckillNo={}", order.getSeckillNo(), e);
                XxlJobHelper.log("【超时取消】处理订单异常，seckillNo={}, 原因={}", order.getSeckillNo(), e.getMessage());
            }
        }

        logger.info("【超时取消】任务完成，成功={}，失败={}", success, fail);
        XxlJobHelper.log("【超时取消】任务完成，成功={}，失败={}", success, fail);
    }

    /**
     * 按统一订单号取消待支付秒杀订单。
     *
     * @param orderNo 统一订单号
     */
    public void cancelPendingOrderByOrderNo(String orderNo) {
        int updated = orderMapper.updateOrderStatusWithCheck(
                Long.valueOf(orderNo),
                0,
                3,
                null,
                new Date(),
                "pay_timeout"
        );
        if (updated == 0) {
            logger.info("【超时取消】订单状态已变更，跳过本次处理，seckillNo={}", order.getSeckillNo());
            return;
        }

        OshSeckillOrder order = orderMapper.selectOrderByOrderNo(orderNo);
        if (order == null) {
            logger.warn("【超时取消】秒杀订单不存在，跳过取消，orderNo={}", orderNo);
            return;
        }
        if (order.getStatus() == null || order.getStatus() != 0) {
            logger.info("【超时取消】秒杀订单不是待支付状态，跳过取消，orderNo={}, status={}", orderNo, order.getStatus());
            return;
        }
        cancelTimeoutOrder(order, false);
    }

    /**
     * 执行秒杀订单超时取消和库存回滚。
     *
     * @param order 秒杀订单
     * @param syncUnifiedOrder 是否同步取消统一订单
     */
    private void cancelTimeoutOrder(OshSeckillOrder order, boolean syncUnifiedOrder) {
        // 1. 更新订单状态为已超时（status=3，与用户主动取消 status=2 区分）
        OshSeckillOrder update = new OshSeckillOrder();
        update.setId(order.getId());
        update.setStatus(3);
        update.setCancelTime(new Date());
        update.setCancelReason("pay_timeout");
        orderMapper.updateOrder(update);

        // 2. 归还 Redis 库存（Lua 原子操作，归还后不超过 totalStock）
        String stockKey     = SECKILL_STOCK_KEY     + order.getActivityId() + ":" + order.getItemId();
        String boughtCntKey = SECKILL_BOUGHT_CNT_KEY + order.getActivityId() + ":" + order.getItemId() + ":" + order.getUserId();
        String orderKey     = SECKILL_ORDER_KEY     + order.getActivityId() + ":" + order.getItemId() + ":" + order.getUserId();

        int qty = order.getQuantity() != null ? order.getQuantity() : 1;

        // 2a. 同步归还数据库库存
        itemMapper.incrStock(order.getItemId(), qty);

        // 2b. 归还 Redis 库存，用 Lua 脚本原子归还，不超过 totalStock 上限
        OshSeckillActivityItem item = itemMapper.selectItemById(order.getItemId());
        Long stockAfter = null;
        if (item != null && Boolean.TRUE.equals(stringRedisTemplate.hasKey(stockKey))) {
            stockAfter = stringRedisTemplate.execute(
                    RETURN_STOCK_SCRIPT,
                    Arrays.asList(stockKey),
                    String.valueOf(qty),
                    String.valueOf(item.getTotalStock())
            );
        }

        // 3. 减少用户已购数量，允许用户重新下单
        Long boughtAfter = null;
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(boughtCntKey))) {
            boughtAfter = stringRedisTemplate.opsForValue().increment(boughtCntKey, -qty);
        }
        logger.info("【超时取消】回滚完成，seckillNo={}, qty={}, Redis库存归还后={}, bought_cnt归还后={}",
                order.getSeckillNo(), qty, stockAfter, boughtAfter);

        // 4. 删除流程状态 Key（超时后通常已自然过期，显式删除兜底）
        stringRedisTemplate.delete(orderKey);

        // 5. 同步取消统一订单（用 orderNo 调支付系统，不能用 seckillNo）
        if (syncUnifiedOrder) {
            try {
                orderService.cancelPaymentByOrderNo(order.getOrderNo());
            } catch (Exception ex) {
                logger.warn("【超时取消】取消统一订单失败，seckillNo={}, orderNo={}, error={}", order.getSeckillNo(), order.getOrderNo(), ex.getMessage());
            }
        }

        logger.info("【超时取消】订单已取消，seckillNo={}, orderNo={}, userId={}, activityId={}, itemId={}",
                order.getSeckillNo(), order.getOrderNo(), order.getUserId(), order.getActivityId(), order.getItemId());
    }
}
