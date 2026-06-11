package com.backstage.system.service.member;

import com.backstage.system.domain.member.OshMemberPlan;
import com.backstage.system.domain.member.dto.MemberCheckoutDTO;
import com.backstage.system.domain.member.dto.MemberPlanConfigDTO;
import com.backstage.system.domain.member.dto.MemberPricingRuleDTO;
import com.backstage.system.domain.member.vo.MemberCenterVO;
import com.backstage.system.domain.member.vo.MemberOrderVO;
import com.backstage.system.domain.member.vo.MemberPayStatusVO;
import com.backstage.system.domain.vo.pay.OrderCheckoutRespVO;

import java.util.List;

public interface MemberCenterService {
    MemberCenterVO getCenter(Long userId);
    List<OshMemberPlan> listPlans();
    List<OshMemberPlan> listAllPlansForFounder();
    void updatePlanConfig(Long operatorId, MemberPlanConfigDTO dto);
    void updatePricingRule(Long operatorId, MemberPricingRuleDTO dto);
    OrderCheckoutRespVO checkout(Long userId, MemberCheckoutDTO dto);
    MemberPayStatusVO getPayStatus(Long userId, String orderNo);
    List<MemberOrderVO> listOrders(Long userId);
}
