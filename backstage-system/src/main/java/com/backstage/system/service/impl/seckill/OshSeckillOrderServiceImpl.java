package com.backstage.system.service.impl.seckill;

import com.alibaba.fastjson2.JSON;
import com.backstage.common.constant.KafkaConstants;
import com.backstage.common.constant.SeckillCacheConstants;
import com.backstage.common.exception.ServiceException;
import com.backstage.common.utils.generate.GenerateUtil;
import com.backstage.common.utils.ip.IpUtils;
import com.backstage.common.utils.kafka.KafkaMessageUtil;
import com.backstage.system.domain.order.OshPayment;
import com.backstage.system.domain.seckill.OshSeckillActivity;
import com.backstage.system.domain.seckill.OshSeckillActivityItem;
import com.backstage.system.domain.seckill.OshSeckillOrder;
import com.backstage.system.domain.vo.seckill.SeckillOrderAdminVO;
import com.backstage.system.domain.vo.seckill.SeckillRecentOrderVO;
import com.backstage.system.domain.vo.seckill.SeckillResultVO;
import com.backstage.system.mapper.seckill.OshSeckillActivityItemMapper;
import com.backstage.system.mapper.seckill.OshSeckillActivityMapper;
import com.backstage.system.mapper.seckill.OshSeckillOrderMapper;
import com.backstage.system.mapper.order.OshPaymentMapper;
import com.backstage.system.service.order.OrderService;
import com.backstage.system.service.seckill.IOshSeckillOrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import java.math.BigDecimal;
import java.util.Date;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 秒杀订单 Service 实现
 *
 * @author backstage
 * @date 2026-04-28
 */
@Service
public class OshSeckillOrderServiceImpl implements IOshSeckillOrderService {

    private static final Logger logger = LoggerFactory.getLogger(OshSeckillOrderServiceImpl.class);

    /**
     * Redis Key 前缀（统一引用 SeckillCacheConstants，避免多处定义不一致）
     *   seckill:stock:{activityId}:{itemId}              库存计数器
     *   seckill:bought_cnt:{activityId}:{itemId}:{userId} 用户当前占用的限购额度
     *   seckill:activity:{activityId}                    活动基本信息缓存
     *   seckill:item:{itemId}                            明细信息缓存
     *   seckill:order:{activityId}:{itemId}:{userId}     当前未完成秒杀尝试号（value=seckillNo）
     */
    private static final String SECKILL_STOCK_KEY      = SeckillCacheConstants.SECKILL_STOCK_KEY;
    private static final String SECKILL_BOUGHT_CNT_KEY = SeckillCacheConstants.SECKILL_BOUGHT_CNT_KEY;
    private static final String SECKILL_ACTIVITY_KEY   = SeckillCacheConstants.SECKILL_ACTIVITY_KEY;
    private static final String SECKILL_ITEM_KEY       = SeckillCacheConstants.SECKILL_ITEM_KEY;
    private static final String SECKILL_ORDER_KEY      = SeckillCacheConstants.SECKILL_ORDER_KEY;
    private static final String SECKILL_SUBMIT_LOCK_KEY = SeckillCacheConstants.SECKILL_SUBMIT_LOCK_KEY;

    /** 秒杀尝试号使用雪花算法生成，见 generateSeckillNo() */

    /**
     * Lua 脚本：原子扣减库存 + 限购校验 + 记录已购数量
     * KEYS[1] = 库存 key（seckill:stock:{activityId}:{itemId}）
     * KEYS[2] = 用户已购数量 key（seckill:bought_cnt:{activityId}:{itemId}:{userId}）
     * ARGV[1] = 本次购买数量（quantity）
     * ARGV[2] = 每人限购上限（limitPerUser）
     * ARGV[3] = 已购数量 Key 过期时间（秒）
     * 返回值：
     *   1  = 秒杀成功
     *  -1  = 库存不足
     *  -2  = 超过限购数量
     */
    private static final String SECKILL_LUA_SCRIPT =
            "local stock = tonumber(redis.call('get', KEYS[1])) \n" +
            "local qty   = tonumber(ARGV[1]) \n" +
            "local limit = tonumber(ARGV[2]) \n" +
            "if stock == nil or stock < qty then \n" +
            "    return -1 \n" +
            "end \n" +
            "local bought = tonumber(redis.call('get', KEYS[2])) or 0 \n" +
            "if bought + qty > limit then \n" +
            "    return -2 \n" +
            "end \n" +
            "redis.call('decrby', KEYS[1], qty) \n" +
            "local newBought = redis.call('incrby', KEYS[2], qty) \n" +
            "if newBought == qty then \n" +
            "    redis.call('expire', KEYS[2], tonumber(ARGV[3])) \n" +
            "end \n" +
            "return 1";

