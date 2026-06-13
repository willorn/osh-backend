package com.backstage.system.domain.behavior;

import java.math.BigDecimal;

public class ContributionCoefficientConfig {
    private BigDecimal base = new BigDecimal("1");
    private BigDecimal cashRevenue = new BigDecimal("1");
    private BigDecimal pointRevenue = new BigDecimal("0.01");
    private BigDecimal purchase = new BigDecimal("3");
    private BigDecimal view = new BigDecimal("0.05");
    private BigDecimal collect = new BigDecimal("0.5");
    private BigDecimal like = new BigDecimal("0.3");
    private BigDecimal use = new BigDecimal("0.2");

    public BigDecimal getBase() { return base; }
    public void setBase(BigDecimal base) { this.base = normalize(base); }
    public BigDecimal getCashRevenue() { return cashRevenue; }
    public void setCashRevenue(BigDecimal cashRevenue) { this.cashRevenue = normalize(cashRevenue); }
    public BigDecimal getPointRevenue() { return pointRevenue; }
    public void setPointRevenue(BigDecimal pointRevenue) { this.pointRevenue = normalize(pointRevenue); }
    public BigDecimal getPurchase() { return purchase; }
    public void setPurchase(BigDecimal purchase) { this.purchase = normalize(purchase); }
    public BigDecimal getView() { return view; }
    public void setView(BigDecimal view) { this.view = normalize(view); }
    public BigDecimal getCollect() { return collect; }
    public void setCollect(BigDecimal collect) { this.collect = normalize(collect); }
    public BigDecimal getLike() { return like; }
    public void setLike(BigDecimal like) { this.like = normalize(like); }
    public BigDecimal getUse() { return use; }
    public void setUse(BigDecimal use) { this.use = normalize(use); }

    private BigDecimal normalize(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
