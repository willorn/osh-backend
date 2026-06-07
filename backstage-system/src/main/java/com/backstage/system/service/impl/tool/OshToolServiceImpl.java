package com.backstage.system.service.impl.tool;

import com.backstage.common.enums.ResourceCodePrefixEnum;
import com.backstage.common.enums.ResourceTypeEnum;
import com.backstage.common.exception.ServiceException;
import com.backstage.system.domain.tool.OshTool;
import com.backstage.system.domain.tool.OshToolPackage;
import com.backstage.system.domain.tool.OshToolTag;
import com.backstage.system.domain.tool.ToolUsagePermission;
import com.backstage.system.domain.user.OshUser;
import com.backstage.system.mapper.tool.OshToolCollectionMapper;
import com.backstage.system.mapper.tool.OshToolMapper;
import com.backstage.system.mapper.tool.OshToolPackageMapper;
import com.backstage.system.mapper.tool.OshToolTagMapper;
import com.backstage.system.mapper.tool.OshToolVoteMapper;
import com.backstage.system.domain.tool.OshToolVote;
import com.backstage.system.request.tool.ToolPackageSaveRequest;
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
import com.github.pagehelper.PageHelper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class OshToolServiceImpl implements IOshToolService {

    private static final int ACCESS_TYPE_INTERNAL = 1;
    private static final int MAX_TOOL_TAG_COUNT = 3;
    private static final String DEFAULT_RESOURCE_TYPE = "FREE";
    private static final String RESOURCE_TYPE_CASH_ONLY = "CASH_ONLY";
    private static final String RESOURCE_TYPE_CASH_POINT = "CASH_POINT";
    private static final int PAY_TYPE_CASH = 1;
    private static final int PAY_TYPE_CASH_POINT = 3;
    private static final int RECOMMEND_PAGE_SIZE = 5;
    private static final int RECOMMEND_TAG_LIMIT = 5;
    private static final int VOTE_TYPE_GOOD = 1;
    private static final int VOTE_TYPE_BAD = 3;
    private static final int LEVEL_FREE = 1;
    private static final int LEVEL_CASH_ONLY = 2;
    private static final int LEVEL_CASH_POINT = 3;
    private static final int LEVEL_VIP = 4;
    private static final int LEVEL_SMALL_CLASS = 5;
    private static final int LEVEL_INTERNAL = 6;

    @Autowired
    private OshToolMapper oshToolMapper;

    @Autowired
    private OshToolTagMapper oshToolTagMapper;

    @Autowired
    private OshToolPackageMapper oshToolPackageMapper;

    @Autowired
    private OshToolCollectionMapper oshToolCollectionMapper;

    @Autowired
    private OshToolVoteMapper oshToolVoteMapper;

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
    public List<OshToolTag> listAvailableTags() {
        return oshToolTagMapper.selectAvailableTags();
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
        if (request.getPackages() != null) {
            syncToolPackages(tool.getId(), resolvePackagesByResourceType(request), operator.getUsername());
        }
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
        if (request.getPackages() != null) {
            syncToolPackages(tool.getId(), resolvePackagesByResourceType(request), operator.getUsername());
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
        tool.setPackages(oshToolPackageMapper.selectPackagesByToolId(toolId));
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
        if (oshToolMapper.consumeUserToolQuota(toolId, userId, operator) <= 0) {
            throw new ServiceException("工具使用次数不足");
        }
        oshToolMapper.increaseTotalUsage(toolId);
        saveToolIndexEvent(toolId, ToolIndexEventType.TOOL_INDEX_COUNTER, operator);
        return oshToolMapper.selectUserRemainingCount(toolId, userId);
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

    private String generateToolNo() {
        return resourceNoGenerator.generateUniqueNo(ResourceCodePrefixEnum.TOOL, no -> oshToolMapper.countByNo(no) > 0);
    }

    private OshTool buildTool(ToolSaveRequest request, String operator) {
        validateAccessTarget(request);
        OshTool tool = new OshTool();
        String resourceType = StringUtils.defaultIfBlank(request.getResourceType(), DEFAULT_RESOURCE_TYPE);
        tool.setToolName(request.getToolName());
        tool.setDescription(request.getDescription());
        tool.setLogoUrl(null);
        tool.setAccessType(ACCESS_TYPE_INTERNAL);
        tool.setRoutePath(request.getRoutePath());
        tool.setIframeUrl(null);
        tool.setGithubUrl(request.getGithubUrl());
        tool.setPrice(BigDecimal.ZERO);
        tool.setOriginalPrice(BigDecimal.ZERO);
        tool.setPointCost(0);
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

    private void syncToolPackages(Long toolId, List<ToolPackageSaveRequest> packages, String operator) {
        if (packages == null) {
            return;
        }
        oshToolPackageMapper.softDeletePackagesByToolId(toolId, operator);
        if (packages.isEmpty()) {
            return;
        }
        for (ToolPackageSaveRequest request : packages) {
            if (request == null) {
                continue;
            }
            validateToolPackage(request);
            OshToolPackage toolPackage = buildToolPackage(toolId, request, operator);
            if (request.getId() == null) {
                if (oshToolPackageMapper.insertToolPackage(toolPackage) <= 0) {
                    throw new ServiceException("新增工具套餐失败");
                }
            } else if (oshToolPackageMapper.updateToolPackage(toolPackage) <= 0) {
                throw new ServiceException("修改工具套餐失败");
            }
        }
    }

    private OshToolPackage buildToolPackage(Long toolId, ToolPackageSaveRequest request, String operator) {
        OshToolPackage toolPackage = new OshToolPackage();
        toolPackage.setId(request.getId());
        toolPackage.setToolId(toolId);
        toolPackage.setPackageName(StringUtils.defaultIfBlank(request.getPackageName(), request.getUseCount() + "次使用套餐"));
        toolPackage.setUseCount(request.getUseCount());
        toolPackage.setPrice(request.getPrice() == null ? BigDecimal.ZERO : request.getPrice());
        toolPackage.setPointCost(resolvePackagePointCost(request));
        toolPackage.setPayType(resolvePackagePayType(request));
        toolPackage.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        toolPackage.setSortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder());
        toolPackage.setCreateBy(operator);
        toolPackage.setUpdateBy(operator);
        return toolPackage;
    }

    private void validateToolPackage(ToolPackageSaveRequest request) {
        if (request.getUseCount() == null || request.getUseCount() <= 0) {
            throw new IllegalArgumentException("套餐使用次数必须大于0");
        }
        if (request.getPrice() != null && request.getPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("套餐价格不能小于0");
        }
        if (request.getPrice() == null || request.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("套餐现金金额必须大于0");
        }
        int payType = request.getPayType() == null ? PAY_TYPE_CASH : request.getPayType();
        if (payType != PAY_TYPE_CASH && payType != PAY_TYPE_CASH_POINT) {
            throw new IllegalArgumentException("套餐支付类型错误");
        }
        if (payType == PAY_TYPE_CASH_POINT && (request.getPointCost() == null || request.getPointCost() <= 0)) {
            throw new IllegalArgumentException("现金+积分套餐的积分金额必须大于0");
        }
        if (payType == PAY_TYPE_CASH && request.getPointCost() != null && request.getPointCost() < 0) {
            throw new IllegalArgumentException("套餐积分金额不能小于0");
        }
    }

    private List<ToolPackageSaveRequest> resolvePackagesByResourceType(ToolSaveRequest request) {
        if (!isPackageEnabledResourceType(request.getResourceType())) {
            return Collections.emptyList();
        }
        return request.getPackages();
    }

    private boolean isPackageEnabledResourceType(String resourceType) {
        return RESOURCE_TYPE_CASH_ONLY.equals(resourceType) || RESOURCE_TYPE_CASH_POINT.equals(resourceType);
    }

    private Integer resolvePackagePointCost(ToolPackageSaveRequest request) {
        if (request.getPayType() == null || PAY_TYPE_CASH == request.getPayType()) {
            return 0;
        }
        return request.getPointCost() == null ? 0 : request.getPointCost();
    }

    private Integer resolvePackagePayType(ToolPackageSaveRequest request) {
        return request.getPayType() == null ? PAY_TYPE_CASH : request.getPayType();
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
        if (!isPackageEnabledResourceType(tool.getResourceType())) {
            permission.setDeductAllowed(false);
            permission.setRemainingCount(0);
            permission.setMessage("允许使用");
            return permission;
        }
        Integer remainingCount = oshToolMapper.selectUserRemainingCount(tool.getId(), userId);
        int value = remainingCount == null ? 0 : remainingCount;
        permission.setRemainingCount(value);
        permission.setDeductAllowed(value > 0);
        permission.setMessage(value > 0 ? "允许使用" : "工具使用次数不足");
        return permission;
    }

    private Integer resolveLevelByResourceType(String resourceType) {
        String normalizedType = StringUtils.trimToEmpty(resourceType).toUpperCase();
        switch (normalizedType) {
            case DEFAULT_RESOURCE_TYPE:
                return LEVEL_FREE;
            case RESOURCE_TYPE_CASH_ONLY:
                return LEVEL_CASH_ONLY;
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

    private void fillToolPackages(List<OshTool> tools) {
        if (tools == null || tools.isEmpty()) {
            return;
        }
        List<Long> toolIds = new ArrayList<>();
        for (OshTool tool : tools) {
            toolIds.add(tool.getId());
        }
        List<OshToolPackage> packages = oshToolPackageMapper.selectPackagesByToolIds(toolIds);
        Map<Long, List<OshToolPackage>> packageMap = new HashMap<>();
        for (OshToolPackage toolPackage : packages) {
            packageMap.computeIfAbsent(toolPackage.getToolId(), key -> new ArrayList<>()).add(toolPackage);
        }
        for (OshTool tool : tools) {
            tool.setPackages(packageMap.getOrDefault(tool.getId(), Collections.emptyList()));
        }
    }

    private void fillToolExtras(List<OshTool> tools) {
        for (OshTool tool : tools) {
            tool.setTags(oshToolTagMapper.selectTagNamesByToolId(tool.getId()));
        }
        fillToolPackages(tools);
    }

    private void fillUserToolQuota(OshTool tool, Long userId) {
        if (userId == null) {
            tool.setRemainingCount(0);
            tool.setPurchasedFlag(0);
            return;
        }
        Integer remainingCount = oshToolMapper.selectUserRemainingCount(tool.getId(), userId);
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
