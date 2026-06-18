package com.backstage.system.domain.openproject.vo;

import com.backstage.system.domain.openproject.OshOpenProjectModuleMember;

import java.util.List;

public class OpenProjectModuleVO {
    private Long id;
    private Long projectId;
    private String moduleName;
    private String moduleDesc;
    private Integer sortOrder;
    private List<OshOpenProjectModuleMember> members;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public String getModuleName() { return moduleName; }
    public void setModuleName(String moduleName) { this.moduleName = moduleName; }
    public String getModuleDesc() { return moduleDesc; }
    public void setModuleDesc(String moduleDesc) { this.moduleDesc = moduleDesc; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public List<OshOpenProjectModuleMember> getMembers() { return members; }
    public void setMembers(List<OshOpenProjectModuleMember> members) { this.members = members; }
}
