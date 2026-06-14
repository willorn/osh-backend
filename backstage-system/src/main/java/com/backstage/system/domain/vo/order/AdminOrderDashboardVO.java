package com.backstage.system.domain.vo.order;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 管理端订单看板响应对象集合。
 */
public class AdminOrderDashboardVO {

    /**
     * 看板汇总指标。
     */
    public static class SummaryVO {
        private Long totalOrderCount = 0L;
        private Long paidOrderCount = 0L;
        private Long pendingOrderCount = 0L;
        private Long closedOrderCount = 0L;
        private Long canceledOrderCount = 0L;
        private BigDecimal paidAmount = BigDecimal.ZERO;
        private BigDecimal pendingAmount = BigDecimal.ZERO;
        private BigDecimal pointsDeductAmount = BigDecimal.ZERO;
        private BigDecimal paidRate = BigDecimal.ZERO;
        private Long pendingRiskCount = 0L;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime generatedTime;
        private List<StatusCardVO> statusCards = new ArrayList<>();

        public Long getTotalOrderCount() { return totalOrderCount; }
        public void setTotalOrderCount(Long totalOrderCount) { this.totalOrderCount = totalOrderCount; }
        public Long getPaidOrderCount() { return paidOrderCount; }
        public void setPaidOrderCount(Long paidOrderCount) { this.paidOrderCount = paidOrderCount; }
        public Long getPendingOrderCount() { return pendingOrderCount; }
        public void setPendingOrderCount(Long pendingOrderCount) { this.pendingOrderCount = pendingOrderCount; }
        public Long getClosedOrderCount() { return closedOrderCount; }
        public void setClosedOrderCount(Long closedOrderCount) { this.closedOrderCount = closedOrderCount; }
        public Long getCanceledOrderCount() { return canceledOrderCount; }
        public void setCanceledOrderCount(Long canceledOrderCount) { this.canceledOrderCount = canceledOrderCount; }
        public BigDecimal getPaidAmount() { return paidAmount; }
        public void setPaidAmount(BigDecimal paidAmount) { this.paidAmount = paidAmount; }
        public BigDecimal getPendingAmount() { return pendingAmount; }
        public void setPendingAmount(BigDecimal pendingAmount) { this.pendingAmount = pendingAmount; }
        public BigDecimal getPointsDeductAmount() { return pointsDeductAmount; }
        public void setPointsDeductAmount(BigDecimal pointsDeductAmount) { this.pointsDeductAmount = pointsDeductAmount; }
        public BigDecimal getPaidRate() { return paidRate; }
        public void setPaidRate(BigDecimal paidRate) { this.paidRate = paidRate; }
        public Long getPendingRiskCount() { return pendingRiskCount; }
        public void setPendingRiskCount(Long pendingRiskCount) { this.pendingRiskCount = pendingRiskCount; }
        public LocalDateTime getGeneratedTime() { return generatedTime; }
        public void setGeneratedTime(LocalDateTime generatedTime) { this.generatedTime = generatedTime; }
        public List<StatusCardVO> getStatusCards() { return statusCards; }
        public void setStatusCards(List<StatusCardVO> statusCards) { this.statusCards = statusCards; }
    }

    /**
     * 订单状态卡片。
     */
    public static class StatusCardVO {
        private Integer status;
        private String statusName;
        private Long count = 0L;

        public Integer getStatus() { return status; }
        public void setStatus(Integer status) { this.status = status; }
        public String getStatusName() { return statusName; }
        public void setStatusName(String statusName) { this.statusName = statusName; }
        public Long getCount() { return count; }
        public void setCount(Long count) { this.count = count; }
    }

    /**
     * 每日流水趋势。
     */
    public static class RevenueTrendVO {
        private List<RevenueTrendRowVO> rows = new ArrayList<>();

        public List<RevenueTrendRowVO> getRows() { return rows; }
        public void setRows(List<RevenueTrendRowVO> rows) { this.rows = rows; }
    }

    /**
     * 每日流水趋势行。
     */
    public static class RevenueTrendRowVO {
        private String day;
        private BigDecimal cashAmount = BigDecimal.ZERO;
        private BigDecimal pointsDeductAmount = BigDecimal.ZERO;
        private BigDecimal pendingAmount = BigDecimal.ZERO;
        private Long paidOrderCount = 0L;
        private Long pendingOrderCount = 0L;

