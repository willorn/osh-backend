package com.backstage.system.domain.site;

import com.backstage.common.core.domain.entity.OSHBaseEntity;
import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.fasterxml.jackson.annotation.JsonFormat;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 内部网站信息对象 osh_site_info
 *
 * @author backstage
 */
@TableName(value = "osh_site_info", autoResultMap = true)
public class OshSiteInfo extends OSHBaseEntity implements Serializable {

    private static final long serialVersionUID = 4936551355109148940L;

    /**
     * 网站编号（主键）
     */
    @TableId(type = IdType.AUTO)
    @TableField(value = "id", updateStrategy = FieldStrategy.NEVER)
    private Long id;

    /**
     * 网站名称
     */
    @NotBlank(message = "请输入网站名称")
    @TableField(value = "site_name", updateStrategy = FieldStrategy.NOT_EMPTY)
    private String siteName;

    /**
     * 网站封面地址
     */
    @TableField(value = "cover", updateStrategy = FieldStrategy.NOT_EMPTY)
    private String cover;

    /**
     * 网站访问路径
     */
    @NotBlank(message = "请输入网站访问路径")
    @TableField(value = "site_url", updateStrategy = FieldStrategy.NOT_EMPTY)
    private String siteUrl;

    /**
     * 网站描述信息
     */
    @TableField(value = "description", updateStrategy = FieldStrategy.NOT_EMPTY)
    private String description;

    /**
     * 标签（多个标签用逗号分隔）
     */
    @TableField(exist = false)
    private List<OshSiteTag> tagList;

    /**
     * 状态
     */
    @TableField(value = "status", updateStrategy = FieldStrategy.NOT_NULL)
    private Integer status;

    /**
     * 网站类型：demo=演示站点
     */
    @TableField(value = "site_type", updateStrategy = FieldStrategy.NOT_EMPTY)
    private String siteType;

    /**
     * 网站类型名称（非数据库字段，由枚举映射）
     */
    @TableField(exist = false)
    private String siteTypeName;

    /**
     * 站点配置 JSON（按类型约定结构）
     */
    @TableField(value = "site_config", updateStrategy = FieldStrategy.NOT_EMPTY, typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> siteConfig;

    /**
     * 最后检查时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(value = "last_check_time")
    private Date lastCheckTime;

    /**
     * 最后检查状态
     */
    @TableField(value = "last_check_status")
    private Integer lastCheckStatus;

    /**
     * 最后检查状态名称
     */
    @TableField(exist = false)
    private String lastCheckStatusName;

    /**
     * 维护者
     */
    @TableField(exist = false)
    private List<OshSiteMaintainer> maintainers;

    /**
     * 维护者用户ID
     */
    @TableField(exist = false)
    private List<String> maintainerUserIds;

    /**
     * 相关资源（多个资源用逗号分隔）
     * 1: 资源类型 {@link SiteResourceType#getType()}
     * 2: 资源主键id
     */
    @TableField(exist = false)
    private List<List<String>> relatedResources;

    @TableField(exist = false)
    private List<OshSiteResourceRelation> resources;

    public List<String> getMaintainerUserIds() {
        return maintainerUserIds;
    }

    public void setMaintainerUserIds(List<String> maintainerUserIds) {
        this.maintainerUserIds = maintainerUserIds;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSiteName() {
        return siteName;
    }

    public void setSiteName(String siteName) {
        this.siteName = siteName;
    }

    public String getCover() {
        return cover;
    }

    public void setCover(String cover) {
        this.cover = cover;
    }

    public String getSiteUrl() {
        return siteUrl;
    }

    public void setSiteUrl(String siteUrl) {
        this.siteUrl = siteUrl;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<OshSiteTag> getTagList() {
        return tagList;
    }

    public void setTagList(List<OshSiteTag> tagList) {
        this.tagList = tagList;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getSiteType() {
        return siteType;
    }

    public void setSiteType(String siteType) {
        this.siteType = siteType;
    }

    public String getSiteTypeName() {
        return siteTypeName;
    }

    public void setSiteTypeName(String siteTypeName) {
        this.siteTypeName = siteTypeName;
    }

    public Map<String, Object> getSiteConfig() {
        return siteConfig;
    }

    public void setSiteConfig(Map<String, Object> siteConfig) {
        this.siteConfig = siteConfig;
    }

    public Date getLastCheckTime() {
        return lastCheckTime;
    }

    public void setLastCheckTime(Date lastCheckTime) {
        this.lastCheckTime = lastCheckTime;
    }

    public List<OshSiteMaintainer> getMaintainers() {
        return maintainers;
    }

    public void setMaintainers(List<OshSiteMaintainer> maintainers) {
        this.maintainers = maintainers;
    }

    public Integer getLastCheckStatus() {
        return lastCheckStatus;
    }

    public void setLastCheckStatus(Integer lastCheckStatus) {
        this.lastCheckStatus = lastCheckStatus;
    }

    public String getLastCheckStatusName() {
        return lastCheckStatusName;
    }

    public void setLastCheckStatusName(String lastCheckStatusName) {
        this.lastCheckStatusName = lastCheckStatusName;
    }

    public List<List<String>> getRelatedResources() {
        return relatedResources;
    }

    public void setRelatedResources(List<List<String>> relatedResources) {
        this.relatedResources = relatedResources;
    }

    public List<OshSiteResourceRelation> getResources() {
        return resources;
    }

    public void setResources(List<OshSiteResourceRelation> resources) {
        this.resources = resources;
    }
}
