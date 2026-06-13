package com.backstage.system.request.tool;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@ApiModel(description = "工具点数套餐保存请求")
public class ToolQuotaPackageSaveRequest {

    @ApiModelProperty(value = "套餐ID，新增不传，修改必传", example = "1001")
    private Long id;

    @NotBlank(message = "套餐名称不能为空")
    @ApiModelProperty(value = "套餐名称", required = true, example = "基础包")
    private String packageName;

    @NotNull(message = "工具点数不能为空")
    @ApiModelProperty(value = "购买后增加的工具点数", required = true, example = "100")
    private Integer useCount;

    @NotNull(message = "现金金额不能为空")
    @ApiModelProperty(value = "现金金额", required = true, example = "9.90")
    private BigDecimal price;

    @ApiModelProperty(value = "积分金额", example = "100")
    private Integer pointCost;

    @NotNull(message = "支付类型不能为空")
    @ApiModelProperty(value = "支付类型：1-纯现金，3-现金+积分", required = true, example = "1")
    private Integer payType;

    @ApiModelProperty(value = "状态：0-停用，1-启用", example = "1")
    private Integer status;

    @ApiModelProperty(value = "排序", example = "10")
    private Integer sortOrder;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public Integer getUseCount() {
        return useCount;
    }

    public void setUseCount(Integer useCount) {
        this.useCount = useCount;
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

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
}
