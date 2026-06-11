package com.backstage.system.domain.member;

import com.backstage.common.core.domain.entity.OSHBaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@TableName("osh_member_plan")
public class OshMemberPlan extends OSHBaseEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private String planCode;
    private String planName;
    private String memberType;
    private String periodType;
    private Integer durationMonths;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private String description;
    private Integer minPurchaseQuantity;
    private Integer maxPurchaseQuantity;
    private BigDecimal growthCoefficient;
    private String capPlanCode;
    private BigDecimal capRatio;
    private Integer sort;
    private Integer status;
    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private List<OshMemberBenefit> benefits;
    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private List<com.backstage.system.domain.member.vo.MemberPlanPriceTierVO> priceTiers;
    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private Integer effectiveMaxPurchaseQuantity;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getPlanCode() { return planCode; }
    public void setPlanCode(String planCode) { this.planCode = planCode; }
    public String getPlanName() { return planName; }
    public void setPlanName(String planName) { this.planName = planName; }
    public String getMemberType() { return memberType; }
    public void setMemberType(String memberType) { this.memberType = memberType; }
    public String getPeriodType() { return periodType; }
    public void setPeriodType(String periodType) { this.periodType = periodType; }
    public Integer getDurationMonths() { return durationMonths; }
    public void setDurationMonths(Integer durationMonths) { this.durationMonths = durationMonths; }
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
    public List<OshMemberBenefit> getBenefits() { return benefits; }
    public void setBenefits(List<OshMemberBenefit> benefits) { this.benefits = benefits; }
    public List<com.backstage.system.domain.member.vo.MemberPlanPriceTierVO> getPriceTiers() { return priceTiers; }
    public void setPriceTiers(List<com.backstage.system.domain.member.vo.MemberPlanPriceTierVO> priceTiers) { this.priceTiers = priceTiers; }
    public Integer getEffectiveMaxPurchaseQuantity() { return effectiveMaxPurchaseQuantity; }
    public void setEffectiveMaxPurchaseQuantity(Integer effectiveMaxPurchaseQuantity) { this.effectiveMaxPurchaseQuantity = effectiveMaxPurchaseQuantity; }
}
