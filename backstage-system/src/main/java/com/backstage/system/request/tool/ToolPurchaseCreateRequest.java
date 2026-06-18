package com.backstage.system.request.tool;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@ApiModel(description = "工具购买下单请求")
public class ToolPurchaseCreateRequest {

    @NotNull(message = "套餐ID不能为空")
    @ApiModelProperty(value = "全局次数套餐ID", required = true, example = "2001")
    private Long packageId;

    @NotNull(message = "支付方式不能为空")
    @ApiModelProperty(value = "套餐支付类型：1-仅现金，2-仅积分，3-现金或积分", required = true, example = "3")
    private Integer payType;

    @NotBlank(message = "支付方式标识不能为空")
    @ApiModelProperty(value = "支付方式标识：points/wxpay/alipay", required = true, example = "points")
    private String paymentMethod;

    public Long getPackageId() {
        return packageId;
    }

    public void setPackageId(Long packageId) {
        this.packageId = packageId;
    }

    public Integer getPayType() {
        return payType;
    }

    public void setPayType(Integer payType) {
        this.payType = payType;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }
}
