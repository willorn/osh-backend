package com.backstage.system.domain.tool;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 工具对象 osh_tool
 */
@ApiModel(description = "工具信息")
@TableName("osh_tool")
public class OshTool {

    @ApiModelProperty("工具ID")
    private Long id;

    @ApiModelProperty("工具名称")
    private String toolName;

    @ApiModelProperty("工具编号")
    private String no;

    @ApiModelProperty("工具描述")
    private String description;

    @ApiModelProperty("访问类型：1-站内工具，2-iframe第三方工具")
    private Integer accessType;

    @ApiModelProperty("站内工具前端路由")
    private String routePath;

    @ApiModelProperty("第三方iframe地址")
    private String iframeUrl;

    @ApiModelProperty("GitHub地址")
    private String githubUrl;

    @ApiModelProperty("单次消耗工具点数")
    private Integer quotaCost;

    @ApiModelProperty("状态：2-待审核，4-上架，6-下架")
    private Integer status;

    @ApiModelProperty("好评数")
    private Integer goodCount;

    @ApiModelProperty("中评数")
    private Integer neutralCount;

    @ApiModelProperty("差评数")
    private Integer badCount;

    @ApiModelProperty("浏览数")
    private Long viewCount;

    @ApiModelProperty("累计使用次数")
    private Long totalUsage;

    @ApiModelProperty("收藏数")
    private Integer collectionCount;

    @ApiModelProperty("备注")
    private String remark;

    @ApiModelProperty("资源类型：FREE,CASH_POINT")
    private String resourceType;

    @ApiModelProperty("资源等级")
    private Integer level;

    @ApiModelProperty("创建者")
    private String createBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty("创建时间")
    private LocalDateTime createTime;

    @ApiModelProperty("更新者")
    private String updateBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty("更新时间")
    private LocalDateTime updateTime;

    @ApiModelProperty("逻辑删除：0-未删除，1-已删除")
    private Integer deleteFlag;

    @ApiModelProperty("标签名称列表")
    private List<String> tags;

    @ApiModelProperty("当前用户是否收藏：0-否，1-是")
    private Integer collectionFlag;

    @ApiModelProperty("当前用户剩余使用次数")
    private Integer remainingCount;

    @ApiModelProperty("当前用户是否已购买：0-否，1-是")
    private Integer purchasedFlag;

    @ApiModelProperty("当前用户评价类型：0-未评价，1-点赞，3-差评")
    private Integer voteType;

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
        this.toolName = toolName;
    }

    public String getNo() {
        return no;
    }

    public void setNo(String no) {
        this.no = no;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getAccessType() {
        return accessType;
    }

    public void setAccessType(Integer accessType) {
        this.accessType = accessType;
    }

    public String getRoutePath() {
        return routePath;
    }

    public void setRoutePath(String routePath) {
        this.routePath = routePath;
    }

    public String getIframeUrl() {
        return iframeUrl;
    }

    public void setIframeUrl(String iframeUrl) {
        this.iframeUrl = iframeUrl;
    }

    public String getGithubUrl() {
        return githubUrl;
    }

    public void setGithubUrl(String githubUrl) {
        this.githubUrl = githubUrl;
    }

    public Integer getQuotaCost() {
        return quotaCost;
    }

    public void setQuotaCost(Integer quotaCost) {
        this.quotaCost = quotaCost;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Integer getGoodCount() {
        return goodCount;
    }

    public void setGoodCount(Integer goodCount) {
        this.goodCount = goodCount;
    }

    public Integer getNeutralCount() {
        return neutralCount;
    }

    public void setNeutralCount(Integer neutralCount) {
        this.neutralCount = neutralCount;
    }

    public Integer getBadCount() {
        return badCount;
    }

    public void setBadCount(Integer badCount) {
        this.badCount = badCount;
    }

    public Long getViewCount() {
        return viewCount;
    }

    public void setViewCount(Long viewCount) {
        this.viewCount = viewCount;
    }

    public Long getTotalUsage() {
        return totalUsage;
    }

    public void setTotalUsage(Long totalUsage) {
        this.totalUsage = totalUsage;
    }

    public Integer getCollectionCount() {
        return collectionCount;
    }

    public void setCollectionCount(Integer collectionCount) {
        this.collectionCount = collectionCount;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public Integer getLevel() {
        return level;
    }

    public void setLevel(Integer level) {
        this.level = level;
    }

    public String getCreateBy() {
        return createBy;
    }

    public void setCreateBy(String createBy) {
        this.createBy = createBy;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public String getUpdateBy() {
        return updateBy;
    }

    public void setUpdateBy(String updateBy) {
        this.updateBy = updateBy;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public Integer getDeleteFlag() {
        return deleteFlag;
    }

    public void setDeleteFlag(Integer deleteFlag) {
        this.deleteFlag = deleteFlag;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public Integer getCollectionFlag() {
        return collectionFlag;
    }

    public void setCollectionFlag(Integer collectionFlag) {
        this.collectionFlag = collectionFlag;
    }

    public Integer getRemainingCount() {
        return remainingCount;
    }

    public void setRemainingCount(Integer remainingCount) {
        this.remainingCount = remainingCount;
    }

    public Integer getPurchasedFlag() {
        return purchasedFlag;
    }

    public void setPurchasedFlag(Integer purchasedFlag) {
        this.purchasedFlag = purchasedFlag;
    }

    public Integer getVoteType() {
        return voteType;
    }

    public void setVoteType(Integer voteType) {
        this.voteType = voteType;
    }

}
