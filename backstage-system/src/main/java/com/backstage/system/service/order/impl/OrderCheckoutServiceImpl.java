package com.backstage.system.service.order.impl;

import com.backstage.system.domain.vo.pay.OrderCheckoutReqVO;
import com.backstage.system.domain.vo.pay.OrderCheckoutRespVO;
import com.backstage.system.service.order.OrderCheckoutService;
import com.backstage.system.service.order.OrderService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 订单结算服务实现，作为其他业务模块调用订单能力的入口。
 */
@Service
public class OrderCheckoutServiceImpl implements OrderCheckoutService {

    @Resource
    private OrderService orderService;

    /**
     * 创建业务商品订单并发起支付。
     *
     * @param reqVO 订单结算参数
     * @return 订单结算结果
     */
    @Override
    public OrderCheckoutRespVO checkout(OrderCheckoutReqVO reqVO) {
        OrderCheckoutRespVO checkoutRespVO = orderService.checkout(reqVO, Boolean.TRUE);
        OrderCheckoutRespVO respVO = new OrderCheckoutRespVO();
        BeanUtils.copyProperties(checkoutRespVO, respVO);
        return respVO;
    }
}