        public String getDay() { return day; }
        public void setDay(String day) { this.day = day; }
        public BigDecimal getCashAmount() { return cashAmount; }
        public void setCashAmount(BigDecimal cashAmount) { this.cashAmount = cashAmount; }
        public BigDecimal getPointsDeductAmount() { return pointsDeductAmount; }
        public void setPointsDeductAmount(BigDecimal pointsDeductAmount) { this.pointsDeductAmount = pointsDeductAmount; }
        public BigDecimal getPendingAmount() { return pendingAmount; }
        public void setPendingAmount(BigDecimal pendingAmount) { this.pendingAmount = pendingAmount; }
        public Long getPaidOrderCount() { return paidOrderCount; }
        public void setPaidOrderCount(Long paidOrderCount) { this.paidOrderCount = paidOrderCount; }
        public Long getPendingOrderCount() { return pendingOrderCount; }
        public void setPendingOrderCount(Long pendingOrderCount) { this.pendingOrderCount = pendingOrderCount; }
    }

    /**
     * 支付结构。
     */
    public static class PaymentMixVO {
        private Long totalCount = 0L;
        private BigDecimal totalAmount = BigDecimal.ZERO;
        private List<PaymentMixRowVO> rows = new ArrayList<>();

        public Long getTotalCount() { return totalCount; }
        public void setTotalCount(Long totalCount) { this.totalCount = totalCount; }
        public BigDecimal getTotalAmount() { return totalAmount; }
        public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
        public List<PaymentMixRowVO> getRows() { return rows; }
        public void setRows(List<PaymentMixRowVO> rows) { this.rows = rows; }
    }

    /**
     * 支付结构行。
     */
    public static class PaymentMixRowVO {
        private String payType;
        private String payTypeName;
        private Long count = 0L;
        private BigDecimal amount = BigDecimal.ZERO;
        private BigDecimal rate = BigDecimal.ZERO;

        public String getPayType() { return payType; }
        public void setPayType(String payType) { this.payType = payType; }
        public String getPayTypeName() { return payTypeName; }
        public void setPayTypeName(String payTypeName) { this.payTypeName = payTypeName; }
        public Long getCount() { return count; }
        public void setCount(Long count) { this.count = count; }
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
        public BigDecimal getRate() { return rate; }
        public void setRate(BigDecimal rate) { this.rate = rate; }
    }

    /**
     * 下单转化漏斗。
     */
    public static class FunnelVO {
        private List<FunnelRowVO> rows = new ArrayList<>();

        public List<FunnelRowVO> getRows() { return rows; }
        public void setRows(List<FunnelRowVO> rows) { this.rows = rows; }
    }

    /**
     * 下单转化漏斗行。
     */
    public static class FunnelRowVO {
        private String stage;
        private String stageName;
        private Long count = 0L;
        private BigDecimal rate = BigDecimal.ZERO;

        public String getStage() { return stage; }
        public void setStage(String stage) { this.stage = stage; }
        public String getStageName() { return stageName; }
        public void setStageName(String stageName) { this.stageName = stageName; }
        public Long getCount() { return count; }
        public void setCount(Long count) { this.count = count; }
        public BigDecimal getRate() { return rate; }
        public void setRate(BigDecimal rate) { this.rate = rate; }
    }

    /**
     * 积分消费分析。
     */
    public static class PointsVO {
        private BigDecimal deductAmount = BigDecimal.ZERO;
        private Long usedPoints = 0L;
        private BigDecimal usageRate = BigDecimal.ZERO;
        private List<PointsSourceRowVO> sources = new ArrayList<>();

        public BigDecimal getDeductAmount() { return deductAmount; }
        public void setDeductAmount(BigDecimal deductAmount) { this.deductAmount = deductAmount; }
        public Long getUsedPoints() { return usedPoints; }
        public void setUsedPoints(Long usedPoints) { this.usedPoints = usedPoints; }
        public BigDecimal getUsageRate() { return usageRate; }
        public void setUsageRate(BigDecimal usageRate) { this.usageRate = usageRate; }
        public List<PointsSourceRowVO> getSources() { return sources; }
        public void setSources(List<PointsSourceRowVO> sources) { this.sources = sources; }
    }

    /**
     * 积分消费来源。
     */
    public static class PointsSourceRowVO {
        private Integer productType;
        private String productTypeName;
        private Long usedPoints = 0L;
        private BigDecimal deductAmount = BigDecimal.ZERO;
        private BigDecimal rate = BigDecimal.ZERO;

