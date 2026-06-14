package com.backstage.system.domain.tool;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@ApiModel(description = "工具点数购买记录")
@TableName("osh_tool_package_purchase_record")
public class OshToolPurchaseRecord {

    @ApiModelProperty("主键ID")
    private Long id;

    @ApiModelProperty("统一订单号")
    private String orderNo;

    @ApiModelProperty("支付流水号")
    private String paymentNo;

    @ApiModelProperty("用户ID")
    private Long userId;

    @ApiModelProperty("套餐ID")
    private Long packageId;

    @ApiModelProperty("套餐名称快照")
    private String packageNameSnapshot;

    @ApiModelProperty("套餐次数快照")
    private Integer packageUseCountSnapshot;

    @ApiModelProperty("套餐现金金额快照")
    private BigDecimal packageCashAmountSnapshot;

    @ApiModelProperty("套餐积分金额快照")
    private Integer packagePointAmountSnapshot;

    @ApiModelProperty("套餐支付类型快照：1-纯现金，3-现金+积分")
    private Integer packagePayTypeSnapshot;

    @ApiModelProperty("订单状态：0-待支付，1-已支付，2-已取消，3-已关闭")
    private Integer orderStatus;

    @ApiModelProperty("发放状态：0-待发放，1-已发放，2-发放失败")
    private Integer grantStatus;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty("发放时间")
    private LocalDateTime grantTime;

    @ApiModelProperty("备注")
    private String remark;

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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
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

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
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
}
