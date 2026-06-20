package com.backstage.system.domain.homepage.es;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class HomepageMemberPlanEsDocument {

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
    private Byte deleteFlag;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private List<BenefitDocument> benefits;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPlanCode() {
        return planCode;
    }

    public void setPlanCode(String planCode) {
        this.planCode = planCode;
    }

    public String getPlanName() {
        return planName;
    }

    public void setPlanName(String planName) {
        this.planName = planName;
    }

    public String getMemberType() {
        return memberType;
    }

    public void setMemberType(String memberType) {
        this.memberType = memberType;
    }

    public String getPeriodType() {
        return periodType;
    }

    public void setPeriodType(String periodType) {
        this.periodType = periodType;
    }

    public Integer getDurationMonths() {
        return durationMonths;
    }

    public void setDurationMonths(Integer durationMonths) {
        this.durationMonths = durationMonths;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getOriginalPrice() {
        return originalPrice;
    }

    public void setOriginalPrice(BigDecimal originalPrice) {
        this.originalPrice = originalPrice;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getMinPurchaseQuantity() {
        return minPurchaseQuantity;
    }

    public void setMinPurchaseQuantity(Integer minPurchaseQuantity) {
        this.minPurchaseQuantity = minPurchaseQuantity;
    }

    public Integer getMaxPurchaseQuantity() {
        return maxPurchaseQuantity;
    }

    public void setMaxPurchaseQuantity(Integer maxPurchaseQuantity) {
        this.maxPurchaseQuantity = maxPurchaseQuantity;
    }

    public BigDecimal getGrowthCoefficient() {
        return growthCoefficient;
    }

    public void setGrowthCoefficient(BigDecimal growthCoefficient) {
        this.growthCoefficient = growthCoefficient;
    }

    public String getCapPlanCode() {
        return capPlanCode;
    }

    public void setCapPlanCode(String capPlanCode) {
        this.capPlanCode = capPlanCode;
    }

    public BigDecimal getCapRatio() {
        return capRatio;
    }

    public void setCapRatio(BigDecimal capRatio) {
        this.capRatio = capRatio;
    }

    public Integer getSort() {
        return sort;
    }

    public void setSort(Integer sort) {
        this.sort = sort;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Byte getDeleteFlag() {
        return deleteFlag;
    }

    public void setDeleteFlag(Byte deleteFlag) {
        this.deleteFlag = deleteFlag;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public List<BenefitDocument> getBenefits() {
        return benefits;
    }

    public void setBenefits(List<BenefitDocument> benefits) {
        this.benefits = benefits;
    }

    public static class BenefitDocument {
        private Long id;
        private Long planId;
        private String benefitTitle;
        private String benefitDescription;
        private String icon;
        private Integer sort;
        private Integer status;
        private Byte deleteFlag;
        private LocalDateTime createTime;
        private LocalDateTime updateTime;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public Long getPlanId() {
            return planId;
        }

        public void setPlanId(Long planId) {
            this.planId = planId;
        }

        public String getBenefitTitle() {
            return benefitTitle;
        }

        public void setBenefitTitle(String benefitTitle) {
            this.benefitTitle = benefitTitle;
        }

        public String getBenefitDescription() {
            return benefitDescription;
        }

        public void setBenefitDescription(String benefitDescription) {
            this.benefitDescription = benefitDescription;
        }

        public String getIcon() {
            return icon;
        }

        public void setIcon(String icon) {
            this.icon = icon;
        }

        public Integer getSort() {
            return sort;
        }

        public void setSort(Integer sort) {
            this.sort = sort;
        }

        public Integer getStatus() {
            return status;
        }

        public void setStatus(Integer status) {
            this.status = status;
        }

        public Byte getDeleteFlag() {
            return deleteFlag;
        }

        public void setDeleteFlag(Byte deleteFlag) {
            this.deleteFlag = deleteFlag;
        }

        public LocalDateTime getCreateTime() {
            return createTime;
        }

        public void setCreateTime(LocalDateTime createTime) {
            this.createTime = createTime;
        }

        public LocalDateTime getUpdateTime() {
            return updateTime;
        }

        public void setUpdateTime(LocalDateTime updateTime) {
            this.updateTime = updateTime;
        }
    }
}
