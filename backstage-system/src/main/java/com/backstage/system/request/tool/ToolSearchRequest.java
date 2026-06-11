package com.backstage.system.request.tool;

import com.backstage.common.request.PageRequest;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

/**
 * 工具搜索请求
 */
@ApiModel(description = "工具搜索请求")
public class ToolSearchRequest extends PageRequest {

    @ApiModelProperty(value = "工具ID，传入时精确查询单个工具", example = "10001")
    private Long toolId;

    @ApiModelProperty(value = "标签ID列表", example = "[1,2]")
    private List<Long> tags;

    @ApiModelProperty(value = "搜索关键字，匹配工具名称、描述、标签", example = "图片转PDF")
    private String keyword;

    @ApiModelProperty(value = "工具编号，传入时优先按编号精确查询", example = "tlAb12Cd")
    private String no;

    @ApiModelProperty(value = "资源类型：FREE,CASH_POINT,VIP,SMALL_CLASS,INTERNAL", example = "FREE")
    private String resourceType;

    @ApiModelProperty(value = "是否只看我收藏的工具", example = "false")
    private Boolean isFollowing;

    @ApiModelProperty(value = "收藏筛选：1-只查询已收藏", example = "1")
    private Integer collectionFlag;

    public Long getToolId() {
        return toolId;
    }

    public void setToolId(Long toolId) {
        this.toolId = toolId;
    }

    public List<Long> getTags() {
        return tags;
    }

    public void setTags(List<Long> tags) {
        this.tags = tags;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = StringUtils.trimToNull(keyword);
    }

    public String getNo() {
        return no;
    }

    public void setNo(String no) {
        this.no = StringUtils.trimToNull(no);
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = StringUtils.trimToNull(resourceType);
    }

    public Boolean getIsFollowing() {
        return isFollowing;
    }

    public void setIsFollowing(Boolean isFollowing) {
        this.isFollowing = isFollowing;
    }

    public Integer getCollectionFlag() {
        return collectionFlag;
    }

    public void setCollectionFlag(Integer collectionFlag) {
        this.collectionFlag = collectionFlag;
    }
}
