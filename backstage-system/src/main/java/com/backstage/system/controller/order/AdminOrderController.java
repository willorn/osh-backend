package com.backstage.system.controller.order;

import com.backstage.common.core.domain.R;
import com.backstage.system.domain.vo.order.AdminOrderDashboardVO;
import com.backstage.system.domain.vo.order.AdminOrderDetailVO;
import com.backstage.system.domain.vo.order.AdminOrderPageItemVO;
import com.backstage.system.domain.vo.order.AdminOrderPageQueryVO;
import com.backstage.system.domain.vo.order.AdminOrderPaymentVO;
import com.backstage.system.service.order.AdminOrderService;
import com.github.pagehelper.PageInfo;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.LinkedHashMap;

/**
 * 管理端订单看板与订单管理接口。
 */
@RestController
@RequestMapping("/pc/admin/order")
public class AdminOrderController {

    @Resource
    private AdminOrderService adminOrderService;

    /**
     * 查询订单看板汇总指标。
     *
     * @param range 时间范围编码
     * @param beginTime 开始时间
     * @param endTime 结束时间
     * @return 汇总指标
     */
    @GetMapping("/dashboard/summary")
    @PreAuthorize("hasAuthority('order:dashboard')")
    public R<AdminOrderDashboardVO.SummaryVO> summary(@RequestParam(required = false, defaultValue = "7d") String range,
                                                      @RequestParam(required = false) String beginTime,
                                                      @RequestParam(required = false) String endTime) {
        return R.ok(adminOrderService.getSummary(range, beginTime, endTime));
    }

    /**
     * 查询每日流水趋势。
     *
     * @param range 时间范围编码
     * @param beginTime 开始时间
     * @param endTime 结束时间
     * @return 每日流水趋势
     */
    @GetMapping("/dashboard/revenue-trend")
    @PreAuthorize("hasAuthority('order:dashboard')")
    public R<AdminOrderDashboardVO.RevenueTrendVO> revenueTrend(@RequestParam(required = false, defaultValue = "7d") String range,
                                                                @RequestParam(required = false) String beginTime,
                                                                @RequestParam(required = false) String endTime) {
        return R.ok(adminOrderService.getRevenueTrend(range, beginTime, endTime));
    }

    /**
     * 查询支付结构。
     *
     * @param range 时间范围编码
     * @param beginTime 开始时间
     * @param endTime 结束时间
     * @return 支付结构
     */
    @GetMapping("/dashboard/payment-mix")
    @PreAuthorize("hasAuthority('order:dashboard')")
    public R<AdminOrderDashboardVO.PaymentMixVO> paymentMix(@RequestParam(required = false, defaultValue = "7d") String range,
                                                            @RequestParam(required = false) String beginTime,
                                                            @RequestParam(required = false) String endTime) {
        return R.ok(adminOrderService.getPaymentMix(range, beginTime, endTime));
    }

    /**
     * 查询下单转化漏斗。
     *
     * @param range 时间范围编码
     * @param beginTime 开始时间
     * @param endTime 结束时间
     * @return 下单转化漏斗
     */
    @GetMapping("/dashboard/funnel")
    @PreAuthorize("hasAuthority('order:dashboard')")
    public R<AdminOrderDashboardVO.FunnelVO> funnel(@RequestParam(required = false, defaultValue = "7d") String range,
                                                    @RequestParam(required = false) String beginTime,
                                                    @RequestParam(required = false) String endTime) {
        return R.ok(adminOrderService.getFunnel(range, beginTime, endTime));
    }

    /**
     * 查询积分消费分析。
     *
     * @param range 时间范围编码
     * @param beginTime 开始时间
     * @param endTime 结束时间
     * @return 积分消费分析
     */
    @GetMapping("/dashboard/points")
    @PreAuthorize("hasAuthority('order:dashboard')")
    public R<AdminOrderDashboardVO.PointsVO> points(@RequestParam(required = false, defaultValue = "7d") String range,
                                                    @RequestParam(required = false) String beginTime,
                                                    @RequestParam(required = false) String endTime) {
        return R.ok(adminOrderService.getPoints(range, beginTime, endTime));
    }

    /**
     * 查询商品排行与风险摘要。
     *
     * @param range 时间范围编码
     * @param beginTime 开始时间
     * @param endTime 结束时间
     * @return 商品排行与风险摘要
     */
    @GetMapping("/dashboard/rankings")
    @PreAuthorize("hasAuthority('order:dashboard')")
    public R<AdminOrderDashboardVO.RankingsVO> rankings(@RequestParam(required = false, defaultValue = "7d") String range,
                                                        @RequestParam(required = false) String beginTime,
                                                        @RequestParam(required = false) String endTime) {
        return R.ok(adminOrderService.getRankings(range, beginTime, endTime));
    }

    /**
     * 分页查询订单管理列表。
     *
     * @param query 查询条件
     * @return 分页订单
     */
    @GetMapping("/page")
    @PreAuthorize("hasAuthority('order:list')")
    public R<LinkedHashMap<String, Object>> page(AdminOrderPageQueryVO query) {
        PageInfo<AdminOrderPageItemVO> pageInfo = adminOrderService.pageOrders(query);
        LinkedHashMap<String, Object> data = new LinkedHashMap<>();
        data.put("total", pageInfo.getTotal());
        data.put("rows", pageInfo.getList());
        return R.ok(data);
    }

    /**
     * 查询订单详情。
     *
     * @param orderNo 订单号
     * @return 订单详情
     */
    @GetMapping("/{orderNo}")
    @PreAuthorize("hasAuthority('order:detail')")
    public R<AdminOrderDetailVO> detail(@PathVariable String orderNo) {
        return R.ok(adminOrderService.getOrderDetail(orderNo));
    }

    /**
     * 查询订单支付流水。
     *
     * @param orderNo 订单号
     * @return 支付流水
     */
    @GetMapping("/{orderNo}/payment")
    @PreAuthorize("hasAuthority('payment:detail')")
    public R<AdminOrderPaymentVO> payment(@PathVariable String orderNo) {
        return R.ok(adminOrderService.getPayment(orderNo));
    }

    /**
     * 关闭待支付订单。
     *
     * @param orderNo 订单号
     * @return 操作结果
     */
    @PostMapping("/{orderNo}/close")
    @PreAuthorize("hasAuthority('order:close')")
    public R<String> close(@PathVariable String orderNo) {
        adminOrderService.closePendingOrder(orderNo);
        return R.ok("订单已关闭");
    }

    /**
     * 重新执行支付成功后的履约处理。
     *
     * @param orderNo 订单号
     * @return 操作结果
     */
    @PostMapping("/{orderNo}/fulfill/retry")
    @PreAuthorize("hasAuthority('order:fulfill:retry')")
    public R<String> retryFulfillment(@PathVariable String orderNo) {
        adminOrderService.retryFulfillment(orderNo);
        return R.ok("重新履约任务已触发");
    }
}
