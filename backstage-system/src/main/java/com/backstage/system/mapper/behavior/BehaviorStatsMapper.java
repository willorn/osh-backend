package com.backstage.system.mapper.behavior;

import com.backstage.system.domain.behavior.BehaviorEventQuery;
import com.backstage.system.domain.behavior.ContributionQuery;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface BehaviorStatsMapper {
    Long selectEventCount(@Param("query") BehaviorEventQuery query);

    List<Map<String, Object>> selectEventPage(@Param("query") BehaviorEventQuery query,
                                              @Param("offset") Integer offset,
                                              @Param("limit") Integer limit);

    Map<String, Object> selectSummary(@Param("query") BehaviorEventQuery query);

    List<Map<String, Object>> selectTrend(@Param("query") BehaviorEventQuery query, @Param("grain") String grain);

    List<Map<String, Object>> selectModuleDistribution(@Param("query") BehaviorEventQuery query);

    List<Map<String, Object>> selectActionDistribution(@Param("query") BehaviorEventQuery query);

    List<Map<String, Object>> selectActiveUserRank(@Param("query") BehaviorEventQuery query);

    List<Map<String, Object>> selectContributionSummary(@Param("query") ContributionQuery query);

    List<Map<String, Object>> selectContributionResources(@Param("query") ContributionQuery query);

    List<Map<String, Object>> selectContributionRevenueDetails(@Param("query") ContributionQuery query);
}
