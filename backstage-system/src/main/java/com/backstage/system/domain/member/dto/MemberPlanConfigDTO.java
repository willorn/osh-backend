package com.backstage.system.domain.member.dto;

import javax.validation.Valid;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

public class MemberPlanConfigDTO {
    @NotNull(message = "套餐ID不能为空")
    private Long id;
    @NotBlank(message = "套餐名称不能为空")
    private String planName;
    @DecimalMin(value = "0.01", message = "套餐价格必须大于0")
    private BigDecimal price;
    private BigDecimal originalPrice;
    private String description;
    @Min(value = 1, message = "最小购买数量不能小于1")
    private Integer minPurchaseQuantity;
    @Min(value = 1, message = "最大购买数量不能小于1")
    private Integer maxPurchaseQuantity;
    @DecimalMin(value = "0.01", message = "增长系数必须大于0")
    private BigDecimal growthCoefficient;
    private String capPlanCode;
    @DecimalMin(value = "0.01", message = "封顶比例必须大于0")
    private BigDecimal capRatio;
    private Integer sort;
    private Integer status;
    @Valid
    private List<MemberBenefitSaveDTO> benefits;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getPlanName() { return planName; }
    public void setPlanName(String planName) { this.planName = planName; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public BigDecimal getOriginalPrice() { return originalPrice; }
    public void setOriginalPrice(BigDecimal originalPrice) { this.originalPrice = originalPrice; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Integer getMinPurchaseQuantity() { return minPurchaseQuantity; }
    public void setMinPurchaseQuantity(Integer minPurchaseQuantity) { this.minPurchaseQuantity = minPurchaseQuantity; }
    public Integer getMaxPurchaseQuantity() { return maxPurchaseQuantity; }
    public void setMaxPurchaseQuantity(Integer maxPurchaseQuantity) { this.maxPurchaseQuantity = maxPurchaseQuantity; }
    public BigDecimal getGrowthCoefficient() { return growthCoefficient; }
    public void setGrowthCoefficient(BigDecimal growthCoefficient) { this.growthCoefficient = growthCoefficient; }
    public String getCapPlanCode() { return capPlanCode; }
    public void setCapPlanCode(String capPlanCode) { this.capPlanCode = capPlanCode; }
    public BigDecimal getCapRatio() { return capRatio; }
    public void setCapRatio(BigDecimal capRatio) { this.capRatio = capRatio; }
    public Integer getSort() { return sort; }
    public void setSort(Integer sort) { this.sort = sort; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public List<MemberBenefitSaveDTO> getBenefits() { return benefits; }
    public void setBenefits(List<MemberBenefitSaveDTO> benefits) { this.benefits = benefits; }
}
