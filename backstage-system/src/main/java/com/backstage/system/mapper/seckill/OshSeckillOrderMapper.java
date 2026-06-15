package com.backstage.system.mapper.seckill;

import com.backstage.system.domain.seckill.OshSeckillOrder;
import com.backstage.system.domain.vo.seckill.SeckillRecentOrderVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 秒杀订单 Mapper 接口
 *
 * @author backstage
 * @date 2026-04-28
 */
public interface OshSeckillOrderMapper {

    /** 根据ID查询 */
    OshSeckillOrder selectOrderById(Long id);

    /** 根据秒杀尝试号查询（消费者幂等判重、秒杀结果轮询、用户取消） */
    OshSeckillOrder selectOrderBySeckillNo(String seckillNo);

    /** 根据统一订单号查询（支付成功回调、超时取消调支付系统） */
    OshSeckillOrder selectOrderByOrderNo(String orderNo);

    /**
     * 查询该用户该 item 当前未完成的订单（status IN (0)，即待支付）
     * 用于 getSeckillResult() 兜底查询（Redis Key 过期后的降级路径）
     */
    OshSeckillOrder selectPendingOrderByItemUser(@Param("itemId") Long itemId,
                                                 @Param("userId") Long userId);

    /**
     * 用于 getSeckillResult() 在 orderKey 已释放后的兜底查询
     * 只查询最近一笔有效订单（待支付/已支付），避免支付成功后立即轮询返回“查不到结果”
     */
    OshSeckillOrder selectLatestEffectiveOrderByItemUser(@Param("itemId") Long itemId,
                                                         @Param("userId") Long userId);

    /** 管理端列表查询（支持多条件筛选） */
    List<OshSeckillOrder> selectOrderList(OshSeckillOrder order);

    /** 新增秒杀订单 */
    int insertOrder(OshSeckillOrder order);

    /** 修改秒杀订单 */
    int updateOrder(OshSeckillOrder order);

    /**
     * CAS 方式更新订单状态，避免支付成功/用户取消/超时取消并发覆盖状态
     */
    int updateOrderStatusWithCheck(@Param("id") Long id,
                                   @Param("oldStatus") Integer oldStatus,
                                   @Param("newStatus") Integer newStatus,
                                   @Param("payTime") java.util.Date payTime,
                                   @Param("cancelTime") java.util.Date cancelTime,
                                   @Param("cancelReason") String cancelReason);

    /** 查询支付超时的待支付订单（status=0 且 pay_expire_time < now） */
    List<OshSeckillOrder> selectTimeoutOrders();

    /** 查询最近已支付订单，关联用户表取昵称/用户名，用于首页滚动条展示 */
    List<SeckillRecentOrderVO> selectRecentPaidOrders(@Param("limit") int limit);
}
