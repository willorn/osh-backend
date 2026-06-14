package com.backstage.system.service.order;

import com.backstage.common.exception.ServiceException;
import com.backstage.system.domain.order.enums.ProductTypeEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 订单取消处理器注册表。
 */
@Component
public class OrderCancelHandlerRegistry {

    private static final Logger log = LoggerFactory.getLogger(OrderCancelHandlerRegistry.class);

    private final Map<String, OrderCancelHandler> handlerMap;

    /**
     * 构造处理器注册表。
     *
     * @param handlers 系统内所有订单取消处理器
     */
    public OrderCancelHandlerRegistry(List<OrderCancelHandler> handlers) {
        this.handlerMap = new HashMap<>();
        for (OrderCancelHandler handler : handlers) {
            String bizType = handler.bizType();
            if (ProductTypeEnum.fromName(bizType) == null) {
                throw new ServiceException("订单取消处理器业务类型非法, bizType=" + bizType);
            }
            handlerMap.put(bizType, handler);
            log.info("注册订单取消处理器成功, bizType={}, handler={}", bizType, handler.getClass().getSimpleName());
        }
    }

    /**
     * 根据业务类型获取处理器。
     *
     * @param bizType 业务类型编码
     * @return 对应处理器
     */
    public OrderCancelHandler getHandler(String bizType) {
        return handlerMap.get(bizType);
    }

    /**
     * 有处理器时执行订单取消后的业务释放逻辑。
     *
     * @param bizType 业务类型编码
     * @param orderNo 订单号
     * @param paymentNo 支付流水号
     * @return 是否执行了业务处理器
     */
    public boolean handleIfPresent(String bizType, String orderNo, String paymentNo) {
        OrderCancelHandler handler = getHandler(bizType);
        if (handler == null) {
            log.info("未配置订单取消处理器，跳过业务释放, bizType={}, orderNo={}", bizType, orderNo);
            return false;
        }
        log.info("准备执行订单取消处理器, bizType={}, orderNo={}, paymentNo={}", bizType, orderNo, paymentNo);
        handler.handle(orderNo, paymentNo);
        return true;
    }
}
