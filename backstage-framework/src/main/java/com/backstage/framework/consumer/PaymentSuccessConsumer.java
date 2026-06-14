package com.backstage.framework.consumer;


import com.backstage.common.constant.KafkaConstants;
import com.backstage.system.domain.message.order.PaySuccessMessage;
import com.backstage.system.domain.order.OshOrder;
import com.backstage.system.domain.order.enums.ProductTypeEnum;
import com.backstage.system.domain.websocket.WsNotifyMessage;
import com.backstage.system.mapper.order.OshOrderMapper;
import com.backstage.system.service.order.OrderPaidHandlerRegistry;
import com.backstage.system.service.websocket.WebSocketNotifyService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Objects;

/**
 * 支付成功消息kafka消费者，
 */
@Component
public class PaymentSuccessConsumer {

    private static final Logger log = LoggerFactory.getLogger(PaymentSuccessConsumer.class);

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private OshOrderMapper orderMapper;

    @Resource
    private WebSocketNotifyService webSocketNotifyService;

    @Resource
    private OrderPaidHandlerRegistry paidHandlerRegistry;

    @KafkaListener(
            topics = KafkaConstants.PAY_SUCCESS_TOPIC,
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consumeUserEvent(String message, Acknowledgment ack) {
        log.info("【Kafka消费者】收到支付成功消息：{}", message);

        try {
            PaySuccessMessage successMessage = objectMapper.readValue(message, PaySuccessMessage.class);
            String orderNo = successMessage.getOrderNo();
            OshOrder order = orderMapper.selectByOrderNo(successMessage.getOrderNo());
            if (Objects.isNull(order) || Objects.isNull(order.getProductType())) {
                log.warn("权益发放跳过，订单信息不完整: orderNo={}", orderNo);
                ack.acknowledge();
                return;
            }

            // 调用对应业务模块的权益handler
            ProductTypeEnum productTypeEnum = ProductTypeEnum.fromCode(order.getProductType());
            if (Objects.isNull(productTypeEnum)) {
                log.warn("权益发放跳过，未知商品类型: orderNo={}, productType={}", orderNo, order.getProductType());
                ack.acknowledge();
                return;
            }
            log.info("权益发放命中商品类型, orderNo={}, productType={}, bizType={}",
                    orderNo, order.getProductType(), productTypeEnum.getName());
            try {
                paidHandlerRegistry.handle(productTypeEnum.getName(), orderNo);
            } catch (Exception e) {
                log.error("权益发放失败, orderNo={}, productType={}", orderNo, order.getProductType(), e);
                return;
            }

            // 调用websocket 让前端发送通知告知用户支付已完成
            WsNotifyMessage msg = new WsNotifyMessage();
            msg.setType("PAY_SUCCESS_NOTIFY");
            msg.setTitle("您购买的 " + order.getProductName() + "支付成功！");
            msg.setContent(null);
            msg.setJumpUrl(null);
            msg.setBizId(orderNo);
            webSocketNotifyService.send(order.getUserId(), msg);
            ack.acknowledge();

        }catch (Exception e){
            log.error("【Kafka消费者】消息处理异常：", e);
        }
    }
}
