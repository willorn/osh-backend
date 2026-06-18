package com.backstage.system.domain.openproject.dto;

import java.util.List;

public class OpenProjectModuleDTO {
    private Long id;
    private String moduleName;
    private String moduleDesc;
    private Integer sortOrder;
    private List<OpenProjectModuleMemberDTO> members;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getModuleName() { return moduleName; }
    public void setModuleName(String moduleName) { this.moduleName = moduleName; }
    public String getModuleDesc() { return moduleDesc; }
    public void setModuleDesc(String moduleDesc) { this.moduleDesc = moduleDesc; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public List<OpenProjectModuleMemberDTO> getMembers() { return members; }
    public void setMembers(List<OpenProjectModuleMemberDTO> members) { this.members = members; }
}
