package com.backstage.system.service.impl.behavior;

import com.backstage.common.constant.OshUserConstants;
import com.backstage.common.threadlocal.ThreadLocalUtil;
import com.backstage.common.utils.generate.GenerateUtil;
import com.backstage.system.domain.behavior.OshResourceContribution;
import com.backstage.system.domain.behavior.OshResourceRevenueRecord;
import com.backstage.system.enums.behavior.ContributionResourceType;
import com.backstage.system.mapper.behavior.OshResourceContributionMapper;
import com.backstage.system.mapper.behavior.OshResourceRevenueRecordMapper;
import com.backstage.system.service.behavior.ContributionService;
import com.backstage.system.service.behavior.ResourceNoResolver;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class ContributionServiceImpl implements ContributionService {
    private static final int MIN_INTERNAL_LEVEL = 4;
    private static final int MAX_INTERNAL_LEVEL = 6;

    @Resource
    private OshResourceContributionMapper contributionMapper;

    @Resource
    private OshResourceRevenueRecordMapper revenueRecordMapper;

    @Resource
    private ResourceNoResolver resourceNoResolver;

    @Override
    public void recordContribution(String resourceType, Long resourceId, String resourceName) {
        Long userId = ThreadLocalUtil.get(OshUserConstants.USER_ID, Long.class);
        String username = ThreadLocalUtil.get(OshUserConstants.USERNAME, String.class);
        Integer level = parseLevel(ThreadLocalUtil.get(OshUserConstants.LEVEL, String.class));
        if (userId == null || !ContributionResourceType.contributionTracked(resourceType) || resourceId == null || !isInternalLevel(level)) {
            return;
        }
        String resourceNo = resourceNoResolver.resolveResourceNo(resourceType, resourceId);

        OshResourceContribution existing = contributionMapper.selectOne(new LambdaQueryWrapper<OshResourceContribution>()
                .eq(OshResourceContribution::getContributorUserId, userId)
                .eq(OshResourceContribution::getResourceType, resourceType)
                .eq(OshResourceContribution::getResourceId, resourceId)
                .last("limit 1"));
        LocalDateTime now = LocalDateTime.now();
        if (existing != null) {
            existing.setContributorUsername(username);
            existing.setContributorRoleLevel(level);
            existing.setResourceNo(resourceNo);
            existing.setResourceName(resourceName);
            existing.setStatus(1);
            existing.setDeleteFlag(0);
            existing.setUpdateBy(userId);
            existing.setUpdateTime(now);
            contributionMapper.updateById(existing);
            return;
        }

        OshResourceContribution contribution = new OshResourceContribution();
        contribution.setId(GenerateUtil.generateSnowflakeId());
        contribution.setContributorUserId(userId);
        contribution.setContributorUsername(username);
        contribution.setContributorRoleLevel(level);
        contribution.setResourceType(resourceType);
        contribution.setResourceId(resourceId);
        contribution.setResourceNo(resourceNo);
        contribution.setResourceName(resourceName);
        contribution.setStatus(1);
        contribution.setCreateBy(userId);
        contribution.setCreateTime(now);
        contribution.setDeleteFlag(0);
        contributionMapper.insert(contribution);
    }

    @Override
    public void recordRevenue(String resourceType, Long resourceId, Long orderId, String orderNo,
                              Long buyerUserId, BigDecimal revenueAmount, Long pointAmount, String bizType) {
        if (!ContributionResourceType.contributionTracked(resourceType) || resourceId == null) {
            return;
        }
        OshResourceContribution contribution = contributionMapper.selectOne(new LambdaQueryWrapper<OshResourceContribution>()
                .eq(OshResourceContribution::getResourceType, resourceType)
                .eq(OshResourceContribution::getResourceId, resourceId)
                .eq(OshResourceContribution::getStatus, 1)
                .eq(OshResourceContribution::getDeleteFlag, 0)
                .orderByAsc(OshResourceContribution::getCreateTime)
                .last("limit 1"));
        if (contribution == null) {
            return;
        }
        if (orderNo != null && revenueRecordMapper.selectCount(new LambdaQueryWrapper<OshResourceRevenueRecord>()
                .eq(OshResourceRevenueRecord::getContributionId, contribution.getId())
                .eq(OshResourceRevenueRecord::getOrderNo, orderNo)
                .eq(OshResourceRevenueRecord::getDeleteFlag, 0)) > 0) {
            return;
        }
        OshResourceRevenueRecord record = new OshResourceRevenueRecord();
        record.setId(GenerateUtil.generateSnowflakeId());
        record.setContributionId(contribution.getId());
        record.setOrderId(orderId);
        record.setOrderNo(orderNo);
        record.setBuyerUserId(buyerUserId);
        record.setRevenueAmount(revenueAmount == null ? BigDecimal.ZERO : revenueAmount);
        record.setPointAmount(pointAmount == null ? 0L : pointAmount);
        record.setBizType(bizType);
        record.setRevenueTime(LocalDateTime.now());
        record.setCreateTime(LocalDateTime.now());
        record.setCreateBy(buyerUserId);
        record.setDeleteFlag(0);
        revenueRecordMapper.insert(record);
    }

    private boolean isInternalLevel(Integer level) {
        return level != null && level >= MIN_INTERNAL_LEVEL && level <= MAX_INTERNAL_LEVEL;
    }

    private Integer parseLevel(String level) {
        try {
            return level == null ? null : Integer.valueOf(level);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
