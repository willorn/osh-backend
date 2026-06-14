package com.backstage.system.service.impl.tool;

import com.backstage.common.enums.ResourceCodePrefixEnum;
import com.backstage.common.enums.ResourceTypeEnum;
import com.backstage.common.exception.ServiceException;
import com.backstage.system.domain.tool.OshTool;
import com.backstage.system.domain.tool.OshToolTag;
import com.backstage.system.domain.tool.ToolUsagePermission;
import com.backstage.system.domain.user.OshUser;
import com.backstage.system.domain.vo.tool.ToolCalculatorResultVO;
import com.backstage.system.domain.vo.tool.ToolQuotaCurrentVO;
import com.backstage.system.mapper.tool.OshToolCollectionMapper;
import com.backstage.system.mapper.tool.OshToolMapper;
import com.backstage.system.mapper.tool.OshToolQuotaMapper;
import com.backstage.system.mapper.tool.OshToolTagMapper;
import com.backstage.system.mapper.tool.OshToolVoteMapper;
import com.backstage.system.domain.tool.OshToolVote;
import com.backstage.system.request.tool.ToolCalculatorRequest;
import com.backstage.system.request.tool.ToolRecommendRequest;
import com.backstage.system.request.tool.ToolSaveRequest;
import com.backstage.system.request.tool.ToolSearchRequest;
import com.backstage.system.service.OutboxEventService;
import com.backstage.system.service.tool.IOshToolEsService;
import com.backstage.system.service.tool.IOshToolService;
import com.backstage.system.service.tool.ResourceNoGenerator;
import com.backstage.system.service.tool.ToolIndexDeleteMessage;
import com.backstage.system.service.tool.ToolIndexEventType;
import com.backstage.system.service.tool.ToolIndexMessage;
import com.backstage.system.utils.ResourcePermissionUtil;
import com.backstage.system.utils.UserContextUtil;
import com.github.pagehelper.PageHelper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class OshToolServiceImpl implements IOshToolService {

    private static final int ACCESS_TYPE_INTERNAL = 1;
    private static final int MAX_TOOL_TAG_COUNT = 3;
    private static final String DEFAULT_RESOURCE_TYPE = "FREE";
    private static final String RESOURCE_TYPE_CASH_POINT = "CASH_POINT";
    private static final int RECOMMEND_PAGE_SIZE = 5;
    private static final int RECOMMEND_TAG_LIMIT = 5;
    private static final int VOTE_TYPE_GOOD = 1;
    private static final int VOTE_TYPE_BAD = 3;
    private static final int DEFAULT_INIT_TOOL_QUOTA = 10;
    private static final int LEVEL_FREE = 1;
    private static final int LEVEL_CASH_POINT = 3;
    private static final int LEVEL_VIP = 4;
    private static final int LEVEL_SMALL_CLASS = 5;
    private static final int LEVEL_INTERNAL = 6;

    @Autowired
    private OshToolMapper oshToolMapper;

    @Autowired
    private OshToolTagMapper oshToolTagMapper;

    @Autowired
    private OshToolCollectionMapper oshToolCollectionMapper;

    @Autowired
    private OshToolVoteMapper oshToolVoteMapper;

    @Autowired
    private OshToolQuotaMapper oshToolQuotaMapper;

    @Autowired
    private IOshToolEsService oshToolEsService;

    @Autowired
    private OutboxEventService outboxEventService;

    @Autowired
    private ResourceNoGenerator resourceNoGenerator;

    @Override
    public List<OshTool> pageQuerySearchTool(Long userId, ToolSearchRequest request) {
        normalizeSearchRequest(request);
        PageHelper.startPage(request.getPageNum(), request.getPageSize());
        List<OshTool> list = oshToolMapper.pageQuerySearchTool(request, userId);
        fillToolExtras(list);
        return list;
    }

    @Override
    public List<OshTool> listRecommendTools(Long userId, ToolRecommendRequest request) {
        normalizeRecommendRequest(request);
        PageHelper.startPage(request.getPageNum(), request.getPageSize());
        List<OshTool> list = oshToolMapper.selectRecommendTools(request, userId);
        fillToolExtras(list);
        return list;
    }

    @Override
    public List<OshToolTag> listAvailableTags(String keyword) {
        return oshToolTagMapper.selectAvailableTags(StringUtils.trimToEmpty(keyword));
    }

    @Override
    public List<OshToolTag> listRecommendTags(int limit) {
        int safeLimit = limit <= 0 ? RECOMMEND_TAG_LIMIT : Math.min(limit, RECOMMEND_TAG_LIMIT);
        return oshToolTagMapper.selectRecommendTags(safeLimit);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createTool(ToolSaveRequest request, OshUser operator) {
        if (StringUtils.isBlank(request.getToolName())) {
            throw new IllegalArgumentException("工具名称不能为空");
        }
        validateToolTags(request.getTags());
        OshTool tool = buildTool(request, operator.getUsername());
        tool.setNo(generateToolNo());
        if (oshToolMapper.insertTool(tool) <= 0) {
            throw new ServiceException("新增工具失败");
        }
        syncToolTags(tool.getId(), request.getTags(), operator.getUsername());
        saveToolIndexEvent(tool.getId(), ToolIndexEventType.TOOL_INDEX_CREATE, operator);
        return tool.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long updateTool(ToolSaveRequest request, OshUser operator) {
        if (request.getId() == null) {
            throw new IllegalArgumentException("工具ID不能为空");
        }
        validateToolTags(request.getTags());
        validateToolExists(request.getId());
        OshTool tool = buildTool(request, operator.getUsername());
        tool.setId(request.getId());
        if (oshToolMapper.updateTool(tool) <= 0) {
            throw new ServiceException("修改工具失败");
        }
        if (request.getTags() != null) {
            syncToolTags(tool.getId(), request.getTags(), operator.getUsername());
        }
        saveToolIndexEvent(tool.getId(), ToolIndexEventType.TOOL_INDEX_UPDATE, operator);
        return tool.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteToolsByIds(List<Long> ids, OshUser operator) {
        if (ids == null || ids.isEmpty()) {
            throw new IllegalArgumentException("请选择要删除的工具");
        }
        oshToolMapper.deleteToolsByIds(ids, operator.getUsername());
        for (Long id : ids) {
            oshToolTagMapper.softDeleteRelationsByToolId(id, operator.getUsername());
            outboxEventService.saveToolIndexDeleteEvent(id, new ToolIndexDeleteMessage(id), operator);
        }
    }

    @Override
    public OshTool getToolDetail(Long toolId, Long userId) {
        OshTool tool = oshToolMapper.selectToolById(toolId);
        if (tool == null) {
            return null;
        }
        tool.setTags(oshToolTagMapper.selectTagNamesByToolId(toolId));
        tool.setCollectionFlag(resolveCollectionFlag(toolId, userId));
        fillUserToolQuota(tool, userId);
        tool.setVoteType(resolveVoteType(toolId, userId));
        return tool;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer consumeToolUsage(Long userId, Integer userLevel, String operator, Long toolId) {
        if (userId == null) {
            throw new IllegalArgumentException("请先登录");
        }
        if (toolId == null) {
            throw new IllegalArgumentException("工具ID不能为空");
        }
        OshTool tool = oshToolMapper.selectToolById(toolId);
        if (tool == null) {
            throw new ServiceException("工具不存在");
        }
        ToolUsagePermission permission = buildToolUsagePermission(tool, userId, userLevel);
        if (!Boolean.TRUE.equals(permission.getUseAllowed())) {
            throw new ServiceException(permission.getMessage());
        }
        if (!isPackageEnabledResourceType(tool.getResourceType())) {
            return 0;
        }
        if (!Boolean.TRUE.equals(permission.getDeductAllowed())) {
            throw new ServiceException(permission.getMessage());
        }
        Integer consumeCount = resolveConsumeCount(tool);
        if (oshToolMapper.consumeUserQuota(userId, consumeCount, operator) <= 0) {
            throw new ServiceException("工具点数不足");
        }
        oshToolMapper.increaseTotalUsage(toolId);
        saveToolIndexEvent(toolId, ToolIndexEventType.TOOL_INDEX_COUNTER, operator);
        return oshToolMapper.selectUserRemainingCount(userId);
    }

    @Override
    public ToolUsagePermission checkToolUsagePermission(Long userId, Integer userLevel, Long toolId) {
        if (userId == null) {
            throw new IllegalArgumentException("请先登录");
        }
        if (toolId == null) {
            throw new IllegalArgumentException("工具ID不能为空");
        }
        OshTool tool = oshToolMapper.selectToolById(toolId);
        if (tool == null) {
            throw new ServiceException("工具不存在");
        }
        return buildToolUsagePermission(tool, userId, userLevel);
    }

    @Override
    public Boolean canUseTool(Long userId, Long toolId) {
        if (userId == null) {
            throw new IllegalArgumentException("请先登录");
        }
        if (toolId == null) {
            throw new IllegalArgumentException("工具ID不能为空");
        }
        OshTool tool = oshToolMapper.selectToolById(toolId);
        if (tool == null) {
            throw new ServiceException("工具不存在");
        }
        if (!isPackageEnabledResourceType(tool.getResourceType())) {
            return true;
        }
        Integer currentLevel = UserContextUtil.getCurrentLevelSafely();
        int requiredLevel = tool.getLevel() == null ? 0 : tool.getLevel();
        if (currentLevel != null && currentLevel > requiredLevel) {
            return true;
        }
        Integer remainingCount = oshToolMapper.selectUserRemainingCount(userId);
        int value = remainingCount == null ? 0 : remainingCount;
        return value >= resolveConsumeCount(tool);
    }

    @Override
    public ToolCalculatorResultVO calculateTool(Long userId, ToolCalculatorRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("计算参数不能为空");
        }
        if (!Boolean.TRUE.equals(canUseTool(userId, request.getToolId()))) {
            throw new ServiceException("工具点数不足");
        }
        OshTool tool = oshToolMapper.selectToolById(request.getToolId());
        if (tool == null) {
            throw new ServiceException("工具不存在");
        }
        OshUser currentUser = UserContextUtil.getCurrentUser();
        String currentOperator = currentUser == null ? "system" : currentUser.getUsername();
        int currentLevel = UserContextUtil.getCurrentLevelSafely();
        int requiredLevel = tool.getLevel() == null ? 0 : tool.getLevel();
        if (isPackageEnabledResourceType(tool.getResourceType()) && currentLevel <= requiredLevel) {
            Integer consumeCount = resolveConsumeCount(tool);
            if (oshToolMapper.consumeUserQuota(userId, consumeCount, currentOperator) <= 0) {
                throw new ServiceException("工具点数不足");
            }
        }
        oshToolMapper.increaseTotalUsage(request.getToolId());
        saveToolIndexEvent(request.getToolId(), ToolIndexEventType.TOOL_INDEX_COUNTER, currentOperator);
        BigDecimal leftValue = request.getLeftValue();
        BigDecimal rightValue = request.getRightValue();
        String operator = StringUtils.trimToEmpty(request.getOperator());
        if (leftValue == null || rightValue == null) {
            throw new IllegalArgumentException("计算数字不能为空");
        }
        ToolCalculatorResultVO resultVO = new ToolCalculatorResultVO();
        switch (operator) {
            case "+":
                resultVO.setResult(leftValue.add(rightValue));
                break;
            case "-":
                resultVO.setResult(leftValue.subtract(rightValue));
                break;
            case "*":
                resultVO.setResult(leftValue.multiply(rightValue));
                break;
            case "/":
                if (BigDecimal.ZERO.compareTo(rightValue) == 0) {
                    throw new IllegalArgumentException("除数不能为0");
                }
                resultVO.setResult(leftValue.divide(rightValue, 8, RoundingMode.HALF_UP).stripTrailingZeros());
                break;
            default:
                throw new IllegalArgumentException("暂不支持该运算符");
        }
        return resultVO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer voteTool(Long userId, String operator, Long toolId, Integer type) {
        if (userId == null) {
            throw new IllegalArgumentException("请先登录");
        }
        if (toolId == null) {
            throw new IllegalArgumentException("工具ID不能为空");
        }
        if (!Integer.valueOf(VOTE_TYPE_GOOD).equals(type) && !Integer.valueOf(VOTE_TYPE_BAD).equals(type)) {
            throw new IllegalArgumentException("评价类型错误");
        }
        validateToolExists(toolId);

        OshToolVote existVote = oshToolVoteMapper.selectByUserIdAndToolId(userId, toolId);
        if (existVote != null && Integer.valueOf(0).equals(existVote.getDeleteFlag())) {
            if (type.equals(existVote.getType())) {
                oshToolVoteMapper.deleteToolVote(existVote.getId(), operator);
                decreaseVoteCount(toolId, type);
                saveToolIndexEvent(toolId, ToolIndexEventType.TOOL_INDEX_COUNTER, operator);
                return 0;
            }
            decreaseVoteCount(toolId, existVote.getType());
            existVote.setType(type);
            existVote.setUpdateBy(operator);
            oshToolVoteMapper.updateToolVote(existVote);
            increaseVoteCount(toolId, type);
            saveToolIndexEvent(toolId, ToolIndexEventType.TOOL_INDEX_COUNTER, operator);
            return type;
        }

        if (existVote != null) {
            existVote.setType(type);
            existVote.setUpdateBy(operator);
            oshToolVoteMapper.updateToolVote(existVote);
        } else {
            OshToolVote vote = new OshToolVote();
            vote.setUserId(userId);
            vote.setToolId(toolId);
            vote.setType(type);
            vote.setCreateBy(operator);
            vote.setUpdateBy(operator);
            oshToolVoteMapper.insertToolVote(vote);
        }
        increaseVoteCount(toolId, type);
        saveToolIndexEvent(toolId, ToolIndexEventType.TOOL_INDEX_COUNTER, operator);
        return type;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordToolView(Long toolId) {
        if (toolId == null) {
            throw new IllegalArgumentException("工具ID不能为空");
        }
        if (oshToolMapper.increaseViewCount(toolId) <= 0) {
            throw new ServiceException("工具不存在");
        }
        saveToolIndexEvent(toolId, ToolIndexEventType.TOOL_INDEX_COUNTER, (String) null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int fillMissingToolNo() {
        List<OshTool> toolList = oshToolMapper.selectToolsWithMissingNo();
        if (toolList == null || toolList.isEmpty()) {
            return 0;
        }
        int updatedCount = 0;
        for (OshTool tool : toolList) {
            if (tool == null || tool.getId() == null) {
                continue;
            }
            String no = generateToolNo();
            updatedCount += oshToolMapper.updateToolNoById(tool.getId(), no);
        }
        return updatedCount;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int initMissingUserToolQuota(String operator) {
        List<Long> userIds = oshToolQuotaMapper.selectUserIdsWithoutQuota();
        if (userIds == null || userIds.isEmpty()) {
            return 0;
        }
        return oshToolQuotaMapper.batchInsertInitialQuota(userIds, DEFAULT_INIT_TOOL_QUOTA, operator);
    }

    @Override
    public ToolQuotaCurrentVO getCurrentUserToolQuota(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("请先登录");
        }
        ToolQuotaCurrentVO quota = oshToolQuotaMapper.selectUserQuotaByUserId(userId);
        if (quota != null) {
            return quota;
        }
        ToolQuotaCurrentVO emptyQuota = new ToolQuotaCurrentVO();
        emptyQuota.setRemainingCount(0);
        emptyQuota.setTotalBuyCount(0);
        emptyQuota.setUsedCount(0);
        return emptyQuota;
    }

    private String generateToolNo() {
        return resourceNoGenerator.generateUniqueNo(ResourceCodePrefixEnum.TOOL, no -> oshToolMapper.countByNo(no) > 0);
    }

    private OshTool buildTool(ToolSaveRequest request, String operator) {
        validateAccessTarget(request);
        OshTool tool = new OshTool();
        String resourceType = StringUtils.defaultIfBlank(request.getResourceType(), DEFAULT_RESOURCE_TYPE);
        tool.setToolName(request.getToolName());
        tool.setDescription(request.getDescription());
        tool.setAccessType(ACCESS_TYPE_INTERNAL);
        tool.setRoutePath(request.getRoutePath());
        tool.setIframeUrl(null);
        tool.setGithubUrl(request.getGithubUrl());
        tool.setQuotaCost(resolveQuotaCost(request, resourceType));
        tool.setStatus(request.getId() == null ? 2 : request.getStatus());
        tool.setRemark(request.getRemark());
        tool.setResourceType(resourceType);
        tool.setLevel(resolveLevelByResourceType(resourceType));
        tool.setCreateBy(operator);
        tool.setUpdateBy(operator);
        return tool;
    }

    private void normalizeSearchRequest(ToolSearchRequest request) {
        if (request.getPageNum() <= 0) {
            request.setPageNum(1);
        }
        if (request.getPageSize() <= 0) {
            request.setPageSize(10);
        }
        if (StringUtils.isNotBlank(request.getNo())) {
            request.setToolId(null);
            request.setKeyword(null);
            request.setTags(null);
            request.setResourceType(null);
            request.setIsFollowing(false);
            request.setCollectionFlag(null);
        }
        if (request.getCollectionFlag() == null && Boolean.TRUE.equals(request.getIsFollowing())) {
            request.setCollectionFlag(1);
        }
    }

    private void normalizeRecommendRequest(ToolRecommendRequest request) {
        if (request.getPageNum() <= 0) {
            request.setPageNum(1);
        }
        request.setPageSize(RECOMMEND_PAGE_SIZE);
        if (!"LATEST".equals(request.getType())) {
            request.setType("HOT");
        }
    }

    private void validateAccessTarget(ToolSaveRequest request) {
        if (StringUtils.isBlank(request.getRoutePath())) {
            throw new IllegalArgumentException("站内工具前端路由不能为空");
        }
    }

    private void validateToolTags(List<String> tags) {
        if (tags != null && tags.size() > MAX_TOOL_TAG_COUNT) {
            throw new IllegalArgumentException("工具标签最多添加" + MAX_TOOL_TAG_COUNT + "个");
        }
    }

    private void validateToolExists(Long toolId) {
        OshTool tool = oshToolMapper.selectToolById(toolId);
        if (tool == null) {
            throw new ServiceException("工具不存在");
        }
    }

    private void syncToolTags(Long toolId, List<String> tags, String operator) {
        oshToolTagMapper.softDeleteRelationsByToolId(toolId, operator);
        List<String> normalizedTags = tags == null ? Collections.emptyList() : tags;
        for (String tagName : normalizedTags) {
            OshToolTag tag = getOrCreateTag(tagName, operator);
            oshToolTagMapper.insertToolTagRel(toolId, tag.getId(), operator);
            oshToolTagMapper.increaseUseCount(tag.getId());
        }
    }

    private OshToolTag getOrCreateTag(String tagName, String operator) {
        OshToolTag tag = oshToolTagMapper.selectByName(tagName);
        if (tag != null) {
            if (!Integer.valueOf(0).equals(tag.getDeleteFlag()) || !Integer.valueOf(1).equals(tag.getStatus())) {
                oshToolTagMapper.activateToolTag(tag.getId(), operator);
            }
            return tag;
        }
        OshToolTag newTag = new OshToolTag();
        newTag.setName(tagName);
        newTag.setCreateBy(operator);
        newTag.setUpdateBy(operator);
        if (oshToolTagMapper.insertToolTag(newTag) <= 0) {
            throw new ServiceException("新增工具标签失败");
        }
        return newTag;
    }

    private boolean isPackageEnabledResourceType(String resourceType) {
        return RESOURCE_TYPE_CASH_POINT.equals(resourceType);
    }

    private ToolUsagePermission buildToolUsagePermission(OshTool tool, Long userId, Integer userLevel) {
        ToolUsagePermission permission = new ToolUsagePermission();
        if (!hasToolPermission(tool.getId(), userLevel, tool.getLevel())) {
            permission.setUseAllowed(false);
            permission.setDeductAllowed(false);
            permission.setRemainingCount(0);
            permission.setMessage("用户权限不足");
            return permission;
        }
        permission.setUseAllowed(true);
        int currentLevel = userLevel == null ? 0 : userLevel;
        int requiredLevel = tool.getLevel() == null ? 0 : tool.getLevel();
        if (!isPackageEnabledResourceType(tool.getResourceType()) || currentLevel > requiredLevel) {
            permission.setDeductAllowed(false);
            permission.setRemainingCount(0);
            permission.setMessage("允许免费使用");
            return permission;
        }
        Integer remainingCount = oshToolMapper.selectUserRemainingCount(userId);
        int value = remainingCount == null ? 0 : remainingCount;
        int consumeCount = resolveConsumeCount(tool);
        permission.setRemainingCount(value);
        permission.setDeductAllowed(value >= consumeCount);
        permission.setMessage(value >= consumeCount ? "允许使用" : "工具点数不足");
        return permission;
    }

    private Integer resolveQuotaCost(ToolSaveRequest request, String resourceType) {
        if (!isPackageEnabledResourceType(resourceType)) {
            return 0;
        }
        if (request.getQuotaCost() == null || request.getQuotaCost() <= 0) {
            throw new IllegalArgumentException("按次数消费的工具，单次消耗工具点数必须大于0");
        }
        return request.getQuotaCost();
    }

    private Integer resolveConsumeCount(OshTool tool) {
        Integer quotaCost = tool.getQuotaCost();
        return quotaCost == null || quotaCost <= 0 ? 1 : quotaCost;
    }

    private Integer resolveLevelByResourceType(String resourceType) {
        String normalizedType = StringUtils.trimToEmpty(resourceType).toUpperCase();
        switch (normalizedType) {
            case DEFAULT_RESOURCE_TYPE:
                return LEVEL_FREE;
            case RESOURCE_TYPE_CASH_POINT:
                return LEVEL_CASH_POINT;
            case "VIP":
                return LEVEL_VIP;
            case "SMALL_CLASS":
                return LEVEL_SMALL_CLASS;
            case "INTERNAL":
                return LEVEL_INTERNAL;
            default:
                throw new IllegalArgumentException("资源类型不支持: " + resourceType);
        }
    }

    private boolean hasToolPermission(Long toolId, Integer userLevel, Integer requiredLevel) {
        try {
            return Boolean.TRUE.equals(ResourcePermissionUtil.hasPermission(ResourceTypeEnum.TOOL, toolId));
        } catch (Exception ex) {
            int level = userLevel == null ? 0 : userLevel;
            int resourceLevel = requiredLevel == null ? 0 : requiredLevel;
            return level >= resourceLevel;
        }
    }

    private void fillToolExtras(List<OshTool> tools) {
        for (OshTool tool : tools) {
            tool.setTags(oshToolTagMapper.selectTagNamesByToolId(tool.getId()));
        }
    }

    private void fillUserToolQuota(OshTool tool, Long userId) {
        if (userId == null) {
            tool.setRemainingCount(0);
            tool.setPurchasedFlag(0);
            return;
        }
        Integer remainingCount = oshToolMapper.selectUserRemainingCount(userId);
        int value = remainingCount == null ? 0 : remainingCount;
        tool.setRemainingCount(value);
        tool.setPurchasedFlag(value > 0 ? 1 : 0);
    }

    private Integer resolveCollectionFlag(Long toolId, Long userId) {
        if (userId == null) {
            return 0;
        }
        List<Long> ids = oshToolCollectionMapper.selectActiveToolIdsByUserIdAndToolIds(userId, Collections.singletonList(toolId));
        return ids.contains(toolId) ? 1 : 0;
    }

    private Integer resolveVoteType(Long toolId, Long userId) {
        if (userId == null) {
            return 0;
        }
        Integer voteType = oshToolVoteMapper.selectVoteType(userId, toolId);
        return voteType == null ? 0 : voteType;
    }

    private void increaseVoteCount(Long toolId, Integer type) {
        if (Integer.valueOf(VOTE_TYPE_GOOD).equals(type)) {
            oshToolMapper.increaseGoodCount(toolId);
            return;
        }
        if (Integer.valueOf(VOTE_TYPE_BAD).equals(type)) {
            oshToolMapper.increaseBadCount(toolId);
        }
    }

    private void decreaseVoteCount(Long toolId, Integer type) {
        if (Integer.valueOf(VOTE_TYPE_GOOD).equals(type)) {
            oshToolMapper.decreaseGoodCount(toolId);
            return;
        }
        if (Integer.valueOf(VOTE_TYPE_BAD).equals(type)) {
            oshToolMapper.decreaseBadCount(toolId);
        }
    }

    private boolean isExternalUrl(String url) {
        return StringUtils.startsWithIgnoreCase(url, "http://") || StringUtils.startsWithIgnoreCase(url, "https://");
    }

    private void saveToolIndexEvent(Long toolId, String eventType, OshUser operator) {
        ToolIndexMessage message = oshToolEsService.buildIndexMessage(toolId, eventType);
        outboxEventService.saveToolIndexEvent(toolId, message, operator);
    }

    private void saveToolIndexEvent(Long toolId, String eventType, String operator) {
        ToolIndexMessage message = oshToolEsService.buildIndexMessage(toolId, eventType);
        outboxEventService.saveToolIndexEvent(toolId, message, operator);
    }
}
