package com.backstage.system.domain.member.vo;

public class MemberPayStatusVO {
    private String orderNo;
    private Integer orderStatus;
    private Integer paymentStatus;
    private Integer memberPayStatus;
    private Integer grantStatus;
    private String grantMessage;
    private Boolean paid;
    private Boolean granted;

    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public Integer getOrderStatus() { return orderStatus; }
    public void setOrderStatus(Integer orderStatus) { this.orderStatus = orderStatus; }
    public Integer getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(Integer paymentStatus) { this.paymentStatus = paymentStatus; }
    public Integer getMemberPayStatus() { return memberPayStatus; }
    public void setMemberPayStatus(Integer memberPayStatus) { this.memberPayStatus = memberPayStatus; }
    public Integer getGrantStatus() { return grantStatus; }
    public void setGrantStatus(Integer grantStatus) { this.grantStatus = grantStatus; }
    public String getGrantMessage() { return grantMessage; }
    public void setGrantMessage(String grantMessage) { this.grantMessage = grantMessage; }
    public Boolean getPaid() { return paid; }
    public void setPaid(Boolean paid) { this.paid = paid; }
    public Boolean getGranted() { return granted; }
    public void setGranted(Boolean granted) { this.granted = granted; }
}
