package com.backstage.system.domain.member;

import com.backstage.common.core.domain.entity.OSHBaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;

@TableName("osh_member_benefit")
public class OshMemberBenefit extends OSHBaseEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long planId;
    private String benefitTitle;
    private String benefitDescription;
    private String icon;
    private Integer sort;
    private Integer status;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getPlanId() { return planId; }
    public void setPlanId(Long planId) { this.planId = planId; }
    public String getBenefitTitle() { return benefitTitle; }
    public void setBenefitTitle(String benefitTitle) { this.benefitTitle = benefitTitle; }
    public String getBenefitDescription() { return benefitDescription; }
    public void setBenefitDescription(String benefitDescription) { this.benefitDescription = benefitDescription; }
    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }
    public Integer getSort() { return sort; }
    public void setSort(Integer sort) { this.sort = sort; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
}
