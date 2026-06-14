package com.backstage.system.mapper.tool;

import com.backstage.system.domain.vo.tool.ToolQuotaCurrentVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface OshToolQuotaMapper {

    int increaseUserGlobalQuota(@Param("userId") Long userId,
                                @Param("addCount") Integer addCount,
                                @Param("operator") String operator);

    int insertUserGlobalQuota(@Param("userId") Long userId,
                              @Param("initCount") Integer initCount,
                              @Param("operator") String operator);

    ToolQuotaCurrentVO selectUserGlobalQuotaByUserId(@Param("userId") Long userId);

    List<Long> selectUserIdsWithoutGlobalQuota();

    int batchInsertInitialGlobalQuota(@Param("userIds") List<Long> userIds,
                                      @Param("initCount") Integer initCount,
                                      @Param("operator") String operator);
}
