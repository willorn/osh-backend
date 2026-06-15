package com.backstage.system.service.assistant.support;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.backstage.system.domain.assistant.AssistantFeedback;
import com.backstage.system.domain.assistant.AssistantFeedbackCategory;
import com.backstage.system.domain.assistant.AssistantTicketStatus;
import com.backstage.system.domain.assistant.vo.*;
import com.backstage.system.domain.user.OshUser;
import com.backstage.system.mapper.user.OshUserMapper;
import com.backstage.system.service.assistant.IAssistantFeedbackCategoryService;
import com.backstage.system.service.assistant.IAssistantFeedbackProcessRecordService;
import com.backstage.system.service.assistant.IAssistantFeedbackTagService;
import com.backstage.system.service.common.OssService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 反馈视图装配器。
 * <p>
 * 负责将领域模型 {@link AssistantFeedback} 转换为各种视图对象(VO)，
 * 用于不同场景的反馈数据展示：
 * <ul>
 *   <li>列表展示 - {@link AssistantFeedbackListVO}</li>
 *   <li>详情展示 - {@link AssistantFeedbackDetailVO}</li>
 *   <li>通用展示 - {@link AssistantFeedbackVO}</li>
 * </ul>
 *
 * @author backstage
 */
@Component
public class AssistantFeedbackViewAssembler {

    private static final Logger log = LoggerFactory.getLogger(AssistantFeedbackViewAssembler.class);

    /**
     * 反馈分类服务，用于获取分类信息
     */
    private final IAssistantFeedbackCategoryService categoryService;

    /**
     * 反馈处理记录服务，用于获取处理历史
     */
    private final IAssistantFeedbackProcessRecordService processRecordService;

    /**
     * 反馈标签服务，用于获取标签信息
     */
    private final IAssistantFeedbackTagService feedbackTagService;

    /**
     * 用户数据访问层，用于获取用户信息
     */
    private final OshUserMapper oshUserMapper;

    /**
     * OSS服务，用于生成图片临时访问URL
     */
    private final OssService ossService;

    public AssistantFeedbackViewAssembler(IAssistantFeedbackCategoryService categoryService,
                                         IAssistantFeedbackProcessRecordService processRecordService,
                                         IAssistantFeedbackTagService feedbackTagService,
                                          OshUserMapper oshUserMapper,
                                          OssService ossService) {
        this.categoryService = categoryService;
        this.processRecordService = processRecordService;
        this.feedbackTagService = feedbackTagService;
        this.oshUserMapper = oshUserMapper;
        this.ossService = ossService;
    }

    /**
     * 将反馈实体转换为通用视图对象。
     * <p>
     * 适用于单条反馈的通用展示场景，会自动查询并填充关联数据。
     * </p>
     *
     * @param feedback 反馈实体
     * @return 通用视图对象，包含完整的反馈信息和关联数据
     */
    public AssistantFeedbackVO toFeedbackVO(AssistantFeedback feedback) {
        return toFeedbackVO(feedback, null);
    }

