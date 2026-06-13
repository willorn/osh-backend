package com.backstage.system.domain.openproject;

import com.backstage.common.core.domain.entity.OSHBaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 开源项目表
 */
@TableName("osh_open_project")
public class OshOpenProject extends OSHBaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 资源编号 */
    private String no;

    /** 项目名称 */
    private String projectName;

    /** 项目描述 */
    private String projectDesc;

    /** 项目链接（Gitee/GitHub） */
    private String projectUrl;

    /** 作者名称 */
    private String authorName;

    /** 封面图片 URL */
    private String projectCover;

    /** 状态：0-待审核，1-已通过，2-已拒绝 */
    private Integer status;

    /** 点击次数 */
    private Integer clickCount;

    /** 拒绝原因 */
    private String rejectReason;

    /** GitHub Star 数 */
    private Integer starCount;

    /** GitHub Fork 数 */
    private Integer forkCount;

    /** 最近一次提交时间（从 GitHub 同步） */
    private LocalDateTime lastCommitTime;

    /** 是否已归档：0-活跃，1-已归档 */
    private Byte isArchived;

    /** 最后一次从 GitHub 同步数据的时间 */
    private LocalDateTime lastSyncTime;

    private Long sourceId;
    private Long githubRepoId;
    private String githubOwner;
    private String githubRepoName;
    private String defaultBranch;
    private String language;
    private String licenseName;
    private String homepage;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNo() { return no; }
    public void setNo(String no) { this.no = no; }

    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }

    public String getProjectDesc() { return projectDesc; }
    public void setProjectDesc(String projectDesc) { this.projectDesc = projectDesc; }

    public String getProjectUrl() { return projectUrl; }
    public void setProjectUrl(String projectUrl) { this.projectUrl = projectUrl; }

    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }

    public String getProjectCover() { return projectCover; }
    public void setProjectCover(String projectCover) { this.projectCover = projectCover; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public Integer getClickCount() { return clickCount; }
    public void setClickCount(Integer clickCount) { this.clickCount = clickCount; }

    public String getRejectReason() { return rejectReason; }
    public void setRejectReason(String rejectReason) { this.rejectReason = rejectReason; }

    public Integer getStarCount() { return starCount; }
    public void setStarCount(Integer starCount) { this.starCount = starCount; }

    public Integer getForkCount() { return forkCount; }
    public void setForkCount(Integer forkCount) { this.forkCount = forkCount; }

    public LocalDateTime getLastCommitTime() { return lastCommitTime; }
    public void setLastCommitTime(LocalDateTime lastCommitTime) { this.lastCommitTime = lastCommitTime; }

    public Byte getIsArchived() { return isArchived; }
    public void setIsArchived(Byte isArchived) { this.isArchived = isArchived; }

    public LocalDateTime getLastSyncTime() { return lastSyncTime; }
    public void setLastSyncTime(LocalDateTime lastSyncTime) { this.lastSyncTime = lastSyncTime; }

    public Long getSourceId() { return sourceId; }
    public void setSourceId(Long sourceId) { this.sourceId = sourceId; }

    public Long getGithubRepoId() { return githubRepoId; }
    public void setGithubRepoId(Long githubRepoId) { this.githubRepoId = githubRepoId; }

    public String getGithubOwner() { return githubOwner; }
    public void setGithubOwner(String githubOwner) { this.githubOwner = githubOwner; }

    public String getGithubRepoName() { return githubRepoName; }
    public void setGithubRepoName(String githubRepoName) { this.githubRepoName = githubRepoName; }

    public String getDefaultBranch() { return defaultBranch; }
    public void setDefaultBranch(String defaultBranch) { this.defaultBranch = defaultBranch; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public String getLicenseName() { return licenseName; }
    public void setLicenseName(String licenseName) { this.licenseName = licenseName; }

    public String getHomepage() { return homepage; }
    public void setHomepage(String homepage) { this.homepage = homepage; }
}
