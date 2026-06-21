package com.backstage.system.domain.openproject.dto;

public class OpenProjectTechComponentLibraryDTO {
    private Long id;
    private String componentName;
    private String componentDesc;
    private String officialUrl;
    private Integer sortOrder;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getComponentName() { return componentName; }
    public void setComponentName(String componentName) { this.componentName = componentName; }
    public String getComponentDesc() { return componentDesc; }
    public void setComponentDesc(String componentDesc) { this.componentDesc = componentDesc; }
    public String getOfficialUrl() { return officialUrl; }
    public void setOfficialUrl(String officialUrl) { this.officialUrl = officialUrl; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
}
