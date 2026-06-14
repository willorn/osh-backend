package com.backstage.system.service.order;

/**
 * 订单取消后的业务处理器。
 */
public interface OrderCancelHandler {

    /**
     * 获取业务类型标识。
     *
     * @return 业务类型标识
     */
    String bizType();

    /**
     * 处理统一订单取消后的业务释放逻辑。
     *
     * @param orderNo 业务订单号
     * @param paymentNo 支付流水号
     */
    void handle(String orderNo, String paymentNo);
}