    /**
     * 将反馈实体转换为通用视图对象（带标签缓存）。
     * <p>
     * 适用于批量转换场景，可通过传入预加载的标签映射避免重复查询。
     * 填充的数据包括：
     * <ul>
     *   <li>发布者信息（昵称、头像）</li>
     *   <li>处理者信息</li>
     *   <li>状态信息（标准化状态码）</li>
     *   <li>处理结果</li>
     *   <li>处理摘要（最后处理时间、关闭原因等）</li>
     *   <li>分类信息（编码、名称、图标）</li>
     *   <li>标签列表</li>
     * </ul>
     * </p>
     *
     * @param feedback 反馈实体
     * @param feedbackTagMap 反馈ID到标签列表的映射，可为null
     * @return 通用视图对象
     */
    public AssistantFeedbackVO toFeedbackVO(AssistantFeedback feedback, Map<Long, List<AssistantFeedbackTagVO>> feedbackTagMap) {
        AssistantFeedbackVO feedbackVO = new AssistantFeedbackVO();
        BeanUtils.copyProperties(feedback, feedbackVO);

        // 解析图片JSON数组并转换为可访问的URL
        if (StrUtil.isNotBlank(feedback.getImages())) {
            try {
                JSONArray jsonArray = JSON.parseArray(feedback.getImages());
                List<String> imagePaths = jsonArray.toJavaList(String.class);
                // 将相对路径转换为临时访问URL（7天有效期）
                List<String> imageUrls = imagePaths.stream()
                        .map(path -> ossService.getLimitedUrl(path, 7 * 24 * 60))
                        .collect(Collectors.toList());
                feedbackVO.setImages(imageUrls);
            } catch (Exception e) {
                // JSON解析失败，设置为空列表
                feedbackVO.setImages(Collections.emptyList());
            }
        } else {
            feedbackVO.setImages(Collections.emptyList());
        }

        fillFeedbackUserInfo(feedbackVO, feedback.getUserId());
        fillFeedbackHandlerInfo(feedbackVO, feedback.getHandlerId(), feedback.getHandlerName());
        feedbackVO.setStatus(AssistantTicketStatus.normalize(feedback.getStatus()));
        feedbackVO.setResult(StrUtil.blankToDefault(feedback.getResult(), ""));
        fillFeedbackProcessSummary(feedbackVO, listProcessRecordsSafely(feedback.getId()));

        AssistantFeedbackCategory category = categoryService.getById(feedback.getCategoryId());
        if (category != null) {
            feedbackVO.setCategoryCode(category.getCode());
            feedbackVO.setCategoryName(category.getName());
            feedbackVO.setCategoryIcon(category.getIcon());
        }
        feedbackVO.setTags(resolveFeedbackTags(feedback.getId(), feedbackTagMap));
        return feedbackVO;
    }

    /**
     * 将反馈实体转换为列表视图对象。
     * <p>
     * 适用于列表展示场景，数据较通用视图更精简。
     * 使用预加载的用户、分类、标签映射提升批量转换性能。
     * 包含内容预览（前100字符）而非完整内容。
     * </p>
     *
     * @param feedback 反馈实体
     * @param userMap 用户ID到用户实体的映射
     * @param categoryMap 分类ID到分类实体的映射
     * @param feedbackTagMap 反馈ID到标签列表的映射
     * @return 列表视图对象
     */
    public AssistantFeedbackListVO toFeedbackListVO(AssistantFeedback feedback,
                                                    Map<Long, OshUser> userMap,
                                                    Map<Long, AssistantFeedbackCategory> categoryMap,
                                                    Map<Long, List<AssistantFeedbackTagVO>> feedbackTagMap) {
        AssistantFeedbackListVO feedbackListVO = new AssistantFeedbackListVO();
        BeanUtils.copyProperties(feedback, feedbackListVO);
        feedbackListVO.setStatus(AssistantTicketStatus.normalize(feedback.getStatus()));
        feedbackListVO.setStatusText(AssistantTicketStatus.getDescriptionByCode(feedbackListVO.getStatus()));
        fillFeedbackListUserInfo(feedbackListVO, userMap.get(feedback.getUserId()));

        AssistantFeedbackCategory category = categoryMap.get(feedback.getCategoryId());
        if (category != null) {
            feedbackListVO.setCategoryCode(category.getCode());
            feedbackListVO.setCategoryName(category.getName());
            feedbackListVO.setCategoryIcon(category.getIcon());
        }

        feedbackListVO.setContentPreview(buildContentPreview(feedback.getContent()));
        feedbackListVO.setTags(feedbackTagMap.getOrDefault(feedback.getId(), Collections.emptyList()));
        return feedbackListVO;
    }

