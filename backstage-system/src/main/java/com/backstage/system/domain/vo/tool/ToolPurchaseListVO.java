package com.backstage.system.domain.vo.tool;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@ApiModel(description = "工具点数购买记录")
public class ToolPurchaseListVO {

    @ApiModelProperty(value = "订单号", example = "O20260517001")
    private String orderNo;

    @ApiModelProperty(value = "支付流水号", example = "P20260517001")
    private String paymentNo;

    @ApiModelProperty(value = "套餐ID", example = "2001")
    private Long packageId;

    @ApiModelProperty(value = "套餐名称", example = "体验包")
    private String packageNameSnapshot;

    @ApiModelProperty(value = "次数快照", example = "10")
    private Integer packageUseCountSnapshot;

    @ApiModelProperty(value = "现金金额快照", example = "9.90")
    private BigDecimal packageCashAmountSnapshot;

    @ApiModelProperty(value = "积分金额快照", example = "100")
    private Integer packagePointAmountSnapshot;

    @ApiModelProperty(value = "支付类型快照", example = "3")
    private Integer packagePayTypeSnapshot;

    @ApiModelProperty(value = "订单状态", example = "1")
    private Integer orderStatus;

    @ApiModelProperty(value = "发放状态", example = "1")
    private Integer grantStatus;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "发放时间", example = "2026-05-17 10:00:00")
    private LocalDateTime grantTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "创建时间", example = "2026-05-17 09:59:00")
    private LocalDateTime createTime;

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public String getPaymentNo() {
        return paymentNo;
    }

    public void setPaymentNo(String paymentNo) {
        this.paymentNo = paymentNo;
    }

    public Long getPackageId() {
        return packageId;
    }

    public void setPackageId(Long packageId) {
        this.packageId = packageId;
    }

    public String getPackageNameSnapshot() {
        return packageNameSnapshot;
    }

    public void setPackageNameSnapshot(String packageNameSnapshot) {
        this.packageNameSnapshot = packageNameSnapshot;
    }

    public Integer getPackageUseCountSnapshot() {
        return packageUseCountSnapshot;
    }

    public void setPackageUseCountSnapshot(Integer packageUseCountSnapshot) {
        this.packageUseCountSnapshot = packageUseCountSnapshot;
    }

    public BigDecimal getPackageCashAmountSnapshot() {
        return packageCashAmountSnapshot;
    }

    public void setPackageCashAmountSnapshot(BigDecimal packageCashAmountSnapshot) {
        this.packageCashAmountSnapshot = packageCashAmountSnapshot;
    }

    public Integer getPackagePointAmountSnapshot() {
        return packagePointAmountSnapshot;
    }

    public void setPackagePointAmountSnapshot(Integer packagePointAmountSnapshot) {
        this.packagePointAmountSnapshot = packagePointAmountSnapshot;
    }

    public Integer getPackagePayTypeSnapshot() {
        return packagePayTypeSnapshot;
    }

    public void setPackagePayTypeSnapshot(Integer packagePayTypeSnapshot) {
        this.packagePayTypeSnapshot = packagePayTypeSnapshot;
    }

    public Integer getOrderStatus() {
        return orderStatus;
    }

    public void setOrderStatus(Integer orderStatus) {
        this.orderStatus = orderStatus;
    }

    public Integer getGrantStatus() {
        return grantStatus;
    }

    public void setGrantStatus(Integer grantStatus) {
        this.grantStatus = grantStatus;
    }

    public LocalDateTime getGrantTime() {
        return grantTime;
    }

    public void setGrantTime(LocalDateTime grantTime) {
        this.grantTime = grantTime;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}
