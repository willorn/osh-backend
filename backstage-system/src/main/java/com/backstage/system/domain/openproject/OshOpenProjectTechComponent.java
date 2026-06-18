package com.backstage.system.domain.openproject;

import com.backstage.common.core.domain.entity.OSHBaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;

@TableName("osh_open_project_tech_component")
public class OshOpenProjectTechComponent extends OSHBaseEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private String componentName;
    private String componentCode;
    private String componentDesc;
    private String officialUrl;
    private Integer sortOrder;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getComponentName() { return componentName; }
    public void setComponentName(String componentName) { this.componentName = componentName; }
    public String getComponentCode() { return componentCode; }
    public void setComponentCode(String componentCode) { this.componentCode = componentCode; }
    public String getComponentDesc() { return componentDesc; }
    public void setComponentDesc(String componentDesc) { this.componentDesc = componentDesc; }
    public String getOfficialUrl() { return officialUrl; }
    public void setOfficialUrl(String officialUrl) { this.officialUrl = officialUrl; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
}
