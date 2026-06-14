package com.backstage.system.domain.behavior;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("osh_resource_contribution")
public class OshResourceContribution {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long contributorUserId;
    private String contributorUsername;
    private Integer contributorRoleLevel;
    private String resourceType;
    private Long resourceId;
    private String resourceNo;
    private String resourceName;
    private Long sourceEventId;
    private Integer status;
    private LocalDateTime createTime;
    private Long createBy;
    private LocalDateTime updateTime;
    private Long updateBy;
    private Integer deleteFlag;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getContributorUserId() { return contributorUserId; }
    public void setContributorUserId(Long contributorUserId) { this.contributorUserId = contributorUserId; }
    public String getContributorUsername() { return contributorUsername; }
    public void setContributorUsername(String contributorUsername) { this.contributorUsername = contributorUsername; }
    public Integer getContributorRoleLevel() { return contributorRoleLevel; }
    public void setContributorRoleLevel(Integer contributorRoleLevel) { this.contributorRoleLevel = contributorRoleLevel; }
    public String getResourceType() { return resourceType; }
    public void setResourceType(String resourceType) { this.resourceType = resourceType; }
    public Long getResourceId() { return resourceId; }
    public void setResourceId(Long resourceId) { this.resourceId = resourceId; }
    public String getResourceNo() { return resourceNo; }
    public void setResourceNo(String resourceNo) { this.resourceNo = resourceNo; }
    public String getResourceName() { return resourceName; }
    public void setResourceName(String resourceName) { this.resourceName = resourceName; }
    public Long getSourceEventId() { return sourceEventId; }
    public void setSourceEventId(Long sourceEventId) { this.sourceEventId = sourceEventId; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public Long getCreateBy() { return createBy; }
    public void setCreateBy(Long createBy) { this.createBy = createBy; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
    public Long getUpdateBy() { return updateBy; }
    public void setUpdateBy(Long updateBy) { this.updateBy = updateBy; }
    public Integer getDeleteFlag() { return deleteFlag; }
    public void setDeleteFlag(Integer deleteFlag) { this.deleteFlag = deleteFlag; }
}