        public Integer getProductType() { return productType; }
        public void setProductType(Integer productType) { this.productType = productType; }
        public String getProductTypeName() { return productTypeName; }
        public void setProductTypeName(String productTypeName) { this.productTypeName = productTypeName; }
        public Long getUsedPoints() { return usedPoints; }
        public void setUsedPoints(Long usedPoints) { this.usedPoints = usedPoints; }
        public BigDecimal getDeductAmount() { return deductAmount; }
        public void setDeductAmount(BigDecimal deductAmount) { this.deductAmount = deductAmount; }
        public BigDecimal getRate() { return rate; }
        public void setRate(BigDecimal rate) { this.rate = rate; }
    }

    /**
     * 商品排行与风险摘要。
     */
    public static class RankingsVO {
        private List<ProductRankRowVO> topProducts = new ArrayList<>();
        private List<RiskRowVO> risks = new ArrayList<>();

        public List<ProductRankRowVO> getTopProducts() { return topProducts; }
        public void setTopProducts(List<ProductRankRowVO> topProducts) { this.topProducts = topProducts; }
        public List<RiskRowVO> getRisks() { return risks; }
        public void setRisks(List<RiskRowVO> risks) { this.risks = risks; }
    }

    /**
     * 商品贡献排行。
     */
    public static class ProductRankRowVO {
        private String productName;
        private Integer productType;
        private String productTypeName;
        private Long orderCount = 0L;
        private BigDecimal paidAmount = BigDecimal.ZERO;
        private BigDecimal pointsDeductAmount = BigDecimal.ZERO;

        public String getProductName() { return productName; }
        public void setProductName(String productName) { this.productName = productName; }
        public Integer getProductType() { return productType; }
        public void setProductType(Integer productType) { this.productType = productType; }
        public String getProductTypeName() { return productTypeName; }
        public void setProductTypeName(String productTypeName) { this.productTypeName = productTypeName; }
        public Long getOrderCount() { return orderCount; }
        public void setOrderCount(Long orderCount) { this.orderCount = orderCount; }
        public BigDecimal getPaidAmount() { return paidAmount; }
        public void setPaidAmount(BigDecimal paidAmount) { this.paidAmount = paidAmount; }
        public BigDecimal getPointsDeductAmount() { return pointsDeductAmount; }
        public void setPointsDeductAmount(BigDecimal pointsDeductAmount) { this.pointsDeductAmount = pointsDeductAmount; }
    }

    /**
     * 风险摘要。
     */
    public static class RiskRowVO {
        private String riskType;
        private String riskName;
        private String description;
        private Long count = 0L;

        public String getRiskType() { return riskType; }
        public void setRiskType(String riskType) { this.riskType = riskType; }
        public String getRiskName() { return riskName; }
        public void setRiskName(String riskName) { this.riskName = riskName; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public Long getCount() { return count; }
        public void setCount(Long count) { this.count = count; }
    }

    /**
     * 汇总聚合查询结果。
     */
    public static class SummaryAggregateVO extends SummaryVO {
    }

    /**
     * 漏斗聚合查询结果。
     */
    public static class FunnelAggregateVO {
        private Long createdCount = 0L;
        private Long paymentCreatedCount = 0L;
        private Long paidCount = 0L;

        public Long getCreatedCount() { return createdCount; }
        public void setCreatedCount(Long createdCount) { this.createdCount = createdCount; }
        public Long getPaymentCreatedCount() { return paymentCreatedCount; }
        public void setPaymentCreatedCount(Long paymentCreatedCount) { this.paymentCreatedCount = paymentCreatedCount; }
        public Long getPaidCount() { return paidCount; }
        public void setPaidCount(Long paidCount) { this.paidCount = paidCount; }
    }

    /**
     * 风险聚合查询结果。
     */
    public static class RiskAggregateVO {
        private Long timeoutPendingCount = 0L;
        private Long amountMismatchCount = 0L;
        private Long paymentFailedCount = 0L;

        public Long getTimeoutPendingCount() { return timeoutPendingCount; }
        public void setTimeoutPendingCount(Long timeoutPendingCount) { this.timeoutPendingCount = timeoutPendingCount; }
        public Long getAmountMismatchCount() { return amountMismatchCount; }
        public void setAmountMismatchCount(Long amountMismatchCount) { this.amountMismatchCount = amountMismatchCount; }
        public Long getPaymentFailedCount() { return paymentFailedCount; }
        public void setPaymentFailedCount(Long paymentFailedCount) { this.paymentFailedCount = paymentFailedCount; }
    }
}
