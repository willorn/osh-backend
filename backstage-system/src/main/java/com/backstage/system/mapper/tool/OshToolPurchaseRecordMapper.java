package com.backstage.system.mapper.tool;

import com.backstage.system.domain.tool.OshToolPurchaseRecord;
import com.backstage.system.domain.vo.tool.ToolPurchaseListVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface OshToolPurchaseRecordMapper {

    int insertToolPurchaseRecord(OshToolPurchaseRecord record);

    OshToolPurchaseRecord selectByOrderNo(@Param("orderNo") String orderNo);

    OshToolPurchaseRecord selectByPaymentNo(@Param("paymentNo") String paymentNo);

    List<ToolPurchaseListVO> selectPurchaseRecordsByUserId(@Param("userId") Long userId);

    int updateOrderStatusByOrderNo(@Param("orderNo") String orderNo,
                                   @Param("orderStatus") Integer orderStatus,
                                   @Param("operator") String operator);

    int updateGrantSuccess(@Param("id") Long id,
                           @Param("grantTime") LocalDateTime grantTime,
                           @Param("operator") String operator);

    int updateGrantFailed(@Param("id") Long id,
                          @Param("remark") String remark,
                          @Param("operator") String operator);
}