    @Autowired
    private RedisTemplate<Object, Object> redisTemplate;

    /** 专门用于库存 Key 的读写，保证存的是纯数字字符串，Lua 脚本 tonumber() 才能正确解析 */
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private OshSeckillActivityMapper activityMapper;

    @Autowired
    private OshSeckillActivityItemMapper itemMapper;

    @Autowired
    private OshSeckillOrderMapper orderMapper;

    @Autowired
    private OrderService orderService;

    @Autowired
    private OshPaymentMapper paymentMapper;

    /**
     * 接口7：管理端查询秒杀订单列表
     */
    @Override
    public List<SeckillOrderAdminVO> selectOrderList(OshSeckillOrder order) {
        return orderMapper.selectOrderList(order)
                .stream()
                .map(this::toAdminVO)
                .collect(Collectors.toList());
    }

    /**
     * 根据秒杀单号查询订单
     */
    @Override
    public OshSeckillOrder getOrderBySeckillNo(String seckillNo, Long userId) {
        return orderMapper.selectOrderBySeckillNo(seckillNo);
    }

    /**
     * 通过秒杀单号查询订单状态（支付完成后前端轮询用）
     * 校验订单归属，防止越权查询
     */
    @Override
    public SeckillResultVO getOrderStatusBySeckillNo(String seckillNo, Long userId) {
        OshSeckillOrder order = orderMapper.selectOrderBySeckillNo(seckillNo);
        if (order == null || !order.getUserId().equals(userId)) {
            return null;
        }
        return toResultVO(order);
    }

