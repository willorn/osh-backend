package com.backstage.system.domain.site;

import com.backstage.common.core.domain.entity.OSHBaseEntity;
import com.baomidou.mybatisplus.annotation.*;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 网站资源关联对象 osh_site_resource_relation
 *
 * @author backstage
 */
@TableName("osh_site_resource_relation")
public class OshSiteResourceRelation extends OSHBaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    @TableField(value = "id", updateStrategy = FieldStrategy.NEVER)
    private Long id;

    /**
     * 关联网站ID
     */
    @NotNull(message = "网站ID不能为空")
    @TableField(value = "site_id", updateStrategy = FieldStrategy.NOT_NULL)
    private Long siteId;

    /**
     * 关联资源ID
     */
    @NotNull(message = "资源ID不能为空")
    @TableField(value = "resource_id", updateStrategy = FieldStrategy.NOT_NULL)
    private Long resourceId;

    /**
     * 关联资源类型
     */
    @NotBlank(message = "资源类型不能为空")
    @TableField(value = "resource_type", updateStrategy = FieldStrategy.NOT_EMPTY)
    private String resourceType;

    /**
     * 跳转地址
     */
    @TableField(exist = false)
    private String jumpingUrl;

    /**
     * 资源名称
     */
    @TableField(exist = false)
    private String resourceName;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSiteId() {
        return siteId;
    }

    public void setSiteId(Long siteId) {
        this.siteId = siteId;
    }

    public Long getResourceId() {
        return resourceId;
    }

    public void setResourceId(Long resourceId) {
        this.resourceId = resourceId;
    }

    public String getResourceType() {
        return resourceType;
    }

    public String getJumpingUrl() {
        return jumpingUrl;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public void setJumpingUrl(String jumpingUrl) {
        this.jumpingUrl = jumpingUrl;
    }

    public String getResourceName() {
        return resourceName;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }
}
