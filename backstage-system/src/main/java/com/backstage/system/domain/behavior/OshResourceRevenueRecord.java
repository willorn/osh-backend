package com.backstage.system.domain.behavior;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("osh_resource_revenue_record")
public class OshResourceRevenueRecord {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long contributionId;
    private Long orderId;
    private String orderNo;
    private Long buyerUserId;
    private BigDecimal revenueAmount;
    private Long pointAmount;
    private String bizType;
    private LocalDateTime revenueTime;
    private LocalDateTime createTime;
    private Long createBy;
    private Integer deleteFlag;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getContributionId() { return contributionId; }
    public void setContributionId(Long contributionId) { this.contributionId = contributionId; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public Long getBuyerUserId() { return buyerUserId; }
    public void setBuyerUserId(Long buyerUserId) { this.buyerUserId = buyerUserId; }
    public BigDecimal getRevenueAmount() { return revenueAmount; }
    public void setRevenueAmount(BigDecimal revenueAmount) { this.revenueAmount = revenueAmount; }
    public Long getPointAmount() { return pointAmount; }
    public void setPointAmount(Long pointAmount) { this.pointAmount = pointAmount; }
    public String getBizType() { return bizType; }
    public void setBizType(String bizType) { this.bizType = bizType; }
    public LocalDateTime getRevenueTime() { return revenueTime; }
    public void setRevenueTime(LocalDateTime revenueTime) { this.revenueTime = revenueTime; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public Long getCreateBy() { return createBy; }
    public void setCreateBy(Long createBy) { this.createBy = createBy; }
    public Integer getDeleteFlag() { return deleteFlag; }
    public void setDeleteFlag(Integer deleteFlag) { this.deleteFlag = deleteFlag; }
}
