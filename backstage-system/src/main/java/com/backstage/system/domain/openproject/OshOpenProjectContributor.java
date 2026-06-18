package com.backstage.system.domain.openproject;

import com.backstage.common.core.domain.entity.OSHBaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;

@TableName("osh_open_project_contributor")
public class OshOpenProjectContributor extends OSHBaseEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long projectId;
    private String githubAccount;
    private String wechatName;
    private String contributorType;
    private Integer contributions;
    private String avatarUrl;
    private String profileUrl;
    private String source;
    private Integer editable;
    private Integer sortOrder;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public String getGithubAccount() { return githubAccount; }
    public void setGithubAccount(String githubAccount) { this.githubAccount = githubAccount; }
    public String getWechatName() { return wechatName; }
    public void setWechatName(String wechatName) { this.wechatName = wechatName; }
    public String getContributorType() { return contributorType; }
    public void setContributorType(String contributorType) { this.contributorType = contributorType; }
    public Integer getContributions() { return contributions; }
    public void setContributions(Integer contributions) { this.contributions = contributions; }
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    public String getProfileUrl() { return profileUrl; }
    public void setProfileUrl(String profileUrl) { this.profileUrl = profileUrl; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public Integer getEditable() { return editable; }
    public void setEditable(Integer editable) { this.editable = editable; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
}
