package com.backstage.system.domain.openproject;

import com.backstage.common.core.domain.entity.OSHBaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.time.LocalDateTime;

@TableName("osh_open_project_source")
public class OshOpenProjectSource extends OSHBaseEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private String sourceName;
    private String githubOwner;
    private String githubUrl;
    private String sourceType;
    private String accessToken;
    private Integer enabled;
    private Integer syncStatus;
    private LocalDateTime lastSyncTime;
    private String lastSyncMessage;
    private Integer repoCount;
    private String remark;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSourceName() { return sourceName; }
    public void setSourceName(String sourceName) { this.sourceName = sourceName; }
    public String getGithubOwner() { return githubOwner; }
    public void setGithubOwner(String githubOwner) { this.githubOwner = githubOwner; }
    public String getGithubUrl() { return githubUrl; }
    public void setGithubUrl(String githubUrl) { this.githubUrl = githubUrl; }
    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
    public Integer getEnabled() { return enabled; }
    public void setEnabled(Integer enabled) { this.enabled = enabled; }
    public Integer getSyncStatus() { return syncStatus; }
    public void setSyncStatus(Integer syncStatus) { this.syncStatus = syncStatus; }
    public LocalDateTime getLastSyncTime() { return lastSyncTime; }
    public void setLastSyncTime(LocalDateTime lastSyncTime) { this.lastSyncTime = lastSyncTime; }
    public String getLastSyncMessage() { return lastSyncMessage; }
    public void setLastSyncMessage(String lastSyncMessage) { this.lastSyncMessage = lastSyncMessage; }
    public Integer getRepoCount() { return repoCount; }
    public void setRepoCount(Integer repoCount) { this.repoCount = repoCount; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
