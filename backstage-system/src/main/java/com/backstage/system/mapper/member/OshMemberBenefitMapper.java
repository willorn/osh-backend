package com.backstage.system.mapper.member;

import com.backstage.system.domain.member.OshMemberBenefit;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface OshMemberBenefitMapper extends BaseMapper<OshMemberBenefit> {
    List<OshMemberBenefit> selectEnabledByPlanIds(@Param("planIds") List<Long> planIds);
    List<OshMemberBenefit> selectByPlanIds(@Param("planIds") List<Long> planIds);
    int softDeleteByPlanId(@Param("planId") Long planId, @Param("operatorId") Long operatorId);
}
