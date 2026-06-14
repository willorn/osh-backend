package com.backstage.system.domain.vo.order;

import java.util.ArrayList;
import java.util.List;

/**
 * 管理端订单详情。
 */
public class AdminOrderDetailVO extends AdminOrderPageItemVO {

    private List<AdminOrderPaymentVO> payments = new ArrayList<>();

    public List<AdminOrderPaymentVO> getPayments() { return payments; }
    public void setPayments(List<AdminOrderPaymentVO> payments) { this.payments = payments; }
}
