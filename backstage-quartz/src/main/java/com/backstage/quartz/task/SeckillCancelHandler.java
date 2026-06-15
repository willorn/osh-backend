package com.backstage.quartz.task;

import com.backstage.system.domain.order.enums.ProductTypeEnum;
import com.backstage.system.service.order.OrderCancelHandler;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 秒杀订单取消处理器。
 */
@Component
public class SeckillCancelHandler implements OrderCancelHandler {

    @Lazy
    @Resource
    private SeckillOrderTimeoutTask seckillOrderTimeoutTask;

    /**
     * 获取秒杀业务类型标识。
     *
     * @return 商品类型标识
     */
    @Override
    public String bizType() {
        return ProductTypeEnum.SECKILL.getName();
    }

    /**
     * 处理秒杀订单取消后的库存和流程状态回滚。
     *
     * @param orderNo 业务订单号
     * @param paymentNo 支付流水号
     */
    @Override
    public void handle(String orderNo, String paymentNo) {
        seckillOrderTimeoutTask.cancelPendingOrderByOrderNo(orderNo);
    }
}
