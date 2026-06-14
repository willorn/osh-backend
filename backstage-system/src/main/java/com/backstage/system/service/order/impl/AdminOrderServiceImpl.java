package com.backstage.system.service.order.impl;

import com.backstage.common.exception.ServiceException;
import com.backstage.common.utils.StringUtils;
import com.backstage.system.domain.order.OshOrder;
import com.backstage.system.domain.order.enums.OrderStatusEnum;
import com.backstage.system.domain.order.enums.PayChannelEnum;
import com.backstage.system.domain.order.enums.PaymentStatusEnum;
import com.backstage.system.domain.order.enums.ProductTypeEnum;
import com.backstage.system.domain.vo.order.AdminOrderDashboardVO;
import com.backstage.system.domain.vo.order.AdminOrderDetailVO;
import com.backstage.system.domain.vo.order.AdminOrderPageItemVO;
import com.backstage.system.domain.vo.order.AdminOrderPageQueryVO;
import com.backstage.system.domain.vo.order.AdminOrderPaymentVO;
import com.backstage.system.mapper.order.OshOrderMapper;
import com.backstage.system.service.order.AdminOrderService;
import com.backstage.system.service.order.OrderPaidHandlerRegistry;
import com.backstage.system.service.order.OrderService;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * 管理端订单查询与看板服务实现。
 */