    /**
     * 接口10：执行秒杀
     * 用户需指定 activityId + itemId，表示抢这场活动里的哪个商品
     */
    @Override
    public SeckillResultVO doSeckill(Long activityId, Long itemId, Long userId, int quantity) {

        // 1. 校验活动状态
        OshSeckillActivity activity = getActivityFromCache(activityId);
        if (activity == null) {
            throw new ServiceException("活动不存在");
        }
        // 已下架/已删除直接拦截
        if (activity.getStatus() == 4) {
            throw new ServiceException("活动已下架");
        }
        // 以时间窗口作为"是否进行中"的唯一标准，不依赖定时任务更新的 status 字段
        Date now = new Date();
        if (activity.getStartTime() == null || now.before(activity.getStartTime())) {
            throw new ServiceException("活动尚未开始");
        }
        if (activity.getEndTime() == null || now.after(activity.getEndTime())) {
            throw new ServiceException("活动已结束");
        }

        // 2. 校验明细（商品）是否属于该活动
        OshSeckillActivityItem item = getItemFromCache(itemId);
        if (item == null || !item.getActivityId().equals(activityId)) {
            throw new ServiceException("商品不存在或不属于该活动");
        }

        // 3. 校验购买数量
        int limitPerUser = item.getLimitPerUser() != null ? item.getLimitPerUser() : 1;
        if (quantity < 1) {
            throw new ServiceException("购买数量不能小于1");
        }
        if (quantity > limitPerUser) {
            throw new ServiceException("超过限购数量，每人最多购买" + limitPerUser + "件");
        }

        // 4. 执行 Lua 脚本（原子扣减库存 + 限购校验 + 累加已购数量）
        String stockKey    = SECKILL_STOCK_KEY     + activityId + ":" + itemId;
        String boughtCntKey = SECKILL_BOUGHT_CNT_KEY + activityId + ":" + itemId + ":" + userId;
        String orderKey    = SECKILL_ORDER_KEY     + activityId + ":" + itemId + ":" + userId;
        String submitLockKey = SECKILL_SUBMIT_LOCK_KEY + activityId + ":" + itemId + ":" + userId;

        Boolean locked = Boolean.TRUE.equals(stringRedisTemplate.opsForValue().setIfAbsent(
                submitLockKey,
                "1",
                SeckillCacheConstants.SECKILL_SUBMIT_LOCK_EXPIRE_SECONDS,
                TimeUnit.SECONDS
        ));
        if (!locked) {
            throw new ServiceException("请求过于频繁，请勿重复点击");
        }
        try {
            // 4.1 先检查流程状态 Key，防止用户重复点击
            Object existingNo = redisTemplate.opsForValue().get(orderKey);
            if (existingNo != null) {
                throw new ServiceException("您已在秒杀流程中，请勿重复提交，单号：" + existingNo);
            }

            DefaultRedisScript<Long> script = new DefaultRedisScript<>();
            script.setScriptText(SECKILL_LUA_SCRIPT);
            script.setResultType(Long.class);

            Long result = stringRedisTemplate.execute(
                    script,
                    Arrays.asList(stockKey, boughtCntKey),
                    String.valueOf(quantity),
                    String.valueOf(limitPerUser),
                    String.valueOf(SeckillCacheConstants.calcExpireSeconds(activity.getEndTime(), SeckillCacheConstants.BOUGHT_CNT_EXPIRE_BUFFER))
            );

            if (result == null || result == -1L) {
                throw new ServiceException("手慢了，库存不足");
            }
            if (result == -2L) {
                throw new ServiceException("超过限购数量，每人最多购买" + limitPerUser + "件");
            }

            // 5. Lua 成功后生成秒杀尝试号（seckillNo），代表"成功抢到资格的一次有效秒杀尝试"
            String seckillNo = generateSeckillNo();

            // 6. 写入未完成订单锁（orderKey），value 存 seckillNo
            // 初始 TTL 设为 2 分钟，给 Kafka 消费者足够的建单处理窗口
            // 消费者建单成功后会延长 TTL 到真实支付超时时间；消费者失败时会主动删除
            long expireSeconds = (activity.getPayTimeoutMin() != null ? activity.getPayTimeoutMin() : 15) * 60L;
            redisTemplate.opsForValue().set(orderKey, seckillNo, 2, TimeUnit.MINUTES);

            // 7. 发送 Kafka 消息，由消费者异步完成：checkout() + decrStock() + insertOrder()
            SeckillOrderMessage message = new SeckillOrderMessage();
            message.setSeckillNo(seckillNo);
            message.setActivityId(activityId);
            message.setItemId(itemId);
            message.setUserId(userId);
            message.setGoodsId(item.getGoodsId());
            message.setGoodsType(item.getGoodsType());
            message.setGoodsTitle(item.getTitle());
            message.setGoodsCover(item.getCover());
            message.setOriginPrice(item.getOriginPrice());
            message.setSeckillPrice(item.getSeckillPrice());
            message.setQuantity(quantity);
            message.setPayExpireTime(calcPayExpireTime(activity.getPayTimeoutMin()));
            message.setStockKey(stockKey);
            message.setBoughtCntKey(boughtCntKey);
            message.setOrderKey(orderKey);
            message.setExpireSeconds(expireSeconds);
            try {
                message.setClientIp(IpUtils.getIpAddr());
            } catch (Exception e) {
                message.setClientIp("unknown");
            }
            try {
                KafkaMessageUtil.sendMessageSync(
                        KafkaConstants.SECKILL_ORDER_CREATE_TOPIC,
                        activityId + ":" + itemId + ":" + userId,
                        JSON.toJSONString(message)
                );
                logger.info("【秒杀】发送订单创建消息成功，userId={}, activityId={}, itemId={}, seckillNo={}", userId, activityId, itemId, seckillNo);
            } catch (Exception e) {
                // Kafka 发送失败，回滚 Redis
                stringRedisTemplate.opsForValue().increment(stockKey, quantity);
                stringRedisTemplate.opsForValue().increment(boughtCntKey, -quantity);
                redisTemplate.delete(orderKey);
                logger.error("【秒杀】Kafka 消息发送失败，回滚 Redis，userId={}, activityId={}, itemId={}", userId, activityId, itemId, e);
                throw new ServiceException("秒杀失败，请重试");
            }

            // 8. 立即返回 seckillNo，前端通过轮询 getSeckillResult() 获取真实订单信息
            SeckillResultVO vo = new SeckillResultVO();
            vo.setSeckillNo(seckillNo);
            vo.setStatus(-1); // 处理中，前端继续轮询
            vo.setNeedPay(true);
            vo.setGoodsId(item.getGoodsId());
            vo.setGoodsType(item.getGoodsType());
            vo.setGoodsTitle(item.getTitle());
            vo.setGoodsCover(item.getCover());
            vo.setOriginPrice(item.getOriginPrice());
            vo.setSeckillPrice(item.getSeckillPrice());
            vo.setTotalAmount(item.getSeckillPrice().multiply(BigDecimal.valueOf(quantity)));
            vo.setQuantity(quantity);
            vo.setPayExpireTime(calcPayExpireTime(activity.getPayTimeoutMin()));
            return vo;
        } finally {
            stringRedisTemplate.delete(submitLockKey);
        }
    }

