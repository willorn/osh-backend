package com.backstage.system.controller.pay;


import com.backstage.common.core.domain.R;
import com.backstage.system.domain.vo.pay.OrderCheckoutReqVO;
import com.backstage.system.domain.vo.pay.OrderCheckoutRespVO;
import com.backstage.system.domain.vo.pay.PayCreateRespVO;
import com.backstage.system.domain.order.OrderPaymentInfo;
import com.backstage.system.domain.order.OrderStatusResult;
import com.backstage.system.service.order.OrderCheckoutService;
import com.backstage.system.service.order.OrderService;
import com.backstage.system.utils.UserContextUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/pc/pay")
public class PayController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderCheckoutService orderCheckoutService;

    /**
     * 创建支付订单。
     *
     * @param reqVO 订单结算参数
     * @return 支付创建结果
     */
    @PostMapping("/create")
    @PreAuthorize("hasAuthority('pay:create')")
    public R<PayCreateRespVO> create(@Valid @RequestBody OrderCheckoutReqVO reqVO) {
        OrderCheckoutRespVO checkoutResult = orderCheckoutService.checkout(reqVO);

        PayCreateRespVO response = new PayCreateRespVO();
        BeanUtils.copyProperties(checkoutResult, response);
        response.setCode(1);
        response.setMsg("ok");
        response.setOutTradeNo(checkoutResult.getPaymentNo());
        OrderPaymentInfo payment = checkoutResult.getPayment();
        if (payment != null) {
            response.setQrcode(payment.getQrcode());
            response.setPayUrl(payment.getPayUrl());
        }
        return R.ok(response);
    }

    /**
     * 查询订单支付状态（前端轮询）。
     *
     * @param orderNo 订单编号
     * @return 订单支付状态mmi
     */
    @GetMapping("/status")
    public R<OrderStatusResult> status(@RequestParam String orderNo) {
        return R.ok(orderService.getOrderStatusForUser(orderNo, UserContextUtil.getCurrentUserIdSafely()));
    }

    /**
     * 取消支付（用户主动取消）。
     *
     * @param orderNo 订单编号
     * @return 取消支付结果
     */

    @PostMapping("/cancel")
    public R<Void> cancel(@RequestParam String orderNo) {
        orderService.cancelPaymentByOrderNoForUser(orderNo, UserContextUtil.getCurrentUserIdSafely());
        return R.ok();
    }
}
