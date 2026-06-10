package com.backstage.system.domain.member.dto;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

public class MemberPricingRuleDTO {
    @NotNull(message = "套餐ID不能为空")
    private Long id;
    @DecimalMin(value = "0.01", message = "增长系数必须大于0")
    private BigDecimal growthCoefficient;
    private String capPlanCode;
    @DecimalMin(value = "0.01", message = "封顶比例必须大于0")
    private BigDecimal capRatio;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public BigDecimal getGrowthCoefficient() { return growthCoefficient; }
    public void setGrowthCoefficient(BigDecimal growthCoefficient) { this.growthCoefficient = growthCoefficient; }
    public String getCapPlanCode() { return capPlanCode; }
    public void setCapPlanCode(String capPlanCode) { this.capPlanCode = capPlanCode; }
    public BigDecimal getCapRatio() { return capRatio; }
    public void setCapRatio(BigDecimal capRatio) { this.capRatio = capRatio; }
}
