package com.backstage.system.domain.openproject;

import com.backstage.common.core.domain.entity.OSHBaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;

@TableName("osh_open_project_module_member")
public class OshOpenProjectModuleMember extends OSHBaseEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long moduleId;
    private Long projectId;
    private Long contributorId;
    private String githubAccount;
    private String wechatName;
    private String memberRole;
    private Integer sortOrder;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getModuleId() { return moduleId; }
    public void setModuleId(Long moduleId) { this.moduleId = moduleId; }
    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public Long getContributorId() { return contributorId; }
    public void setContributorId(Long contributorId) { this.contributorId = contributorId; }
    public String getGithubAccount() { return githubAccount; }
    public void setGithubAccount(String githubAccount) { this.githubAccount = githubAccount; }
    public String getWechatName() { return wechatName; }
    public void setWechatName(String wechatName) { this.wechatName = wechatName; }
    public String getMemberRole() { return memberRole; }
    public void setMemberRole(String memberRole) { this.memberRole = memberRole; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
}
