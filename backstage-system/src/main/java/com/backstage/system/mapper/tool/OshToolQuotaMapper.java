package com.backstage.system.mapper.tool;

import com.backstage.system.domain.vo.tool.ToolQuotaCurrentVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface OshToolQuotaMapper {

    int increaseUserQuota(@Param("userId") Long userId,
                          @Param("addCount") Integer addCount,
                          @Param("operator") String operator);

    int insertUserQuota(@Param("userId") Long userId,
                        @Param("initCount") Integer initCount,
                        @Param("operator") String operator);

    ToolQuotaCurrentVO selectUserQuotaByUserId(@Param("userId") Long userId);

    List<Long> selectUserIdsWithoutQuota();

    int batchInsertInitialQuota(@Param("userIds") List<Long> userIds,
                                @Param("initCount") Integer initCount,
                                @Param("operator") String operator);
}