    /**
     * 接口11：查询秒杀结果（前端轮询）
     *
     * 状态机：
     *   1. orderKey 不存在 + DB 无未完成订单 → null（未参与或已完结）
     *   2. orderKey 存在，DB 里还没有对应 seckillNo → status=-1（建单中）
     *   3. DB 里有对应 seckillNo 的订单 → 按订单状态返回（0/1/2/3）
     *      status=0 时额外查支付信息（qrcode/payUrl）
     */
    @Override
    public SeckillResultVO getSeckillResult(Long activityId, Long itemId, Long userId) {
        String orderKey = SECKILL_ORDER_KEY + activityId + ":" + itemId + ":" + userId;
        Object cachedVal = redisTemplate.opsForValue().get(orderKey);

        if (cachedVal == null) {
            // orderKey 不存在：Redis Key 过期或从未参与
            // 兜底查 DB，防止：
            // 1. Key 过期但待支付订单实际存在
            // 2. 支付成功后 orderKey 已主动释放，前端继续轮询却误判为“无结果”
            OshSeckillOrder dbOrder = orderMapper.selectPendingOrderByItemUser(itemId, userId);
            if (dbOrder != null) {
                return buildResultVOWithPayInfo(dbOrder);
            }
            OshSeckillOrder latestEffectiveOrder = orderMapper.selectLatestEffectiveOrderByItemUser(itemId, userId);
            if (latestEffectiveOrder != null) {
                return buildResultVOWithPayInfo(latestEffectiveOrder);
            }
            return null;
        }

        // orderKey 存在，value 就是 seckillNo
        String seckillNo = cachedVal.toString();
        OshSeckillOrder order = orderMapper.selectOrderBySeckillNo(seckillNo);
        if (order == null) {
            // Kafka 消费还未完成，订单尚未落库
            SeckillResultVO pending = new SeckillResultVO();
            pending.setSeckillNo(seckillNo);
            pending.setStatus(-1);
            return pending;
        }

        return buildResultVOWithPayInfo(order);
    }

    /**
     * 构建结果 VO，status=0 时额外填充支付信息
     */
    private SeckillResultVO buildResultVOWithPayInfo(OshSeckillOrder order) {
        SeckillResultVO vo = toResultVO(order);
        vo.setNeedPay(order.getTotalAmount() != null && order.getTotalAmount().compareTo(BigDecimal.ZERO) > 0);
        if (order.getStatus() == 0 && order.getOrderNo() != null) {
            try {
                OshPayment payment = paymentMapper.selectByOrderNo(order.getOrderNo());
                if (payment != null) {
                    vo.setQrcode(payment.getQrcode());
                    vo.setPayUrl(payment.getPayUrl());
                }
            } catch (Exception e) {
                logger.warn("【秒杀结果查询】获取支付信息失败，orderNo={}, error={}", order.getOrderNo(), e.getMessage());
            }
        }
        return vo;
    }

