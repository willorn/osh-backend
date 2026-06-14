package com.backstage.system.service.order.handler;

import com.backstage.system.domain.order.enums.ProductTypeEnum;
import com.backstage.system.service.order.OrderCancelHandler;
import com.backstage.system.service.tool.ToolPurchaseService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 工具订单取消处理器。
 */
@Component
public class ToolCancelHandler implements OrderCancelHandler {

    @Lazy
    @Resource
    private ToolPurchaseService toolPurchaseService;

    /**
     * 获取工具业务类型标识。
     *
     * @return 商品类型标识
     */
    @Override
    public String bizType() {
        return ProductTypeEnum.TOOL.getName();
    }

    /**
     * 处理工具订单取消后的购买记录回滚。
     *
     * @param orderNo 业务订单号
     * @param paymentNo 支付流水号
     */
    @Override
    public void handle(String orderNo, String paymentNo) {
        toolPurchaseService.cancelPendingPurchase(paymentNo);
    }
}
