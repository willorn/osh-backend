package com.backstage.system.request.tool;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 工具新增/修改请求
 */
@ApiModel(description = "工具新增/修改请求")
public class ToolSaveRequest {

    @ApiModelProperty(value = "工具ID，新增不传，修改必传", example = "10001")
    private Long id;

    @ApiModelProperty(value = "工具名称", required = true, example = "图片转PDF")
    private String toolName;

    @ApiModelProperty(value = "工具描述", example = "支持多张图片合成为PDF文件")
    private String description;

    @ApiModelProperty(value = "工具图标相对路径", example = "common/image/tool/202605/logo.png")
    private String logoUrl;

    @ApiModelProperty(value = "站内工具前端路由", example = "/tool/image-to-pdf")
    private String routePath;

    @ApiModelProperty(value = "GitHub地址", example = "https://github.com/example/tool")
    private String githubUrl;

    @ApiModelProperty(value = "状态：2-待审核，4-上架，6-下架", example = "2")
    private Integer status;

    @ApiModelProperty(value = "备注", example = "首批上线工具")
    private String remark;

    @ApiModelProperty(value = "资源类型：FREE,CASH_ONLY,CASH_POINT,VIP,SMALL_CLASS,INTERNAL", example = "FREE")
    private String resourceType;

    @ApiModelProperty(value = "标签名称列表，不存在的标签会自动创建", example = "[\"PDF工具\",\"图片工具\"]")
    private List<String> tags;

    @ApiModelProperty(value = "工具售卖套餐列表", example = "[{\"packageName\":\"10次包\",\"useCount\":10,\"price\":9.90}]")
    private List<ToolPackageSaveRequest> packages;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getToolName() {
        return toolName;
    }

    public void setToolName(String toolName) {
        this.toolName = StringUtils.trimToNull(toolName);
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = StringUtils.trimToNull(description);
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = StringUtils.trimToNull(logoUrl);
    }

    public String getRoutePath() {
        return routePath;
    }

    public void setRoutePath(String routePath) {
        this.routePath = StringUtils.trimToNull(routePath);
    }

    public String getGithubUrl() {
        return githubUrl;
    }

    public void setGithubUrl(String githubUrl) {
        this.githubUrl = StringUtils.trimToNull(githubUrl);
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = StringUtils.trimToNull(remark);
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = StringUtils.trimToNull(resourceType);
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        if (tags == null) {
            this.tags = null;
            return;
        }
        List<String> normalized = new ArrayList<>();
        for (String tag : tags) {
            String value = StringUtils.trimToNull(tag);
            if (value != null && !normalized.contains(value)) {
                normalized.add(value);
            }
        }
        this.tags = normalized;
    }

    public List<ToolPackageSaveRequest> getPackages() {
        return packages;
    }

    public void setPackages(List<ToolPackageSaveRequest> packages) {
        this.packages = packages;
    }
}
