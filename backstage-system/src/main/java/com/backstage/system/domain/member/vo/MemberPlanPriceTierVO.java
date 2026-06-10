package com.backstage.system.domain.member.vo;

import java.math.BigDecimal;

public class MemberPlanPriceTierVO {
    private Integer quantity;
    private Integer durationMonths;
    private BigDecimal price;
    private BigDecimal originalPrice;

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public Integer getDurationMonths() { return durationMonths; }
    public void setDurationMonths(Integer durationMonths) { this.durationMonths = durationMonths; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public BigDecimal getOriginalPrice() { return originalPrice; }
    public void setOriginalPrice(BigDecimal originalPrice) { this.originalPrice = originalPrice; }
}
