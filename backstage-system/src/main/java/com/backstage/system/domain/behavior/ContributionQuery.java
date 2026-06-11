package com.backstage.system.domain.behavior;

import java.math.BigDecimal;

public class ContributionQuery {
    private Long contributorUserId;
    private String resourceType;
    private String startTime;
    private String endTime;
    private Integer pageNum = 1;
    private Integer pageSize = 20;
    private BigDecimal baseCoefficient = BigDecimal.ONE;
    private BigDecimal cashRevenueCoefficient = BigDecimal.ONE;
    private BigDecimal pointRevenueCoefficient = new BigDecimal("0.01");
    private BigDecimal purchaseCoefficient = new BigDecimal("3");
    private BigDecimal viewCoefficient = new BigDecimal("0.05");
    private BigDecimal collectCoefficient = new BigDecimal("0.5");
    private BigDecimal likeCoefficient = new BigDecimal("0.3");
    private BigDecimal useCoefficient = new BigDecimal("0.2");

    public Long getContributorUserId() {
        return contributorUserId;
    }

    public void setContributorUserId(Long contributorUserId) {
        this.contributorUserId = contributorUserId;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public Integer getPageNum() {
        return pageNum;
    }

    public void setPageNum(Integer pageNum) {
        this.pageNum = pageNum;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    public BigDecimal getBaseCoefficient() { return baseCoefficient; }
    public void setBaseCoefficient(BigDecimal baseCoefficient) { this.baseCoefficient = normalize(baseCoefficient); }
    public BigDecimal getCashRevenueCoefficient() { return cashRevenueCoefficient; }
    public void setCashRevenueCoefficient(BigDecimal cashRevenueCoefficient) { this.cashRevenueCoefficient = normalize(cashRevenueCoefficient); }
    public BigDecimal getPointRevenueCoefficient() { return pointRevenueCoefficient; }
    public void setPointRevenueCoefficient(BigDecimal pointRevenueCoefficient) { this.pointRevenueCoefficient = normalize(pointRevenueCoefficient); }
    public BigDecimal getPurchaseCoefficient() { return purchaseCoefficient; }
    public void setPurchaseCoefficient(BigDecimal purchaseCoefficient) { this.purchaseCoefficient = normalize(purchaseCoefficient); }
    public BigDecimal getViewCoefficient() { return viewCoefficient; }
    public void setViewCoefficient(BigDecimal viewCoefficient) { this.viewCoefficient = normalize(viewCoefficient); }
    public BigDecimal getCollectCoefficient() { return collectCoefficient; }
    public void setCollectCoefficient(BigDecimal collectCoefficient) { this.collectCoefficient = normalize(collectCoefficient); }
    public BigDecimal getLikeCoefficient() { return likeCoefficient; }
    public void setLikeCoefficient(BigDecimal likeCoefficient) { this.likeCoefficient = normalize(likeCoefficient); }
    public BigDecimal getUseCoefficient() { return useCoefficient; }
    public void setUseCoefficient(BigDecimal useCoefficient) { this.useCoefficient = normalize(useCoefficient); }

    public void applyCoefficients(ContributionCoefficientConfig config) {
        if (config == null) {
            return;
        }
        setBaseCoefficient(config.getBase());
        setCashRevenueCoefficient(config.getCashRevenue());
        setPointRevenueCoefficient(config.getPointRevenue());
        setPurchaseCoefficient(config.getPurchase());
        setViewCoefficient(config.getView());
        setCollectCoefficient(config.getCollect());
        setLikeCoefficient(config.getLike());
        setUseCoefficient(config.getUse());
    }

    private BigDecimal normalize(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
