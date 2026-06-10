package com.backstage.system.service.order.handler;

import com.backstage.system.domain.order.enums.ProductTypeEnum;
import com.backstage.system.service.member.MemberEntitlementService;
import com.backstage.system.service.order.OrderPaidHandler;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
public class MemberPaidHandler implements OrderPaidHandler {
    @Resource
    private MemberEntitlementService memberEntitlementService;

    @Override
    public String bizType() {
        return ProductTypeEnum.MEMBER.getName();
    }

    @Override
    public void handle(String orderNo) {
        memberEntitlementService.handlePaid(orderNo);
    }
}
