package com.backstage.system.mapper.tool;

import com.backstage.system.domain.tool.OshTool;
import com.backstage.system.request.tool.ToolRecommendRequest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface OshToolMapper {

    List<OshTool> pageQuerySearchTool(@Param("request") com.backstage.system.request.tool.ToolSearchRequest request,
                                      @Param("userId") Long userId);

    List<OshTool> selectAllToolsForEsSync();

    List<OshTool> selectToolsWithMissingNo();

    List<OshTool> selectRecommendTools(@Param("request") ToolRecommendRequest request,
                                       @Param("userId") Long userId);

    OshTool selectToolById(@Param("id") Long id);

    Integer selectUserGlobalRemainingCount(@Param("userId") Long userId);

    int consumeUserGlobalQuota(@Param("userId") Long userId,
                               @Param("quotaCost") Integer quotaCost,
                               @Param("operator") String operator);

    int increaseTotalUsage(@Param("toolId") Long toolId);

    int increaseViewCount(@Param("toolId") Long toolId);

    int increaseGoodCount(@Param("toolId") Long toolId);

    int decreaseGoodCount(@Param("toolId") Long toolId);

    int increaseBadCount(@Param("toolId") Long toolId);

    int decreaseBadCount(@Param("toolId") Long toolId);

    int insertTool(OshTool tool);

    int updateTool(OshTool tool);

    int updateToolNoById(@Param("id") Long id, @Param("no") String no);

    int countByNo(@Param("no") String no);

    int deleteToolsByIds(@Param("ids") List<Long> ids, @Param("operator") String operator);

    int increaseCollectionCount(@Param("toolId") Long toolId);

    int decreaseCollectionCount(@Param("toolId") Long toolId);
}
