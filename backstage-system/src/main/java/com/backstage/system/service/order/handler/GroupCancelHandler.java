package com.backstage.system.service.order.handler;

import com.backstage.system.domain.order.enums.ProductTypeEnum;
import com.backstage.system.service.order.OrderCancelHandler;
import com.backstage.system.task.GroupOrderTimeoutCancelTask;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 拼团订单取消处理器。
 */
@Component
public class GroupCancelHandler implements OrderCancelHandler {

    @Lazy
    @Resource
    private GroupOrderTimeoutCancelTask groupOrderTimeoutCancelTask;

    /**
     * 获取拼团业务类型标识。
     *
     * @return 商品类型标识
     */
    @Override
    public String bizType() {
        return ProductTypeEnum.GROUP.getName();
    }

    /**
     * 处理拼团订单取消后的名额释放。
     *
     * @param orderNo 业务订单号
     * @param paymentNo 支付流水号
     */
    @Override
    public void handle(String orderNo, String paymentNo) {
        groupOrderTimeoutCancelTask.cancelPendingOrderByOrderNo(orderNo);
    }
}
