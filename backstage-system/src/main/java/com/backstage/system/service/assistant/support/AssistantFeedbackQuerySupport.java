package com.backstage.system.service.assistant.support;

import cn.hutool.core.util.StrUtil;
import com.backstage.system.domain.assistant.AssistantFeedback;
import com.backstage.system.domain.assistant.AssistantFeedbackCategory;
import com.backstage.system.domain.assistant.AssistantTicketStatus;
import com.backstage.system.domain.assistant.dto.AssistantFeedbackPageDTO;
import com.backstage.system.mapper.assistant.AssistantFeedbackMapper;
import com.backstage.system.service.assistant.IAssistantFeedbackCategoryService;
import com.backstage.system.service.assistant.IAssistantFeedbackFavoriteService;
import com.backstage.system.service.assistant.IAssistantFeedbackTagService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 反馈分页查询支持类。
 * <p>
 * 负责处理用户反馈的分页查询逻辑，支持多维度筛选条件：
 * <ul>
 *   <li>标签筛选 - 根据标签ID过滤反馈</li>
 *   <li>查询模式 - 支持全部(all)、我的(mine)、收藏(favorite)三种模式</li>
 *   <li>分类筛选 - 按反馈分类查询（公告已迁出到 osh_announcement，统一不再掺入反馈列表）</li>
 *   <li>状态筛选 - 按工单状态过滤</li>
 *   <li>置顶筛选 - 筛选置顶/非置顶反馈</li>
 *   <li>关键词搜索 - 标题和内容模糊匹配</li>
 *   <li>排序 - 支持热度、评论数、最新时间排序</li>
 * </ul>
 * </p>
 *
 * @author backstage
 */
@Component
public class AssistantFeedbackQuerySupport {

    /**
     * 反馈数据访问层
     */
    private final AssistantFeedbackMapper feedbackMapper;

    /**
     * 反馈分类服务
     */
    private final IAssistantFeedbackCategoryService categoryService;

    /**
     * 反馈收藏服务
     */
    private final IAssistantFeedbackFavoriteService favoriteService;

    /**
     * 反馈标签服务
     */
    private final IAssistantFeedbackTagService feedbackTagService;

    public AssistantFeedbackQuerySupport(AssistantFeedbackMapper feedbackMapper,
                                         IAssistantFeedbackCategoryService categoryService,
                                         IAssistantFeedbackFavoriteService favoriteService,
                                         IAssistantFeedbackTagService feedbackTagService) {
        this.feedbackMapper = feedbackMapper;
        this.categoryService = categoryService;
        this.favoriteService = favoriteService;
        this.feedbackTagService = feedbackTagService;
    }

    /**
     * 执行反馈分页查询。
     * <p>
     * 查询流程：
     * <ol>
     *   <li>根据标签ID筛选反馈ID集合</li>
     *   <li>根据查询模式筛选反馈ID集合（全部/我的/收藏）</li>
     *   <li>取两个集合的交集作为最终筛选条件</li>
     *   <li>构建并执行分页查询</li>
     * </ol>
     * </p>
     *
     * @param dto 分页查询参数，包含页码、页大小、筛选条件、排序方式等
     * @return 分页结果，包含当前页数据及总记录数
     */
    public Page<AssistantFeedback> pageFeedback(AssistantFeedbackPageDTO dto) {
        Set<Long> tagFilteredFeedbackIds = filterFeedbackIdsByTags(dto.getTagIds());
        Set<Long> scopedFeedbackIds = filterFeedbackIdsByQueryMode(dto);
        Set<Long> filteredFeedbackIds = intersectFeedbackIds(tagFilteredFeedbackIds, scopedFeedbackIds);
        if (shouldReturnEmptyPage(tagFilteredFeedbackIds, scopedFeedbackIds, filteredFeedbackIds)) {
            return new Page<>(dto.getPageNum(), dto.getPageSize(), 0);
        }

        LambdaQueryWrapper<AssistantFeedback> queryWrapper = Wrappers.lambdaQuery(AssistantFeedback.class)
                .eq(AssistantFeedback::getDeleteFlag, (byte) 0)
                .in(filteredFeedbackIds != null && !filteredFeedbackIds.isEmpty(), AssistantFeedback::getId, filteredFeedbackIds)
                .eq(dto.getCategoryId() != null, AssistantFeedback::getCategoryId, dto.getCategoryId())
                .eq(StrUtil.isNotBlank(dto.getStatus()), AssistantFeedback::getStatus, AssistantTicketStatus.normalize(dto.getStatus()))
                .eq(dto.getIsPinned() != null, AssistantFeedback::getIsPinned, dto.getIsPinned())
                .and(StrUtil.isNotBlank(dto.getKeyword()), wrapper -> wrapper
                        .like(AssistantFeedback::getTitle, dto.getKeyword())
                        .or()
                        .like(AssistantFeedback::getContent, dto.getKeyword()))
                .last(buildOrderBySql(dto));

        // 公告已迁移至 osh_announcement，反馈列表统一排除遗留公告分类下的数据
        Long announcementCategoryId = resolveAnnouncementCategoryId();
        if (announcementCategoryId != null) {
            queryWrapper.ne(AssistantFeedback::getCategoryId, announcementCategoryId);
        }

        return feedbackMapper.selectPage(new Page<>(dto.getPageNum(), dto.getPageSize()), queryWrapper);
    }