    /**
     * 将反馈实体转换为详情视图对象。
     * <p>
     * 适用于详情页展示，包含最完整的反馈信息。
     * 填充的数据包括：
     * <ul>
     *   <li>完整的反馈字段</li>
     *   <li>发布者和处理者信息</li>
     *   <li>状态及状态描述文本</li>
     *   <li>处理记录列表</li>
     *   <li>处理摘要</li>
     *   <li>分类信息及是否允许评论标识</li>
     *   <li>标签列表</li>
     *   <li>当前浏览次数</li>
     * </ul>
     * </p>
     *
     * @param feedback 反馈实体
     * @param processRecords 处理记录列表
     * @param currentViewCount 当前浏览次数
     * @return 详情视图对象
     */
    public AssistantFeedbackDetailVO toFeedbackDetailVO(AssistantFeedback feedback,
                                                        List<AssistantFeedbackProcessRecordVO> processRecords,
                                                        int currentViewCount) {
        AssistantFeedbackDetailVO detailVO = BeanUtil.copyProperties(feedback, AssistantFeedbackDetailVO.class);

        // 解析图片JSON数组并转换为可访问的URL
        if (StrUtil.isNotBlank(feedback.getImages())) {
            try {
                JSONArray jsonArray = JSON.parseArray(feedback.getImages());
                List<String> imagePaths = jsonArray.toJavaList(String.class);
                // 将相对路径转换为临时访问URL（7天有效期）
                List<String> imageUrls = imagePaths.stream()
                        .map(path -> ossService.getLimitedUrl(path, 7 * 24 * 60))
                        .collect(Collectors.toList());
                detailVO.setImages(imageUrls);
            } catch (Exception e) {
                // JSON解析失败，设置为空列表
                detailVO.setImages(Collections.emptyList());
            }
        } else {
            detailVO.setImages(Collections.emptyList());
        }

        fillFeedbackDetailUserInfo(detailVO, feedback.getUserId());
        fillFeedbackDetailHandlerInfo(detailVO, feedback.getHandlerId(), feedback.getHandlerName());
        detailVO.setStatus(AssistantTicketStatus.normalize(feedback.getStatus()));
        detailVO.setStatusText(AssistantTicketStatus.getDescriptionByCode(detailVO.getStatus()));
        detailVO.setProcessRecords(processRecords);
        fillFeedbackProcessSummary(detailVO, processRecords);

        AssistantFeedbackCategory category = categoryService.getById(feedback.getCategoryId());
        if (category != null) {
            detailVO.setCategoryCode(category.getCode());
            detailVO.setCategoryName(category.getName());
            detailVO.setCategoryIcon(category.getIcon());
            detailVO.setAllowComment(category.getAllowComment());
        }
        detailVO.setTags(buildFeedbackTagMap(Collections.singletonList(feedback))
                .getOrDefault(feedback.getId(), Collections.emptyList()));
        detailVO.setViewCount(currentViewCount);
        return detailVO;
    }

    /**
     * 构建反馈列表对应的用户映射。
     * <p>
     * 批量查询用户表，避免N+1查询问题。
     * </p>
     *
     * @param feedbackList 反馈列表
     * @return 用户ID到用户实体的映射
     */
    public Map<Long, OshUser> buildUserMap(List<AssistantFeedback> feedbackList) {
        Set<Long> userIds = feedbackList.stream()
                .map(AssistantFeedback::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return oshUserMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(OshUser::getId, Function.identity()));
    }

