package com.backstage.system.domain.vo.order;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 管理端订单支付流水。
 */
public class AdminOrderPaymentVO {

    private String orderNo;
    private String paymentNo;
    private Integer channel;
    private String channelName;
    private BigDecimal amount;
    private Integer status;
    private String statusName;
    private String platformTradeNo;
    private String payUrl;
    private String qrcode;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime paidTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expireTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdTime;
    private List<NotifyLogVO> notifyLogs = new ArrayList<>();

    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public String getPaymentNo() { return paymentNo; }
    public void setPaymentNo(String paymentNo) { this.paymentNo = paymentNo; }
    public Integer getChannel() { return channel; }
    public void setChannel(Integer channel) { this.channel = channel; }
    public String getChannelName() { return channelName; }
    public void setChannelName(String channelName) { this.channelName = channelName; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public String getStatusName() { return statusName; }
    public void setStatusName(String statusName) { this.statusName = statusName; }
    public String getPlatformTradeNo() { return platformTradeNo; }
    public void setPlatformTradeNo(String platformTradeNo) { this.platformTradeNo = platformTradeNo; }
    public String getPayUrl() { return payUrl; }
    public void setPayUrl(String payUrl) { this.payUrl = payUrl; }
    public String getQrcode() { return qrcode; }
    public void setQrcode(String qrcode) { this.qrcode = qrcode; }
    public LocalDateTime getPaidTime() { return paidTime; }
    public void setPaidTime(LocalDateTime paidTime) { this.paidTime = paidTime; }
    public LocalDateTime getExpireTime() { return expireTime; }
    public void setExpireTime(LocalDateTime expireTime) { this.expireTime = expireTime; }
    public LocalDateTime getCreatedTime() { return createdTime; }
    public void setCreatedTime(LocalDateTime createdTime) { this.createdTime = createdTime; }
    public List<NotifyLogVO> getNotifyLogs() { return notifyLogs; }
    public void setNotifyLogs(List<NotifyLogVO> notifyLogs) { this.notifyLogs = notifyLogs; }

    /**
     * 支付回调记录。
     */
    public static class NotifyLogVO {
        private String paymentNo;
        private String orderNo;
        private String platformTradeNo;
        private Integer signValid;
        private Integer processStatus;
        private String errorMsg;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime createdTime;

        public String getPaymentNo() { return paymentNo; }
        public void setPaymentNo(String paymentNo) { this.paymentNo = paymentNo; }
        public String getOrderNo() { return orderNo; }
        public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
        public String getPlatformTradeNo() { return platformTradeNo; }
        public void setPlatformTradeNo(String platformTradeNo) { this.platformTradeNo = platformTradeNo; }
        public Integer getSignValid() { return signValid; }
        public void setSignValid(Integer signValid) { this.signValid = signValid; }
        public Integer getProcessStatus() { return processStatus; }
        public void setProcessStatus(Integer processStatus) { this.processStatus = processStatus; }
        public String getErrorMsg() { return errorMsg; }
        public void setErrorMsg(String errorMsg) { this.errorMsg = errorMsg; }
        public LocalDateTime getCreatedTime() { return createdTime; }
        public void setCreatedTime(LocalDateTime createdTime) { this.createdTime = createdTime; }
    }
}
