package com.backstage.common.constant;

import java.util.Date;

/**
 * 秒杀模块 Redis Key 常量
 * 统一管理所有 Key 前缀和过期时间，避免多处定义不一致
 *
 * Key 规则：
 *   seckill:stock:{activityId}:{itemId}                  库存计数器
 *   seckill:bought:{activityId}:{itemId}                 已购用户 Set（废弃，保留兼容）
 *   seckill:bought_cnt:{activityId}:{itemId}:{userId}    用户当前占用的限购额度（待支付+已支付，取消/超时后回滚）
 *   seckill:activity:{activityId}                        活动基本信息缓存
 *   seckill:item:{itemId}                                明细信息缓存
 *   seckill:order:{activityId}:{itemId}:{userId}         当前未完成秒杀尝试号（value=seckillNo，存在即表示有未完成订单）
 *
 * @author backstage
 * @date 2026-05-16
 */
public class SeckillCacheConstants {

    private SeckillCacheConstants() {}

    // ==================== Key 前缀 ====================

    public static final String SECKILL_STOCK_KEY    = "seckill:stock:";
    public static final String SECKILL_BOUGHT_KEY   = "seckill:bought:";
    /** 用户已购数量计数器 Key 前缀，完整 Key = 前缀 + activityId:itemId:userId */
    public static final String SECKILL_BOUGHT_CNT_KEY = "seckill:bought_cnt:";
    public static final String SECKILL_ACTIVITY_KEY = "seckill:activity:";
    public static final String SECKILL_ITEM_KEY     = "seckill:item:";
    public static final String SECKILL_ORDER_KEY    = "seckill:order:";
    /**
     * 防并发重复提交锁 Key 前缀，完整 Key = 前缀 + activityId:itemId:userId
     * TTL 建议 8 秒，仅防同一请求并发重复点击，与订单生命周期无关
     */
    public static final String SECKILL_SUBMIT_LOCK_KEY = "seckill:submit_lock:";
    /** 防重复提交短锁 TTL：8 秒 */
    public static final long SECKILL_SUBMIT_LOCK_EXPIRE_SECONDS = 8L;

    // ==================== 过期时间（秒） ====================

    /** 活动/明细信息缓存在活动结束后的保留缓冲时间：24 小时 */
    public static final long ACTIVITY_EXPIRE_BUFFER = 24 * 3600L;

    /** 库存 Key 在活动结束后的保留缓冲时间：2 小时 */
    public static final long STOCK_EXPIRE_BUFFER = 2 * 3600L;

    /** 已购数量 Key 在活动结束后的保留缓冲时间：2 小时 */
    public static final long BOUGHT_CNT_EXPIRE_BUFFER = 2 * 3600L;

    /**
     * 根据活动结束时间动态计算 Key 过期时间（秒）
     * = (endTime - now) + buffer
     * 若活动已结束（结果 <= 0），返回 buffer 本身作为兜底，避免写入永不过期的 Key
     *
     * @param endTime 活动结束时间
     * @param buffer  活动结束后额外保留的秒数
     * @return 过期时间（秒），最小为 buffer
     */
    public static long calcExpireSeconds(Date endTime, long buffer) {
        if (endTime == null) {
            return buffer;
        }
        long remaining = (endTime.getTime() - System.currentTimeMillis()) / 1000;
        return Math.max(remaining, 0) + buffer;
    }
}
