package com.backstage.system.domain.member.dto;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

public class MemberCheckoutDTO {
    @NotNull(message = "套餐ID不能为空")
    private Long planId;
    @Min(value = 1, message = "购买数量不能小于1")
    private Integer quantity;
    private String channel;
    private Boolean usePoints;

    public Long getPlanId() { return planId; }
    public void setPlanId(Long planId) { this.planId = planId; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }
    public Boolean getUsePoints() { return usePoints; }
    public void setUsePoints(Boolean usePoints) { this.usePoints = usePoints; }
}
