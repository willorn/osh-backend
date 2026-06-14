package com.backstage.quartz.task;

import com.backstage.system.domain.order.OshPayment;
import com.backstage.system.mapper.order.OshPaymentMapper;
import com.backstage.system.service.order.OrderService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class OrderCloseTask {

    private static final Logger log = LoggerFactory.getLogger(OrderCloseTask.class);

    /**
     * 订单关闭缓冲时间，单位：分钟
     */
    private static final long ORDER_CLOSE_BUFFER_MINUTES = 1L;

    @Resource
    private OshPaymentMapper oshPaymentMapper;

    @Resource
    private OrderService orderService;

    @XxlJob("OrderCloseHandler")
    public void closeExpiredPendingPayments() {
        LocalDateTime expireDeadline = LocalDateTime.now().minusMinutes(ORDER_CLOSE_BUFFER_MINUTES);
        List<OshPayment> payments = oshPaymentMapper.selectExpiredPendingPayments(expireDeadline);
        if (payments == null || payments.isEmpty()) {
            XxlJobHelper.log("未扫描到超时待支付订单");
            return;
        }
        XxlJobHelper.log("扫描到超时待支付订单数量: {0}", payments.size());
        log.info("扫描到超时待支付订单数量: {}", payments.size());
        for (OshPayment payment : payments) {
            if (payment == null || payment.getPaymentNo() == null) {
                continue;
            }
            try {
                orderService.cancelPayment(payment.getPaymentNo());
            } catch (Exception ex) {
                XxlJobHelper.log("关闭超时支付订单失败, paymentNo={0}, error={1}", payment.getPaymentNo(), ex.getMessage());
                log.warn("关闭超时支付订单失败, paymentNo={}, error={}", payment.getPaymentNo(), ex.getMessage(), ex);
            }
        }
    }
}