    /**
     * 构建反馈列表对应的分类映射。
     * <p>
     * 批量查询分类表，避免N+1查询问题。
     * </p>
     *
     * @param feedbackList 反馈列表
     * @return 分类ID到分类实体的映射
     */
    public Map<Long, AssistantFeedbackCategory> buildCategoryMap(List<AssistantFeedback> feedbackList) {
        Set<Long> categoryIds = feedbackList.stream()
                .map(AssistantFeedback::getCategoryId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (categoryIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return categoryService.lambdaQuery()
                .in(AssistantFeedbackCategory::getId, categoryIds)
                .list()
                .stream()
                .collect(Collectors.toMap(AssistantFeedbackCategory::getId, Function.identity()));
    }

    /**
     * 构建反馈列表对应的标签映射。
     * <p>
     * 批量查询标签关联表，按反馈ID分组返回。
     * 使用LinkedHashSet保持反馈ID的顺序。
     * </p>
     *
     * @param feedbackList 反馈列表
     * @return 反馈ID到标签列表的映射
     */
    public Map<Long, List<AssistantFeedbackTagVO>> buildFeedbackTagMap(List<AssistantFeedback> feedbackList) {
        Set<Long> feedbackIds = feedbackList.stream()
                .map(AssistantFeedback::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return feedbackTagService.mapFeedbackTags(feedbackIds);
    }

    /**
     * 安全地查询反馈处理记录。
     * <p>
     * 捕获并记录查询异常，避免单条记录查询失败影响整体流程。
     * </p>
     *
     * @param feedbackId 反馈ID
     * @return 处理记录列表，查询失败返回空列表
     */
    public List<AssistantFeedbackProcessRecordVO> listProcessRecordsSafely(Long feedbackId) {
        try {
            return processRecordService.listByFeedbackId(feedbackId);
        } catch (Exception exception) {
            log.warn("查询反馈处理记录失败，feedbackId={}, message={}", feedbackId, exception.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 填充通用视图的用户信息。
     *
     * @param feedbackVO 通用视图对象
     * @param userId 用户ID
     */
    private void fillFeedbackUserInfo(AssistantFeedbackVO feedbackVO, Long userId) {
        OshUser user = getUserById(userId);
        if (user == null) {
            return;
        }
        feedbackVO.setUserName(resolveUserDisplayName(user, feedbackVO.getUserName()));
        feedbackVO.setUserAvatar(user.getAvatar());
    }

    /**
     * 填充列表视图的用户信息。
     *
     * @param feedbackListVO 列表视图对象
     * @param user 用户实体
     */
    private void fillFeedbackListUserInfo(AssistantFeedbackListVO feedbackListVO, OshUser user) {
        if (user == null) {
            return;
        }
        feedbackListVO.setUserName(resolveUserDisplayName(user, feedbackListVO.getUserName()));
        feedbackListVO.setUserAvatar(user.getAvatar());
    }

    /**
     * 填充详情视图的用户信息。
     *
     * @param detailVO 详情视图对象
     * @param userId 用户ID
     */
    private void fillFeedbackDetailUserInfo(AssistantFeedbackDetailVO detailVO, Long userId) {
        OshUser user = getUserById(userId);
        if (user == null) {
            return;
        }
        detailVO.setUserName(resolveUserDisplayName(user, detailVO.getUserName()));
        detailVO.setUserAvatar(user.getAvatar());
    }

    /**
     * 填充通用视图的处理者信息。
     *
     * @param feedbackVO 通用视图对象
     * @param handlerId 处理者ID
     * @param fallbackHandlerName 默认处理者名称
     */
    private void fillFeedbackHandlerInfo(AssistantFeedbackVO feedbackVO, Long handlerId, String fallbackHandlerName) {
        OshUser handler = getUserById(handlerId);
        feedbackVO.setHandlerName(resolveUserDisplayName(handler, fallbackHandlerName));
    }

    /**
     * 填充详情视图的处理者信息。
     *
     * @param detailVO 详情视图对象
     * @param handlerId 处理者ID
     * @param fallbackHandlerName 默认处理者名称
     */
    private void fillFeedbackDetailHandlerInfo(AssistantFeedbackDetailVO detailVO, Long handlerId, String fallbackHandlerName) {
        OshUser handler = getUserById(handlerId);
        detailVO.setHandlerName(resolveUserDisplayName(handler, fallbackHandlerName));
    }

    /**
     * 构建内容预览文本。
     * <p>
     * 将换行符替换为空格，截取前100个字符作为预览。
     * </p>
     *
     * @param content 原始内容
     * @return 预览文本，最长100字符
     */
    private String buildContentPreview(String content) {
        if (StrUtil.isBlank(content)) {
            return "";
        }
        String normalizedContent = StrUtil.replace(content.trim(), "\n", " ");
        return normalizedContent.length() <= 100 ? normalizedContent : normalizedContent.substring(0, 100);
    }

    /**
     * 解析反馈的标签列表。
     * <p>
     * 优先使用传入的标签映射，未命中时查询数据库。
     * </p>
     *
     * @param feedbackId 反馈ID
     * @param feedbackTagMap 标签映射缓存，可为null
     * @return 标签列表
     */
    private List<AssistantFeedbackTagVO> resolveFeedbackTags(Long feedbackId, Map<Long, List<AssistantFeedbackTagVO>> feedbackTagMap) {
        if (feedbackTagMap != null) {
            return feedbackTagMap.getOrDefault(feedbackId, Collections.emptyList());
        }
        return feedbackTagService.mapFeedbackTags(Collections.singleton(feedbackId))
                .getOrDefault(feedbackId, Collections.emptyList());
    }

    /**
     * 根据ID查询用户。
     *
     * @param userId 用户ID
     * @return 用户实体，ID为null时返回null
     */
    private OshUser getUserById(Long userId) {
        return userId == null ? null : oshUserMapper.selectById(userId);
    }

    /**
     * 解析用户显示名称。
     * <p>
     * 优先使用昵称，昵称为空时使用用户名，
     * 都为空时使用默认值。
     * </p>
     *
     * @param user 用户实体
     * @param fallbackName 默认名称
     * @return 显示名称
     */
    private String resolveUserDisplayName(OshUser user, String fallbackName) {
        if (user == null) {
            return fallbackName;
        }
        return StrUtil.isNotBlank(user.getUsername()) ? user.getUsername() : "匿名用户";
    }

    /**
     * 填充通用视图的处理摘要信息。
     * <p>
     * 从处理记录中提取：
     * <ul>
     *   <li>最后处理时间</li>
     *   <li>关闭原因（如已关闭）</li>
     *   <li>处理人名称（如未设置）</li>
     * </ul>
     * </p>
     *
     * @param feedbackVO 通用视图对象
     * @param processRecords 处理记录列表
     */
    private void fillFeedbackProcessSummary(AssistantFeedbackVO feedbackVO, List<AssistantFeedbackProcessRecordVO> processRecords) {
        if (processRecords == null || processRecords.isEmpty()) {
            return;
        }
        AssistantFeedbackProcessRecordVO latestRecord = processRecords.stream()
                .filter(record -> record.getCreateTime() != null)
                .max(Comparator.comparing(AssistantFeedbackProcessRecordVO::getCreateTime))
                .orElse(processRecords.get(processRecords.size() - 1));
        feedbackVO.setHandledTime(latestRecord.getCreateTime());
        if (AssistantTicketStatus.CLOSED.getCode().equals(latestRecord.getToStatus())) {
            feedbackVO.setCloseReason(latestRecord.getRemark());
        }
        if (StrUtil.isBlank(feedbackVO.getHandlerName())) {
            feedbackVO.setHandlerName(latestRecord.getOperatorName());
        }
    }

    /**
     * 填充详情视图的处理摘要信息。
     * <p>
     * 从处理记录中提取：
     * <ul>
     *   <li>最后处理时间</li>
     *   <li>关闭原因（如已关闭）</li>
     *   <li>处理人名称（如未设置）</li>
     * </ul>
     * </p>
     *
     * @param detailVO 详情视图对象
     * @param processRecords 处理记录列表
     */
    private void fillFeedbackProcessSummary(AssistantFeedbackDetailVO detailVO, List<AssistantFeedbackProcessRecordVO> processRecords) {
        if (processRecords == null || processRecords.isEmpty()) {
            return;
        }
        AssistantFeedbackProcessRecordVO latestRecord = processRecords.stream()
                .filter(record -> record.getCreateTime() != null)
                .max(Comparator.comparing(AssistantFeedbackProcessRecordVO::getCreateTime))
                .orElse(processRecords.get(processRecords.size() - 1));
        detailVO.setHandledTime(latestRecord.getCreateTime());
        if (AssistantTicketStatus.CLOSED.getCode().equals(latestRecord.getToStatus())) {
            detailVO.setCloseReason(latestRecord.getRemark());
        }
        if (StrUtil.isBlank(detailVO.getHandlerName())) {
            detailVO.setHandlerName(latestRecord.getOperatorName());
        }
    }
}
