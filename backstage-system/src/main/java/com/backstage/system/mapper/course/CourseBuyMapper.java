package com.backstage.system.mapper.course;

import java.math.BigDecimal;
import java.util.Map;
import org.apache.ibatis.annotations.Param;

/**
 * Mapper for course purchase records in osh_course_buy.
 */
public interface CourseBuyMapper {

    int countPaidByUserAndCourse(@Param("userId") Long userId, @Param("courseId") Long courseId);

    int countPaidByOrderNoAndUserId(@Param("orderNo") String orderNo, @Param("userId") Long userId);

    int upsertPendingOrder(@Param("userId") Long userId,
                           @Param("courseId") Long courseId,
                           @Param("orderNo") String orderNo,
                           @Param("payMethod") String payMethod,
                           @Param("payPrice") BigDecimal payPrice,
                           @Param("originPrice") BigDecimal originPrice);

    Map<String, Object> selectByOrderNoAndUserId(@Param("orderNo") String orderNo, @Param("userId") Long userId);

    int markPaidByOrderNoAndUserId(@Param("orderNo") String orderNo, @Param("userId") Long userId);
}
