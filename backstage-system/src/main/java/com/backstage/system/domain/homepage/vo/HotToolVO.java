package com.backstage.system.domain.homepage.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.math.BigDecimal;

/**
 * 首页热门工具 VO
 *
 * @author jayTatum
 */
@ApiModel(description = "首页热门工具")
public class HotToolVO {

    @ApiModelProperty("工具ID")
    private Long id;

    @ApiModelProperty("工具名称")
    private String toolName;

    @ApiModelProperty("工具描述")
    private String description;

    @ApiModelProperty("资源类型")
    private String resourceType;

    @ApiModelProperty("当前现金价格")
    private BigDecimal price;

    @ApiModelProperty("当前点数价格")
    private Integer pointCost;

    @ApiModelProperty("支付类型：0-无需支付，1-现金，3-现金+积分")
    private Integer payType;

    @ApiModelProperty("累计使用次数")
    private Long totalUsage;

    @ApiModelProperty("详情页跳转路径")
    private String detailUrl;

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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Integer getPointCost() {
        return pointCost;
    }

    public void setPointCost(Integer pointCost) {
        this.pointCost = pointCost;
    }

    public Integer getPayType() {
        return payType;
    }

    public void setPayType(Integer payType) {
        this.payType = payType;
    }

    public Long getTotalUsage() {
        return totalUsage;
    }

    public void setTotalUsage(Long totalUsage) {
        this.totalUsage = totalUsage;
    }

    public String getDetailUrl() {
        return detailUrl;
    }

    public void setDetailUrl(String detailUrl) {
        this.detailUrl = detailUrl;
    }
}
