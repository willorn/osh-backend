package com.backstage.system.mapper.member;

import com.backstage.system.domain.member.OshMemberOrder;
import com.backstage.system.domain.member.vo.MemberOrderVO;
import com.backstage.system.domain.member.vo.MemberStatusVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface OshMemberOrderMapper extends BaseMapper<OshMemberOrder> {
    OshMemberOrder selectByOrderNoForUpdate(@Param("orderNo") String orderNo);
    OshMemberOrder selectByOrderNoAndUserId(@Param("orderNo") String orderNo, @Param("userId") Long userId);
    List<MemberOrderVO> selectMemberOrders(@Param("userId") Long userId);
    MemberStatusVO selectMemberStatus(@Param("userId") Long userId, @Param("roleCode") String roleCode);
    Integer selectRoleIdByCode(@Param("roleCode") String roleCode);
    LocalDateTime selectRoleExpireForUpdate(@Param("userId") Long userId, @Param("roleId") Integer roleId);
    int upsertUserRole(@Param("userId") Long userId,
                       @Param("roleId") Integer roleId,
                       @Param("expireTime") LocalDateTime expireTime,
                       @Param("operatorId") Long operatorId);
    int updatePaid(@Param("orderNo") String orderNo, @Param("payTime") LocalDateTime payTime);
    int updateGrantSuccess(@Param("id") Long id,
                           @Param("startTime") LocalDateTime startTime,
                           @Param("expireTime") LocalDateTime expireTime,
                           @Param("grantTime") LocalDateTime grantTime);
    int updateGrantFailed(@Param("id") Long id, @Param("grantMessage") String grantMessage);
}
