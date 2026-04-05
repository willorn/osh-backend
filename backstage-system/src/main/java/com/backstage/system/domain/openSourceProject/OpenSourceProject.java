package com.backstage.system.domain.openSourceProject;

import java.util.Date;


/**
 * Created with IntelliJ IDEA.
 * Description:
 * User: jayTatum
 */
public class OpenSourceProject  {
    private Long id;
    private String projectName;
    private String projectCover;
    private String projectUrl;
    private String projectDesc;
    private String tags;//技术栈标签
    private String techStack;  //项目标签
    private String authorName;
    private String authorUrl;
    private Integer status;  //状态 1=启用，0=禁用
    private String createdBy;
    private String updateBy;
    private Date creationTime;
    private Date updateTime;
    //0未删除，1已删除
    private Integer isDeleted;

    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public String getProjectName() {
        return projectName;
    }
    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }
    public String getProjectCover() {
        return projectCover;
    }
    public void setProjectCover(String projectCover) {
        this.projectCover = projectCover;
    }
    public String getProjectUrl() {
        return projectUrl;
    }
    public void setProjectUrl(String projectUrl) {
        this.projectUrl = projectUrl;
    }
    public String getProjectDesc() {return projectDesc;}
    public void setProjectDesc(String projectDesc) {this.projectDesc = projectDesc;}
    public String getTags() {return tags;}
    public void setTags(String tags) {this.tags = tags;}
    public String getTechStack() {return techStack;}
    public void setTechStack(String techStack) {this.techStack = techStack;}
    public String getAuthorName() {return authorName;}
    public void setAuthorName(String authorName) {this.authorName = authorName;}
    public String getAuthorUrl() {return authorUrl;}
    public void setAuthorUrl(String authorUrl) {this.authorUrl = authorUrl;}

    public Integer getStatus() {return status;}
    public void setStatus(Integer status) {this.status = status;}
    public String getCreatedBy() {return createdBy;}
    public void setCreatedBy(String createdBy) {this.createdBy = createdBy;}
    public String getUpdateBy() {return updateBy;}
    public void setUpdateBy(String updateBy) {this.updateBy = updateBy;}

    public Date getCreationTime() {return creationTime;}
    public void setCreationTime(Date creationTime) {this.creationTime = creationTime;}


    public Date getUpdateTime() {return updateTime;}
    public void setUpdateTime(Date updateTime) {this.updateTime = updateTime;}

    public Integer getIsDeleted() {return isDeleted;}
    public void setIsDeleted(Integer isDeleted) {this.isDeleted = isDeleted;}

    @Override
    public String toString(){
        return "OpenSourceProject{" +
                "id=" + id +
                ", projectName='" + projectName + '\'' +
                ", projectCover='" + projectCover + '\'' +
                ", projectUrl='" + projectUrl + '\'' +
                ", projectDesc='" + projectDesc + '\'' +
                ", tags='" + tags + '\'' +
                ", techStack='" + techStack + '\'' +
                ", authorName='" + authorName + '\'' +
                ", authorUrl='" + authorUrl + '\'' +
                ", status=" + status +
                ", createdBy='" + createdBy + '\'' +
                ", creationTime=" + creationTime +
                ", updateBy='" + updateBy + '\'' +
                ", updateTime=" + updateTime +
                ", isDeleted=" + isDeleted +
                '}';
    }










}