    /**
     * 接口12：取消秒杀订单
     */
    @Override
    public void cancelOrder(String seckillNo, Long userId) {
        OshSeckillOrder order = orderMapper.selectOrderBySeckillNo(seckillNo);
        if (order == null) {
            throw new ServiceException("订单不存在");
        }
        if (!order.getUserId().equals(userId)) {
            throw new ServiceException("无权操作此订单");
        }
        if (order.getStatus() != 0) {
            throw new ServiceException("只有待支付的订单才能取消");
        }

        int updated = orderMapper.updateOrderStatusWithCheck(
                order.getId(),
                0,
                2,
                null,
                new Date(),
                "user_cancel"
        );
        if (updated == 0) {
            throw new ServiceException("订单状态已变更，请刷新后重试");
        }

        // 同步取消统一订单（用 orderNo 调支付系统，不能用 seckillNo）
        try {
            orderService.cancelPaymentByOrderNo(order.getOrderNo());
        } catch (Exception e) {
            logger.warn("【秒杀】取消统一订单失败，seckillNo={}, orderNo={}, error={}", seckillNo, order.getOrderNo(), e.getMessage());
        }

        // 回滚 Redis 库存、已购数量、流程状态 Key
        String stockKey     = SECKILL_STOCK_KEY     + order.getActivityId() + ":" + order.getItemId();
        String boughtCntKey = SECKILL_BOUGHT_CNT_KEY + order.getActivityId() + ":" + order.getItemId() + ":" + userId;
        String orderKey     = SECKILL_ORDER_KEY     + order.getActivityId() + ":" + order.getItemId() + ":" + userId;
        int qty = order.getQuantity() != null ? order.getQuantity() : 1;
        itemMapper.incrStock(order.getItemId(), qty);
        stringRedisTemplate.opsForValue().increment(stockKey, qty);
        Long boughtAfter = stringRedisTemplate.opsForValue().increment(boughtCntKey, -qty);
        redisTemplate.delete(orderKey);

        logger.info("【秒杀】用户取消订单，seckillNo={}, orderNo={}, userId={}, activityId={}, itemId={}, qty={}, bought_cnt归还后={}",
                seckillNo, order.getOrderNo(), userId, order.getActivityId(), order.getItemId(), qty, boughtAfter);
    }

    @Override
    public List<SeckillRecentOrderVO> getRecentPaidOrders(int limit) {
        // 最大限制50条，防止滥用
        int safeLimit = (limit <= 0 || limit > 50) ? 10 : limit;
        return orderMapper.selectRecentPaidOrders(safeLimit);
    }

    // ==================== 私有方法 ====================

