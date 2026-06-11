package com.backstage.system.domain.vo.tool;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.math.BigDecimal;

@ApiModel(description = "工具点数套餐")
public class ToolQuotaPackageVO {

    @ApiModelProperty(value = "套餐ID", example = "1001")
    private Long packageId;

    @ApiModelProperty(value = "套餐名称", example = "基础包")
    private String packageName;

    @ApiModelProperty(value = "购买后增加的工具点数", example = "100")
    private Integer useCount;

    @ApiModelProperty(value = "现金金额", example = "9.90")
    private BigDecimal cashAmount;

    @ApiModelProperty(value = "积分金额", example = "100")
    private Integer pointAmount;

    @ApiModelProperty(value = "支付类型：1-纯现金，3-现金+积分", example = "1")
    private Integer payType;

    @ApiModelProperty(value = "状态：0-停用，1-启用", example = "1")
    private Integer status;

    @ApiModelProperty(value = "排序", example = "10")
    private Integer sortOrder;

    public Long getPackageId() {
        return packageId;
    }

    public void setPackageId(Long packageId) {
        this.packageId = packageId;
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

    public BigDecimal getCashAmount() {
        return cashAmount;
    }

    public void setCashAmount(BigDecimal cashAmount) {
        this.cashAmount = cashAmount;
    }

    public Integer getPointAmount() {
        return pointAmount;
    }

    public void setPointAmount(Integer pointAmount) {
        this.pointAmount = pointAmount;
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
