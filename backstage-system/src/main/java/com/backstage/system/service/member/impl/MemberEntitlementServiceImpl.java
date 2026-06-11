package com.backstage.system.service.member.impl;

import com.backstage.common.exception.ServiceException;
import com.backstage.system.domain.member.OshMemberOrder;
import com.backstage.system.mapper.member.OshMemberOrderMapper;
import com.backstage.system.service.member.MemberEntitlementService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;

@Service
public class MemberEntitlementServiceImpl implements MemberEntitlementService {
    private static final int GRANT_STATUS_SUCCESS = 1;

    @Resource
    private OshMemberOrderMapper memberOrderMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handlePaid(String orderNo) {
        OshMemberOrder order = memberOrderMapper.selectByOrderNoForUpdate(orderNo);
        if (order == null) {
            throw new ServiceException("会员订单不存在");
        }
        if (Integer.valueOf(GRANT_STATUS_SUCCESS).equals(order.getGrantStatus())) {
            return;
        }

        try {
            Integer roleId = memberOrderMapper.selectRoleIdByCode(order.getMemberType());
            if (roleId == null) {
                throw new ServiceException("会员角色不存在: " + order.getMemberType());
            }
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime currentExpire = memberOrderMapper.selectRoleExpireForUpdate(order.getUserId(), roleId);
            LocalDateTime startTime = currentExpire != null && currentExpire.isAfter(now) ? currentExpire : now;
            LocalDateTime expireTime = startTime.plusMonths(order.getDurationMonths());

            memberOrderMapper.upsertUserRole(order.getUserId(), roleId, expireTime, 0L);
            memberOrderMapper.updatePaid(orderNo, now);
            int updated = memberOrderMapper.updateGrantSuccess(order.getId(), startTime, expireTime, now);
            if (updated <= 0) {
                throw new ServiceException("会员权益发放状态更新失败");
            }
        } catch (Exception ex) {
            memberOrderMapper.updateGrantFailed(order.getId(), StringUtils.left(ex.getMessage(), 500));
            if (ex instanceof RuntimeException) {
                throw (RuntimeException) ex;
            }
            throw new ServiceException("会员权益发放失败");
        }
    }
}