    /**
     * 从 Redis 缓存获取活动信息，没有则查 DB 并写入缓存
     */
    private OshSeckillActivity getActivityFromCache(Long activityId) {
        String cacheKey = SECKILL_ACTIVITY_KEY + activityId;
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return JSON.parseObject(JSON.toJSONString(cached), OshSeckillActivity.class);
        }
        OshSeckillActivity activity = activityMapper.selectActivityById(activityId);
        if (activity != null) {
            long expire = SeckillCacheConstants.calcExpireSeconds(activity.getEndTime(), SeckillCacheConstants.ACTIVITY_EXPIRE_BUFFER);
            redisTemplate.opsForValue().set(cacheKey, activity, expire, TimeUnit.SECONDS);
        }
        return activity;
    }

    /**
     * 从 Redis 缓存获取明细信息，没有则查 DB 并写入缓存
     * 同时初始化该明细的库存 Key（如果还没有）
     *
     * 注意：库存 Key 过期重建时使用数据库的 available_stock（实时剩余库存），
     * 而非 total_stock（原始总库存），避免库存"复活"导致超卖。
     */
    private OshSeckillActivityItem getItemFromCache(Long itemId) {
        String cacheKey = SECKILL_ITEM_KEY + itemId;
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        OshSeckillActivityItem item;
        if (cached != null) {
            item = JSON.parseObject(JSON.toJSONString(cached), OshSeckillActivityItem.class);
        } else {
            item = itemMapper.selectItemById(itemId);
            if (item != null) {
                // 需要活动结束时间来计算过期时间，从活动缓存里取
                OshSeckillActivity activity = getActivityFromCache(item.getActivityId());
                Date endTime = activity != null ? activity.getEndTime() : null;
                long expire = SeckillCacheConstants.calcExpireSeconds(endTime, SeckillCacheConstants.ACTIVITY_EXPIRE_BUFFER);
                redisTemplate.opsForValue().set(cacheKey, item, expire, TimeUnit.SECONDS);
            }
        }
        // 库存 Key 不存在时重建（首次初始化 或 Key 过期后懒加载重建）
        if (item != null) {
            String stockKey = SECKILL_STOCK_KEY + item.getActivityId() + ":" + itemId;
            if (!Boolean.TRUE.equals(stringRedisTemplate.hasKey(stockKey))) {
                OshSeckillActivityItem freshItem = itemMapper.selectItemById(itemId);
                if (freshItem != null) {
                    OshSeckillActivity activity = getActivityFromCache(item.getActivityId());
                    Date endTime = activity != null ? activity.getEndTime() : null;
                    long stockExpire = SeckillCacheConstants.calcExpireSeconds(endTime, SeckillCacheConstants.STOCK_EXPIRE_BUFFER);
                    stringRedisTemplate.opsForValue().set(stockKey,
                            String.valueOf(freshItem.getAvailableStock()),
                            stockExpire, TimeUnit.SECONDS);
                    logger.info("【库存重建】itemId={}, 实时剩余库存={}, 过期时间={}s", itemId, freshItem.getAvailableStock(), stockExpire);
                }
            }
        }
        return item;
    }

    /**
     * 计算支付截止时间
     */
    private Date calcPayExpireTime(Integer payTimeoutMin) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MINUTE, payTimeoutMin != null ? payTimeoutMin : 15);
        return cal.getTime();
    }

    /**
     * 生成秒杀尝试号
     * 使用项目统一的雪花算法（MyBatis-Plus DefaultIdentifierGenerator），多实例下唯一性有保障
     * 格式：SK + 雪花ID（18位数字）
     */
    private String generateSeckillNo() {
        return "SK" + GenerateUtil.generateSnowflakeId();
    }

    /** 订单实体转管理端 VO */
    private SeckillOrderAdminVO toAdminVO(OshSeckillOrder order) {
        SeckillOrderAdminVO vo = new SeckillOrderAdminVO();
        vo.setId(order.getId());
        vo.setSeckillNo(order.getSeckillNo());
        vo.setOrderNo(order.getOrderNo());
        vo.setActivityId(order.getActivityId());
        vo.setUserId(order.getUserId());
        vo.setGoodsId(order.getGoodsId());
        vo.setGoodsType(order.getGoodsType());
        vo.setGoodsTitle(order.getGoodsTitle());
        vo.setOriginPrice(order.getOriginPrice());
        vo.setSeckillPrice(order.getSeckillPrice());
        vo.setTotalAmount(order.getTotalAmount());
        vo.setQuantity(order.getQuantity());
        vo.setStatus(order.getStatus());
        vo.setPayTime(order.getPayTime());
        vo.setPayExpireTime(order.getPayExpireTime());
        vo.setCancelTime(order.getCancelTime());
        vo.setCancelReason(order.getCancelReason());
        vo.setCreateTime(order.getCreateTime());
        return vo;
    }

    /** 订单实体转结果 VO */
    private SeckillResultVO toResultVO(OshSeckillOrder order) {
        SeckillResultVO vo = new SeckillResultVO();
        vo.setSeckillNo(order.getSeckillNo());
        vo.setOrderNo(order.getOrderNo()); // orderNo 是统一订单号，与 seckillNo 严格分离
        vo.setStatus(order.getStatus());
        vo.setGoodsId(order.getGoodsId());
        vo.setGoodsType(order.getGoodsType());
        vo.setGoodsTitle(order.getGoodsTitle());
        vo.setGoodsCover(order.getGoodsCover());
        vo.setOriginPrice(order.getOriginPrice());
        vo.setSeckillPrice(order.getSeckillPrice());
        vo.setTotalAmount(order.getTotalAmount());
        vo.setQuantity(order.getQuantity());
        vo.setPayExpireTime(order.getPayExpireTime());
        return vo;
    }

    // ==================== 内部消息类 ====================

    /**
     * Kafka 消息体（秒杀订单创建消息）
     */
    public static class SeckillOrderMessage {
        /** 秒杀尝试号（Lua成功后生成，消费者幂等判重依据） */
        private String seckillNo;
        private Long activityId;
        private Long itemId;
        private Long userId;
        private Long goodsId;
        private Integer goodsType;
        private String goodsTitle;
        private String goodsCover;
        private BigDecimal originPrice;
        private BigDecimal seckillPrice;
        private Integer quantity;
        private Date payExpireTime;
        /** Redis 库存 Key，消费者失败时用于回滚 */
        private String stockKey;
        /** Redis 已购数量 Key，消费者失败时用于回滚 */
        private String boughtCntKey;
        /** Redis 未完成订单锁 Key，value=seckillNo，消费者成功后延长 TTL */
        private String orderKey;
        /** orderKey 的过期时间（秒），对应支付超时时间 */
        private long expireSeconds;
        /** 用户客户端 IP，从 HTTP 请求线程获取后传入消费者 */
        private String clientIp;

        public String getSeckillNo() { return seckillNo; }
        public void setSeckillNo(String seckillNo) { this.seckillNo = seckillNo; }
        public Long getActivityId() { return activityId; }
        public void setActivityId(Long activityId) { this.activityId = activityId; }
        public Long getItemId() { return itemId; }
        public void setItemId(Long itemId) { this.itemId = itemId; }
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public Long getGoodsId() { return goodsId; }
        public void setGoodsId(Long goodsId) { this.goodsId = goodsId; }
        public Integer getGoodsType() { return goodsType; }
        public void setGoodsType(Integer goodsType) { this.goodsType = goodsType; }
        public String getGoodsTitle() { return goodsTitle; }
        public void setGoodsTitle(String goodsTitle) { this.goodsTitle = goodsTitle; }
        public String getGoodsCover() { return goodsCover; }
        public void setGoodsCover(String goodsCover) { this.goodsCover = goodsCover; }
        public BigDecimal getOriginPrice() { return originPrice; }
        public void setOriginPrice(BigDecimal originPrice) { this.originPrice = originPrice; }
        public BigDecimal getSeckillPrice() { return seckillPrice; }
        public void setSeckillPrice(BigDecimal seckillPrice) { this.seckillPrice = seckillPrice; }
        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
        public Date getPayExpireTime() { return payExpireTime; }
        public void setPayExpireTime(Date payExpireTime) { this.payExpireTime = payExpireTime; }
        public String getStockKey() { return stockKey; }
        public void setStockKey(String stockKey) { this.stockKey = stockKey; }
        public String getBoughtCntKey() { return boughtCntKey; }
        public void setBoughtCntKey(String boughtCntKey) { this.boughtCntKey = boughtCntKey; }
        public String getOrderKey() { return orderKey; }
        public void setOrderKey(String orderKey) { this.orderKey = orderKey; }
        public long getExpireSeconds() { return expireSeconds; }
        public void setExpireSeconds(long expireSeconds) { this.expireSeconds = expireSeconds; }
        public String getClientIp() { return clientIp; }
        public void setClientIp(String clientIp) { this.clientIp = clientIp; }
    }
}