    /**
     * 根据标签ID列表筛选反馈ID集合。
     * <p>
     * 当标签列表为空时返回null表示不过滤，
     * 否则返回同时包含所有指定标签的反馈ID集合。
     * </p>
     *
     * @param tagIds 标签ID列表
     * @return 符合条件的反馈ID集合，null表示不过滤
     */
    private Set<Long> filterFeedbackIdsByTags(List<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return null;
        }
        return feedbackTagService.listFeedbackIdsByTagIds(tagIds);
    }

    /**
     * 根据查询模式筛选反馈ID集合。
     * <p>
     * 支持三种查询模式：
     * <ul>
     *   <li>all - 返回null表示查询全部，不过滤</li>
     *   <li>mine - 查询当前用户发布的反馈</li>
     *   <li>favorite - 查询当前用户收藏的反馈</li>
     * </ul>
     * 当用户未登录时返回空集合。
     * </p>
     *
     * @param dto 包含查询模式和用户ID的查询参数
     * @return 符合条件的反馈ID集合，null表示查询全部
     */
    private Set<Long> filterFeedbackIdsByQueryMode(AssistantFeedbackPageDTO dto) {
        String queryMode = normalizeQueryMode(dto.getQueryMode());
        if ("all".equals(queryMode)) {
            return null;
        }
        Long userId = dto.getUserId();
        if (userId == null) {
            return Collections.emptySet();
        }
        if ("mine".equals(queryMode)) {
            return feedbackMapper.selectList(Wrappers.<AssistantFeedback>lambdaQuery()
                            .select(AssistantFeedback::getId)
                            .eq(AssistantFeedback::getUserId, userId)
                            .eq(AssistantFeedback::getDeleteFlag, (byte) 0))
                    .stream()
                    .map(AssistantFeedback::getId)
                    .collect(Collectors.toSet());
        }
        if ("favorite".equals(queryMode)) {
            return favoriteService.listFavoriteFeedbackIds(userId);
        }
        return null;
    }

    /**
     * 获取遗留"公告"反馈分类的 ID（历史数据，反馈页面应永远排除）。
     *
     * @return 公告分类ID，不存在则返回null
     */
    private Long resolveAnnouncementCategoryId() {
        AssistantFeedbackCategory announcementCategory = categoryService.lambdaQuery()
                .eq(AssistantFeedbackCategory::getCode, "announcement")
                .one();
        return announcementCategory == null ? null : announcementCategory.getId();
    }

    /**
     * 构建排序SQL片段。
     * <p>
     * 排序优先级：
     * <ol>
     *   <li>置顶状态降序（置顶在前）</li>
     *   <li>置顶排序升序（置顶顺序）</li>
     *   <li>根据sortType指定的排序方式（白名单校验，防SQL注入）</li>
     * </ol>
     * 支持的排序类型：hot(热度)、comment(评论数)、latest(最新)、related(最相关)
     * </p>
     *
     * @param dto 分页查询参数
     * @return 完整的ORDER BY SQL语句
     */
    private String buildOrderBySql(AssistantFeedbackPageDTO dto) {
        boolean searchMode = StrUtil.isNotBlank(dto.getKeyword());
        String resolvedSortType = resolveSortType(dto.getSortType(), searchMode);
        String queryMode = normalizeQueryMode(dto.getQueryMode());
        // 前缀：置顶优先 + mine模式待确认优先
        String minePrioritySql = !searchMode && "mine".equals(queryMode) ? "CASE WHEN status = 'PENDING_CONFIRM' THEN 0 ELSE 1 END ASC, " : "";
        String pinOrderPrefix = !searchMode ? "ORDER BY " + minePrioritySql + "is_pinned DESC, pin_order ASC, " : "ORDER BY ";

        // 白名单校验 sortType，防止 SQL 注入
        switch (resolvedSortType) {
            case "related":
                String escapedKeyword = escapeSqlLike(dto.getKeyword());
                return pinOrderPrefix + "CASE WHEN title LIKE '%" + escapedKeyword + "%' THEN 0 ELSE 1 END ASC, "
                        + "hot_score DESC, create_time DESC";
            case "hot":
                return pinOrderPrefix + "hot_score DESC, create_time DESC";
            case "comment":
                return pinOrderPrefix + "comment_count DESC, create_time DESC";
            case "latest":
                return pinOrderPrefix + "create_time DESC";
            default:
                return pinOrderPrefix + "hot_score DESC, create_time DESC";
        }
    }

    private String resolveSortType(String sortType, boolean searchMode) {
        String resolvedSortType = StrUtil.isBlank(sortType) ? "hot" : sortType.trim().toLowerCase();
        if ("related".equals(resolvedSortType) && !searchMode) {
            return "hot";
        }
        return resolvedSortType;
    }

    /**
     * 转义 SQL LIKE 特殊字符，防止注入和通配符逃逸。
     * 转义字符：\ % _ '
     */
    private String escapeSqlLike(String keyword) {
        if (StrUtil.isBlank(keyword)) {
            return "";
        }
        return keyword.replace("\\", "\\\\")
                .replace("'", "''")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }

    /**
     * 规范化查询模式参数。
     * <p>
     * 将查询模式转换为小写并去除首尾空格，
     * 空值默认为"all"。
     * </p>
     *
     * @param queryMode 原始查询模式
     * @return 规范化后的查询模式
     */
    private String normalizeQueryMode(String queryMode) {
        if (StrUtil.isBlank(queryMode)) {
            return "all";
        }
        return queryMode.trim().toLowerCase();
    }

    /**
     * 计算两个反馈ID集合的交集。
     * <p>
     * 当任一集合为null时，返回另一个集合（null表示不过滤）。
     * </p>
     *
     * @param leftIds 第一个集合（标签筛选结果）
     * @param rightIds 第二个集合（查询模式筛选结果）
     * @return 两个集合的交集
     */
    private Set<Long> intersectFeedbackIds(Set<Long> leftIds, Set<Long> rightIds) {
        if (leftIds == null) {
            return rightIds;
        }
        if (rightIds == null) {
            return leftIds;
        }
        return leftIds.stream().filter(rightIds::contains).collect(Collectors.toSet());
    }

    /**
     * 判断是否应该返回空分页结果。
     * <p>
     * 当标签筛选结果为空、查询模式筛选结果为空、
     * 或两者交集为空时，直接返回空分页避免无效查询。
     * </p>
     *
     * @param tagFilteredFeedbackIds 标签筛选结果
     * @param scopedFeedbackIds 查询模式筛选结果
     * @param filteredFeedbackIds 交集结果
     * @return true表示应该返回空分页
     */
    private boolean shouldReturnEmptyPage(Set<Long> tagFilteredFeedbackIds, Set<Long> scopedFeedbackIds, Set<Long> filteredFeedbackIds) {
        boolean tagFilterEmpty = tagFilteredFeedbackIds != null && tagFilteredFeedbackIds.isEmpty();
        boolean scopeFilterEmpty = scopedFeedbackIds != null && scopedFeedbackIds.isEmpty();
        boolean intersectionEmpty = filteredFeedbackIds != null && filteredFeedbackIds.isEmpty();
        return tagFilterEmpty || scopeFilterEmpty || intersectionEmpty;
    }
}
