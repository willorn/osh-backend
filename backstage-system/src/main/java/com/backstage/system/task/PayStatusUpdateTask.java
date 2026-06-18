package com.backstage.system.task;

import com.backstage.system.domain.order.OshPayment;
import com.backstage.system.mapper.order.OshPaymentMapper;
import com.backstage.system.service.order.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class PayStatusUpdateTask {
    private static final Logger log = LoggerFactory.getLogger(PayStatusUpdateTask.class);

    private static final long ORDER_CLOSE_BUFFER_MINUTES = 1L;


    @Resource
    private OrderService orderService;

    @Resource
    private OshPaymentMapper paymentMapper;


    /**
     * 关闭超时未支付的支付流水和关联订单。
     */
//    @Scheduled(cron = "0 0/30 * * * ?")
    public void updateOrderStatus() {
        log.info("【支付-定时任务】开始关闭超时未支付订单");


        try{
            LocalDateTime expireDeadline = LocalDateTime.now().minusMinutes(ORDER_CLOSE_BUFFER_MINUTES);
            List<OshPayment> expiredPayments = paymentMapper.selectExpiredPendingPayments(expireDeadline);
            for (OshPayment payment : expiredPayments) {
                closeExpiredPayment(payment);
            }
        }catch (Exception e){
            log.error("【支付-定时任务】关闭超时未支付订单异常, 异常详情:{}",e.getMessage(),e);
        }

        log.info("【支付-定时任务】关闭超时未支付订单完成");
    }

    /**
     * 逐笔关闭过期支付流水，单笔异常不影响后续流水。
     *
     * @param payment 过期待支付流水
     */
    private void closeExpiredPayment(OshPayment payment) {
        try {
            orderService.cancelPayment(payment.getPaymentNo());
        } catch (Exception e) {
            log.warn("【支付-定时任务】关闭超时支付流水失败, paymentNo={}, orderNo={}, error={}",
                    payment.getPaymentNo(), payment.getOrderNo(), e.getMessage(), e);
        }
    }
}
