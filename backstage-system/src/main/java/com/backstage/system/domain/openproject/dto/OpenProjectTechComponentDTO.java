package com.backstage.system.domain.openproject.dto;

public class OpenProjectTechComponentDTO {
    private Long componentId;
    private String componentName;
    private String componentCode;
    private String componentDesc;
    private String officialUrl;
    private Integer sortOrder;

    public Long getComponentId() { return componentId; }
    public void setComponentId(Long componentId) { this.componentId = componentId; }
    public String getComponentName() { return componentName; }
    public void setComponentName(String componentName) { this.componentName = componentName; }
    public String getComponentCode() { return componentCode; }
    public void setComponentCode(String componentCode) { this.componentCode = componentCode; }
    public String getComponentDesc() { return componentDesc; }
    public void setComponentDesc(String componentDesc) { this.componentDesc = componentDesc; }
    public String getOfficialUrl() { return officialUrl; }
    public void setOfficialUrl(String officialUrl) { this.officialUrl = officialUrl; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
}
