package com.backstage.system.mapper.order;

import com.backstage.system.domain.order.OshOrder;
import com.backstage.system.domain.vo.order.AdminOrderDashboardVO;
import com.backstage.system.domain.vo.order.AdminOrderPageItemVO;
import com.backstage.system.domain.vo.order.AdminOrderPageQueryVO;
import com.backstage.system.domain.vo.order.AdminOrderPaymentVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface OshOrderMapper extends BaseMapper<OshOrder> {

    int insertOshOrder(OshOrder order);

    OshOrder selectByOrderNo(@Param("orderNo") String orderNo);

    int updatePendingToPaid(@Param("orderNo") String orderNo, @Param("paidTime") LocalDateTime paidTime);

    int updatePendingToClosed(@Param("orderNo") String orderNo, @Param("closeTime") LocalDateTime closeTime);

    /**
     * 按用户ID查询订单列表（按创建时间倒序）
     */
    List<OshOrder> selectByUserId(@Param("userId") Long userId);

    /**
     * 按用户ID和状态查询订单列表（status 为 null 时查全部）
     */
    List<OshOrder> selectByUserIdAndStatus(@Param("userId") Long userId, @Param("status") Integer status);

    /**
     * 查询管理端订单看板汇总。
     *
     * @param beginTime 开始时间
     * @param endTime 结束时间
     * @return 汇总指标
     */
    AdminOrderDashboardVO.SummaryAggregateVO selectAdminOrderSummary(@Param("beginTime") LocalDateTime beginTime,
                                                                     @Param("endTime") LocalDateTime endTime);

    /**
     * 查询管理端每日流水趋势。
     *
     * @param beginTime 开始时间
     * @param endTime 结束时间
     * @return 每日流水趋势
     */
    List<AdminOrderDashboardVO.RevenueTrendRowVO> selectAdminOrderRevenueTrend(@Param("beginTime") LocalDateTime beginTime,
                                                                               @Param("endTime") LocalDateTime endTime);

    /**
     * 查询管理端支付结构。
     *
     * @param beginTime 开始时间
     * @param endTime 结束时间
     * @return 支付结构
     */
    List<AdminOrderDashboardVO.PaymentMixRowVO> selectAdminOrderPaymentMix(@Param("beginTime") LocalDateTime beginTime,
                                                                           @Param("endTime") LocalDateTime endTime);

    /**
     * 查询管理端下单转化漏斗。
     *
     * @param beginTime 开始时间
     * @param endTime 结束时间
     * @return 转化漏斗聚合
     */
    AdminOrderDashboardVO.FunnelAggregateVO selectAdminOrderFunnel(@Param("beginTime") LocalDateTime beginTime,
                                                                   @Param("endTime") LocalDateTime endTime);

    /**
     * 查询管理端积分消费汇总。
     *
     * @param beginTime 开始时间
     * @param endTime 结束时间
     * @return 积分消费汇总
     */
    AdminOrderDashboardVO.PointsVO selectAdminOrderPointsSummary(@Param("beginTime") LocalDateTime beginTime,
                                                                 @Param("endTime") LocalDateTime endTime);

    /**
     * 查询管理端积分消费来源。
     *
     * @param beginTime 开始时间
     * @param endTime 结束时间
     * @return 积分消费来源
     */
    List<AdminOrderDashboardVO.PointsSourceRowVO> selectAdminOrderPointsSources(@Param("beginTime") LocalDateTime beginTime,
                                                                                @Param("endTime") LocalDateTime endTime);

    /**
     * 查询管理端商品贡献排行。
     *
     * @param beginTime 开始时间
     * @param endTime 结束时间
     * @return 商品贡献排行
     */
    List<AdminOrderDashboardVO.ProductRankRowVO> selectAdminOrderTopProducts(@Param("beginTime") LocalDateTime beginTime,
                                                                             @Param("endTime") LocalDateTime endTime);

    /**
     * 查询管理端风险摘要。
     *
     * @param beginTime 开始时间
     * @param endTime 结束时间
     * @param timeoutTime 待支付超时时间
     * @return 风险摘要
     */
    AdminOrderDashboardVO.RiskAggregateVO selectAdminOrderRisks(@Param("beginTime") LocalDateTime beginTime,
                                                                @Param("endTime") LocalDateTime endTime,
                                                                @Param("timeoutTime") LocalDateTime timeoutTime);

    /**
     * 统计超时未支付订单数。
     *
     * @param beginTime 开始时间
     * @param endTime 结束时间
     * @param timeoutTime 待支付超时时间
     * @return 超时未支付订单数
     */
    Long countTimeoutPendingOrders(@Param("beginTime") LocalDateTime beginTime,
                                   @Param("endTime") LocalDateTime endTime,
                                   @Param("timeoutTime") LocalDateTime timeoutTime);

    /**
     * 分页查询管理端订单列表。
     *
     * @param query 查询条件
     * @return 订单列表
     */
    List<AdminOrderPageItemVO> selectAdminOrderPage(@Param("query") AdminOrderPageQueryVO query);

    /**
     * 查询管理端订单详情。
     *
     * @param orderNo 订单号
     * @return 订单详情
     */
    AdminOrderPageItemVO selectAdminOrderDetail(@Param("orderNo") String orderNo);

    /**
     * 查询管理端订单支付流水。
     *
     * @param orderNo 订单号
     * @return 支付流水
     */
    AdminOrderPaymentVO selectAdminOrderPayment(@Param("orderNo") String orderNo);

    /**
     * 查询管理端订单支付回调记录。
     *
     * @param orderNo 订单号
     * @return 回调记录
     */
    List<AdminOrderPaymentVO.NotifyLogVO> selectAdminOrderPaymentNotifyLogs(@Param("orderNo") String orderNo);
}
