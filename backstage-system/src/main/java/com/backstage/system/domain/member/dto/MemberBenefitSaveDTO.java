package com.backstage.system.domain.member.dto;

import javax.validation.constraints.NotBlank;

public class MemberBenefitSaveDTO {
    private Long id;
    @NotBlank(message = "权益标题不能为空")
    private String benefitTitle;
    private String benefitDescription;
    private String icon;
    private Integer sort;
    private Integer status;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
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
