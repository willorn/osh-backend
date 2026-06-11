package com.backstage.system.service.tool;

import com.backstage.system.domain.tool.OshTool;
import com.backstage.system.domain.tool.OshToolTag;
import com.backstage.system.domain.tool.ToolUsagePermission;
import com.backstage.system.domain.user.OshUser;
import com.backstage.system.domain.vo.tool.ToolCalculatorResultVO;
import com.backstage.system.request.tool.ToolRecommendRequest;
import com.backstage.system.request.tool.ToolCalculatorRequest;
import com.backstage.system.request.tool.ToolSaveRequest;
import com.backstage.system.request.tool.ToolSearchRequest;
import com.backstage.system.domain.vo.tool.ToolQuotaCurrentVO;

import java.util.List;

public interface IOshToolService {

    List<OshTool> pageQuerySearchTool(Long userId, ToolSearchRequest request);

    List<OshTool> listRecommendTools(Long userId, ToolRecommendRequest request);

    List<OshToolTag> listAvailableTags(String keyword);

    List<OshToolTag> listRecommendTags(int limit);

    Long createTool(ToolSaveRequest request, OshUser operator);

    Long updateTool(ToolSaveRequest request, OshUser operator);

    void deleteToolsByIds(List<Long> ids, OshUser operator);

    OshTool getToolDetail(Long toolId, Long userId);

    ToolUsagePermission checkToolUsagePermission(Long userId, Integer userLevel, Long toolId);

    Boolean canUseTool(Long userId, Long toolId);

    ToolCalculatorResultVO calculateTool(Long userId, ToolCalculatorRequest request);

    Integer consumeToolUsage(Long userId, Integer userLevel, String operator, Long toolId);

    Integer voteTool(Long userId, String operator, Long toolId, Integer type);

    void recordToolView(Long toolId);

    int fillMissingToolNo();

    int initMissingUserToolQuota(String operator);

    ToolQuotaCurrentVO getCurrentUserToolQuota(Long userId);
}
