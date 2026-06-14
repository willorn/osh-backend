package com.backstage.system.service.order;

import com.backstage.system.domain.vo.order.AdminOrderDashboardVO;
import com.backstage.system.domain.vo.order.AdminOrderDetailVO;
import com.backstage.system.domain.vo.order.AdminOrderPageItemVO;
import com.backstage.system.domain.vo.order.AdminOrderPageQueryVO;
import com.backstage.system.domain.vo.order.AdminOrderPaymentVO;
import com.github.pagehelper.PageInfo;

/**
 * 管理端订单查询与看板服务。
 */
public interface AdminOrderService {

    /**
     * 查询订单看板汇总指标。
     *
     * @param range 时间范围编码
     * @param beginTime 开始时间
     * @param endTime 结束时间
     * @return 汇总指标
     */
    AdminOrderDashboardVO.SummaryVO getSummary(String range, String beginTime, String endTime);

    /**
     * 查询每日流水趋势。
     *
     * @param range 时间范围编码
     * @param beginTime 开始时间
     * @param endTime 结束时间
     * @return 每日流水趋势
     */
    AdminOrderDashboardVO.RevenueTrendVO getRevenueTrend(String range, String beginTime, String endTime);

    /**
     * 查询支付结构。
     *
     * @param range 时间范围编码
     * @param beginTime 开始时间
     * @param endTime 结束时间
     * @return 支付结构
     */
    AdminOrderDashboardVO.PaymentMixVO getPaymentMix(String range, String beginTime, String endTime);

    /**
     * 查询下单转化漏斗。
     *
     * @param range 时间范围编码
     * @param beginTime 开始时间
     * @param endTime 结束时间
     * @return 转化漏斗
     */
    AdminOrderDashboardVO.FunnelVO getFunnel(String range, String beginTime, String endTime);

    /**
     * 查询积分消费分析。
     *
     * @param range 时间范围编码
     * @param beginTime 开始时间
     * @param endTime 结束时间
     * @return 积分消费分析
     */
    AdminOrderDashboardVO.PointsVO getPoints(String range, String beginTime, String endTime);

    /**
     * 查询商品排行和风险摘要。
     *
     * @param range 时间范围编码
     * @param beginTime 开始时间
     * @param endTime 结束时间
     * @return 商品排行和风险摘要
     */
    AdminOrderDashboardVO.RankingsVO getRankings(String range, String beginTime, String endTime);

    /**
     * 分页查询订单列表。
     *
     * @param query 查询条件
     * @return 分页订单
     */
    PageInfo<AdminOrderPageItemVO> pageOrders(AdminOrderPageQueryVO query);

    /**
     * 查询订单详情。
     *
     * @param orderNo 订单号
     * @return 订单详情
     */
    AdminOrderDetailVO getOrderDetail(String orderNo);

    /**
     * 查询订单支付流水。
     *
     * @param orderNo 订单号
     * @return 支付流水
     */
    AdminOrderPaymentVO getPayment(String orderNo);

    /**
     * 关闭待支付订单。
     *
     * @param orderNo 订单号
     */
    void closePendingOrder(String orderNo);

    /**
     * 重新执行支付成功后的履约处理。
     *
     * @param orderNo 订单号
     */
    void retryFulfillment(String orderNo);
}