@Service
public class AdminOrderServiceImpl implements AdminOrderService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int DEFAULT_PAGE_NUM = 1;
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    @Resource
    private OshOrderMapper oshOrderMapper;

    @Resource
    private OrderService orderService;

    @Resource
    private OrderPaidHandlerRegistry orderPaidHandlerRegistry;

    /**
     * 查询订单看板汇总指标。
     *
     * @param range 时间范围编码
     * @param beginTime 开始时间
     * @param endTime 结束时间
     * @return 汇总指标
     */
    @Override
    public AdminOrderDashboardVO.SummaryVO getSummary(String range, String beginTime, String endTime) {
        TimeRange timeRange = resolveRange(range, beginTime, endTime);
        AdminOrderDashboardVO.SummaryAggregateVO aggregate = oshOrderMapper.selectAdminOrderSummary(timeRange.begin, timeRange.end);
        AdminOrderDashboardVO.SummaryVO summary = aggregate == null ? new AdminOrderDashboardVO.SummaryVO() : aggregate;
        summary.setPaidRate(percent(summary.getPaidOrderCount(), summary.getTotalOrderCount()));
        summary.setPendingRiskCount(oshOrderMapper.countTimeoutPendingOrders(timeRange.begin, timeRange.end, LocalDateTime.now().minusMinutes(30)));
        summary.setGeneratedTime(LocalDateTime.now());
        summary.setStatusCards(buildStatusCards(summary));
        return summary;
    }

    /**
     * 查询每日流水趋势。
     *
     * @param range 时间范围编码
     * @param beginTime 开始时间
     * @param endTime 结束时间
     * @return 每日流水趋势
     */
    @Override
    public AdminOrderDashboardVO.RevenueTrendVO getRevenueTrend(String range, String beginTime, String endTime) {
        TimeRange timeRange = resolveRange(range, beginTime, endTime);
        AdminOrderDashboardVO.RevenueTrendVO vo = new AdminOrderDashboardVO.RevenueTrendVO();
        vo.setRows(oshOrderMapper.selectAdminOrderRevenueTrend(timeRange.begin, timeRange.end));
        return vo;
    }

    /**
     * 查询支付结构。
     *
     * @param range 时间范围编码
     * @param beginTime 开始时间
     * @param endTime 结束时间
     * @return 支付结构
     */
    @Override
    public AdminOrderDashboardVO.PaymentMixVO getPaymentMix(String range, String beginTime, String endTime) {
        TimeRange timeRange = resolveRange(range, beginTime, endTime);
        List<AdminOrderDashboardVO.PaymentMixRowVO> rows = oshOrderMapper.selectAdminOrderPaymentMix(timeRange.begin, timeRange.end);
        long totalCount = rows.stream().mapToLong(row -> safeLong(row.getCount())).sum();
        BigDecimal totalAmount = rows.stream()
                .map(row -> safeMoney(row.getAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        for (AdminOrderDashboardVO.PaymentMixRowVO row : rows) {
            row.setPayTypeName(channelName(row.getPayType()));
            row.setRate(percent(row.getCount(), totalCount));
        }
        AdminOrderDashboardVO.PaymentMixVO vo = new AdminOrderDashboardVO.PaymentMixVO();
        vo.setTotalCount(totalCount);
        vo.setTotalAmount(totalAmount);
        vo.setRows(rows);
        return vo;
    }

    /**
     * 查询下单转化漏斗。
     *
     * @param range 时间范围编码
     * @param beginTime 开始时间
     * @param endTime 结束时间
     * @return 转化漏斗
     */
    @Override
    public AdminOrderDashboardVO.FunnelVO getFunnel(String range, String beginTime, String endTime) {
        TimeRange timeRange = resolveRange(range, beginTime, endTime);
        AdminOrderDashboardVO.FunnelAggregateVO aggregate = oshOrderMapper.selectAdminOrderFunnel(timeRange.begin, timeRange.end);
        long createdCount = aggregate == null ? 0L : safeLong(aggregate.getCreatedCount());
        long paymentCreatedCount = aggregate == null ? 0L : safeLong(aggregate.getPaymentCreatedCount());
        long paidCount = aggregate == null ? 0L : safeLong(aggregate.getPaidCount());

        AdminOrderDashboardVO.FunnelVO vo = new AdminOrderDashboardVO.FunnelVO();
        vo.setRows(Arrays.asList(
                funnelRow("created", "创建订单", createdCount, createdCount),
                funnelRow("payment_created", "拉起支付", paymentCreatedCount, createdCount),
                funnelRow("paid", "支付成功", paidCount, createdCount),
                funnelRow("fulfilled", "履约完成", paidCount, createdCount)
        ));
        return vo;
    }

    /**
     * 查询积分消费分析。
     *
     * @param range 时间范围编码
     * @param beginTime 开始时间
     * @param endTime 结束时间
     * @return 积分消费分析
     */
    @Override
    public AdminOrderDashboardVO.PointsVO getPoints(String range, String beginTime, String endTime) {
        TimeRange timeRange = resolveRange(range, beginTime, endTime);
        AdminOrderDashboardVO.PointsVO vo = oshOrderMapper.selectAdminOrderPointsSummary(timeRange.begin, timeRange.end);
        if (vo == null) {
            vo = new AdminOrderDashboardVO.PointsVO();
        }
        List<AdminOrderDashboardVO.PointsSourceRowVO> sources = oshOrderMapper.selectAdminOrderPointsSources(timeRange.begin, timeRange.end);
        long totalPoints = safeLong(vo.getUsedPoints());
        for (AdminOrderDashboardVO.PointsSourceRowVO row : sources) {
            row.setProductTypeName(productTypeName(row.getProductType()));
            row.setRate(percent(row.getUsedPoints(), totalPoints));
        }
        vo.setSources(sources);
        return vo;
    }

    /**
     * 查询商品排行和风险摘要。
     *
     * @param range 时间范围编码
     * @param beginTime 开始时间
     * @param endTime 结束时间
     * @return 商品排行和风险摘要
     */
    @Override
    public AdminOrderDashboardVO.RankingsVO getRankings(String range, String beginTime, String endTime) {
        TimeRange timeRange = resolveRange(range, beginTime, endTime);
        List<AdminOrderDashboardVO.ProductRankRowVO> topProducts = oshOrderMapper.selectAdminOrderTopProducts(timeRange.begin, timeRange.end);
        for (AdminOrderDashboardVO.ProductRankRowVO row : topProducts) {
            row.setProductTypeName(productTypeName(row.getProductType()));
        }
        AdminOrderDashboardVO.RiskAggregateVO riskAggregate = oshOrderMapper.selectAdminOrderRisks(timeRange.begin, timeRange.end, LocalDateTime.now().minusMinutes(30));

        AdminOrderDashboardVO.RankingsVO vo = new AdminOrderDashboardVO.RankingsVO();
        vo.setTopProducts(topProducts);
        vo.setRisks(buildRisks(riskAggregate));
        return vo;
    }

    /**
     * 分页查询订单列表。
     *
     * @param query 查询条件
     * @return 分页订单
     */
    @Override
    public PageInfo<AdminOrderPageItemVO> pageOrders(AdminOrderPageQueryVO query) {
        AdminOrderPageQueryVO actualQuery = normalizeQuery(query);
        PageHelper.startPage(actualQuery.getPageNum(), actualQuery.getPageSize());
        List<AdminOrderPageItemVO> rows = oshOrderMapper.selectAdminOrderPage(actualQuery);
        fillOrderDisplayFields(rows);
        return new PageInfo<>(rows);
    }

    /**
     * 查询订单详情。
     *
     * @param orderNo 订单号
     * @return 订单详情
     */
    @Override
    public AdminOrderDetailVO getOrderDetail(String orderNo) {
        requireOrderNo(orderNo);
        AdminOrderPageItemVO item = oshOrderMapper.selectAdminOrderDetail(orderNo);
        if (item == null) {
            throw new ServiceException("订单不存在，orderNo=" + orderNo);
        }
        fillOrderDisplayFields(Arrays.asList(item));
        AdminOrderDetailVO detail = new AdminOrderDetailVO();
        BeanUtils.copyProperties(item, detail);
        detail.setPayments(Arrays.asList(getPayment(orderNo)));
        return detail;
    }

    /**
     * 查询订单支付流水。
     *
     * @param orderNo 订单号
     * @return 支付流水
     */
    @Override
    public AdminOrderPaymentVO getPayment(String orderNo) {
        requireOrderNo(orderNo);
        AdminOrderPaymentVO payment = oshOrderMapper.selectAdminOrderPayment(orderNo);
        if (payment == null) {
            throw new ServiceException("支付流水不存在，orderNo=" + orderNo);
        }
        payment.setChannelName(channelName(payment.getChannel()));
        payment.setStatusName(paymentStatusName(payment.getStatus()));
        List<AdminOrderPaymentVO.NotifyLogVO> notifyLogs = oshOrderMapper.selectAdminOrderPaymentNotifyLogs(orderNo);
        payment.setNotifyLogs(notifyLogs);
        return payment;
    }

    /**
     * 关闭待支付订单。
     *
     * @param orderNo 订单号
     */
    @Override
    public void closePendingOrder(String orderNo) {
        requireOrderNo(orderNo);
        orderService.cancelPaymentByOrderNo(orderNo);
    }

    /**
     * 重新执行支付成功后的履约处理。
     *
     * @param orderNo 订单号
     */
    @Override
    public void retryFulfillment(String orderNo) {
        requireOrderNo(orderNo);
        OshOrder order = oshOrderMapper.selectByOrderNo(orderNo);
        if (order == null) {
            throw new ServiceException("订单不存在，orderNo=" + orderNo);
        }
        if (!Objects.equals(order.getStatus(), OrderStatusEnum.PAID.getCode())) {
            throw new ServiceException("仅支付成功订单可重新履约，orderNo=" + orderNo + ", status=" + order.getStatus());
        }
        ProductTypeEnum productType = ProductTypeEnum.fromCode(order.getProductType());
        if (productType == null) {
            throw new ServiceException("订单商品类型不支持重新履约，orderNo=" + orderNo + ", productType=" + order.getProductType());
        }
        orderPaidHandlerRegistry.handle(productType.getName(), orderNo);
    }

    /**
     * 构建订单状态卡片。
     *
     * @param summary 汇总指标
     * @return 状态卡片列表
     */
    private List<AdminOrderDashboardVO.StatusCardVO> buildStatusCards(AdminOrderDashboardVO.SummaryVO summary) {
        List<AdminOrderDashboardVO.StatusCardVO> cards = new ArrayList<>();
        cards.add(statusCard(OrderStatusEnum.PAID.getCode(), OrderStatusEnum.PAID.getDesc(), summary.getPaidOrderCount()));
        cards.add(statusCard(OrderStatusEnum.PENDING.getCode(), OrderStatusEnum.PENDING.getDesc(), summary.getPendingOrderCount()));
        cards.add(statusCard(OrderStatusEnum.CLOSED.getCode(), OrderStatusEnum.CLOSED.getDesc(), summary.getClosedOrderCount()));
        cards.add(statusCard(OrderStatusEnum.CANCELED.getCode(), OrderStatusEnum.CANCELED.getDesc(), summary.getCanceledOrderCount()));
        return cards;
    }

    /**
     * 构建订单状态卡片。
     *
     * @param status 订单状态
     * @param statusName 订单状态名称
     * @param count 数量
     * @return 状态卡片
     */
    private AdminOrderDashboardVO.StatusCardVO statusCard(Integer status, String statusName, Long count) {
        AdminOrderDashboardVO.StatusCardVO card = new AdminOrderDashboardVO.StatusCardVO();
        card.setStatus(status);
        card.setStatusName(statusName);
        card.setCount(safeLong(count));
        return card;
    }

    /**
     * 构建漏斗行。
     *
     * @param stage 阶段编码
     * @param stageName 阶段名称
     * @param count 当前阶段数量
     * @param total 总数量
     * @return 漏斗行
     */
    private AdminOrderDashboardVO.FunnelRowVO funnelRow(String stage, String stageName, Long count, Long total) {
        AdminOrderDashboardVO.FunnelRowVO row = new AdminOrderDashboardVO.FunnelRowVO();
        row.setStage(stage);
        row.setStageName(stageName);
        row.setCount(safeLong(count));
        row.setRate(percent(count, total));
        return row;
    }

    /**
     * 构建风险摘要。
     *
     * @param aggregate 风险聚合结果
     * @return 风险摘要列表
     */
    private List<AdminOrderDashboardVO.RiskRowVO> buildRisks(AdminOrderDashboardVO.RiskAggregateVO aggregate) {
        AdminOrderDashboardVO.RiskAggregateVO actual = aggregate == null ? new AdminOrderDashboardVO.RiskAggregateVO() : aggregate;
        List<AdminOrderDashboardVO.RiskRowVO> risks = new ArrayList<>();
        risks.add(riskRow("timeout_pending", "超时未关闭", "待支付超过 30 分钟仍未关闭", actual.getTimeoutPendingCount()));
        risks.add(riskRow("amount_mismatch", "金额不一致", "订单应付金额与支付流水金额不一致", actual.getAmountMismatchCount()));
        risks.add(riskRow("payment_failed", "支付失败", "支付流水已失败但订单仍需核对", actual.getPaymentFailedCount()));
        return risks;
    }

    /**
     * 构建风险行。
     *
     * @param riskType 风险类型
     * @param riskName 风险名称
     * @param description 风险说明
     * @param count 数量
     * @return 风险行
     */
    private AdminOrderDashboardVO.RiskRowVO riskRow(String riskType, String riskName, String description, Long count) {
        AdminOrderDashboardVO.RiskRowVO row = new AdminOrderDashboardVO.RiskRowVO();
        row.setRiskType(riskType);
        row.setRiskName(riskName);
        row.setDescription(description);
        row.setCount(safeLong(count));
        return row;
    }

    /**
     * 补充订单列表展示字段。
     *
     * @param rows 订单列表
     */
    private void fillOrderDisplayFields(List<AdminOrderPageItemVO> rows) {
        if (rows == null) {
            return;
        }
        for (AdminOrderPageItemVO row : rows) {
            row.setStatusName(orderDisplayStatusName(row.getStatus(), row.getPaymentStatus(), row.getPaidTime()));
            row.setProductTypeName(productTypeName(row.getProductType()));
            row.setPayChannelName(channelName(row.getPayChannel()));
            row.setPaymentStatusName(paymentStatusName(row.getPaymentStatus()));
        }
    }

    /**
     * 规范化分页查询参数。
     *
     * @param query 原始查询参数
     * @return 规范化后的查询参数
     */
    private AdminOrderPageQueryVO normalizeQuery(AdminOrderPageQueryVO query) {
        AdminOrderPageQueryVO actual = query == null ? new AdminOrderPageQueryVO() : query;
        int pageNum = actual.getPageNum() == null || actual.getPageNum() < 1 ? DEFAULT_PAGE_NUM : actual.getPageNum();
        int pageSize = actual.getPageSize() == null || actual.getPageSize() < 1 ? DEFAULT_PAGE_SIZE : Math.min(actual.getPageSize(), MAX_PAGE_SIZE);
        actual.setPageNum(pageNum);
        actual.setPageSize(pageSize);
        return actual;
    }

    /**
     * 解析看板时间范围。
     *
     * @param range 范围编码
     * @param beginTime 开始时间字符串
     * @param endTime 结束时间字符串
     * @return 时间范围
     */
    private TimeRange resolveRange(String range, String beginTime, String endTime) {
        LocalDateTime begin = parseTime(beginTime);
        LocalDateTime end = parseTime(endTime);
        if (begin != null || end != null) {
            return new TimeRange(begin, end);
        }

        LocalDate today = LocalDate.now();
        String actualRange = StringUtils.isBlank(range) ? "7d" : range;
        if ("30d".equals(actualRange)) {
            return new TimeRange(today.minusDays(29).atStartOfDay(), LocalDateTime.of(today, LocalTime.MAX));
        }
        if ("month".equals(actualRange)) {
            return new TimeRange(today.withDayOfMonth(1).atStartOfDay(), LocalDateTime.of(today, LocalTime.MAX));
        }
        if ("quarter".equals(actualRange)) {
            int currentMonth = today.getMonthValue();
            int firstQuarterMonth = ((currentMonth - 1) / 3) * 3 + 1;
            return new TimeRange(LocalDate.of(today.getYear(), firstQuarterMonth, 1).atStartOfDay(), LocalDateTime.of(today, LocalTime.MAX));
        }
        return new TimeRange(today.minusDays(6).atStartOfDay(), LocalDateTime.of(today, LocalTime.MAX));
    }

    /**
     * 解析 yyyy-MM-dd HH:mm:ss 时间。
     *
     * @param value 时间字符串
     * @return 时间
     */
    private LocalDateTime parseTime(String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        return LocalDateTime.parse(value, DATE_TIME_FORMATTER);
    }

    /**
     * 计算百分比。
     *
     * @param value 分子
     * @param total 分母
     * @return 百分比
     */
    private BigDecimal percent(Long value, Long total) {
        long safeTotal = safeLong(total);
        if (safeTotal <= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(safeLong(value))
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(safeTotal), 2, RoundingMode.HALF_UP);
    }

    /**
     * 获取安全 long 值。
     *
     * @param value 原始值
     * @return long 值
     */
    private long safeLong(Long value) {
        return value == null ? 0L : value;
    }

    /**
     * 获取安全金额。
     *
     * @param value 原始金额
     * @return 金额
     */
    private BigDecimal safeMoney(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    /**
     * 校验订单号。
     *
     * @param orderNo 订单号
     */
    private void requireOrderNo(String orderNo) {
        if (StringUtils.isBlank(orderNo)) {
            throw new ServiceException("订单号不能为空");
        }
    }

    /**
     * 订单状态名称。
     *
     * @param status 状态值
     * @return 状态名称
     */
    private String orderStatusName(Integer status) {
        OrderStatusEnum statusEnum = OrderStatusEnum.fromCode(status);
        return statusEnum == null ? "未知状态" : statusEnum.getDesc();
    }

    /**
     * 订单列表展示状态名称。
     *
     * @param orderStatus 订单状态
     * @param paymentStatus 支付流水状态
     * @param paidTime 支付时间
     * @return 展示状态名称
     */
    private String orderDisplayStatusName(Integer orderStatus, Integer paymentStatus, LocalDateTime paidTime) {
        if (Objects.equals(paymentStatus, PaymentStatusEnum.SUCCESS.getCode()) || paidTime != null) {
            return OrderStatusEnum.PAID.getDesc();
        }
        return orderStatusName(orderStatus);
    }

    /**
     * 商品类型名称。
     *
     * @param productType 商品类型
     * @return 商品类型名称
     */
    private String productTypeName(Integer productType) {
        ProductTypeEnum typeEnum = ProductTypeEnum.fromCode(productType);
        return typeEnum == null ? "未知商品" : typeEnum.getDesc();
    }

    /**
     * 支付渠道名称。
     *
     * @param channel 支付渠道
     * @return 支付渠道名称
     */
    private String channelName(Integer channel) {
        if (channel == null) {
            return "未支付";
        }
        PayChannelEnum channelEnum = PayChannelEnum.fromCode(channel);
        return channelEnum == null ? "未知渠道" : channelEnum.getDesc();
    }

    /**
     * 支付渠道名称。
     *
     * @param payType 支付渠道字符串
     * @return 支付渠道名称
     */
    private String channelName(String payType) {
        if (StringUtils.isBlank(payType)) {
            return "未支付";
        }
        return channelName(Integer.valueOf(payType));
    }

    /**
     * 支付状态名称。
     *
     * @param status 支付状态
     * @return 支付状态名称
     */
    private String paymentStatusName(Integer status) {
        if (status == null) {
            return "未支付";
        }
        for (PaymentStatusEnum statusEnum : PaymentStatusEnum.values()) {
            if (statusEnum.getCode() == status) {
                return statusEnum.getDesc();
            }
        }
        return "未知状态";
    }

    /**
     * 看板时间范围。
     */
    private static class TimeRange {
        private final LocalDateTime begin;
        private final LocalDateTime end;

        private TimeRange(LocalDateTime begin, LocalDateTime end) {
            this.begin = begin;
            this.end = end;
        }
    }
}
