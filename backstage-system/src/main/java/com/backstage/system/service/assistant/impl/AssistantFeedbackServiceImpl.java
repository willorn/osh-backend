package com.backstage.system.service.assistant.impl;

import cn.hutool.core.util.StrUtil;
import com.backstage.common.core.page.TableDataInfo;
import com.backstage.common.exception.ServiceException;
import com.backstage.system.config.properties.SearchEsProperties;
import com.backstage.system.domain.assistant.AssistantFeedback;
import com.backstage.system.domain.assistant.AssistantFeedbackCategory;
import com.backstage.system.domain.assistant.AssistantFeedbackProcessRecord;
import com.backstage.system.domain.assistant.AssistantTicketStatus;
import com.backstage.system.domain.assistant.dto.AssistantFeedbackCreateDTO;
import com.backstage.system.domain.assistant.dto.AssistantFeedbackPageDTO;
import com.backstage.system.domain.assistant.dto.AssistantTicketStatusUpdateDTO;
import com.backstage.system.domain.assistant.vo.AssistantFeedbackDetailVO;
import com.backstage.system.domain.assistant.vo.AssistantFeedbackProcessRecordVO;
import com.backstage.system.domain.assistant.vo.AssistantFeedbackTagVO;
import com.backstage.system.domain.assistant.vo.AssistantFeedbackVO;
import com.backstage.system.domain.user.OshUser;
import com.backstage.system.domain.websocket.WsNotifyMessage;
import com.backstage.system.mapper.assistant.AssistantFeedbackMapper;
import com.backstage.system.mapper.user.OshUserMapper;
import com.backstage.system.service.assistant.*;
import com.backstage.system.service.assistant.support.AssistantFeedbackQuerySupport;
import com.backstage.system.service.assistant.support.AssistantFeedbackViewAssembler;
import com.backstage.system.service.websocket.WebSocketNotifyService;
import com.backstage.system.util.FeedbackHotScoreCalculator;
import com.backstage.system.utils.UserContextUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AI 助手反馈服务实现
 *
 * @author backstage
 */
@Service
public class AssistantFeedbackServiceImpl extends ServiceImpl<AssistantFeedbackMapper, AssistantFeedback>
        implements IAssistantFeedbackService {

    private static final Logger log = LoggerFactory.getLogger(AssistantFeedbackServiceImpl.class);
    private static final long DEFAULT_PENDING_CONFIRM_WINDOW_DAYS = 7L;
    private static final String FEEDBACK_STATUS_CHANGED = "FEEDBACK_STATUS_CHANGED";
    private static final String FEEDBACK_PENDING_CONFIRM_REMINDER_DAY3 = "FEEDBACK_PENDING_CONFIRM_REMINDER_DAY3";
    private static final String FEEDBACK_PENDING_CONFIRM_REMINDER_DAY6 = "FEEDBACK_PENDING_CONFIRM_REMINDER_DAY6";
    private static final String FEEDBACK_AUTO_CONFIRMED = "FEEDBACK_AUTO_CONFIRMED";

    private final IAssistantFeedbackCategoryService categoryService;
    private final IAssistantFeedbackLikeService likeService;
    private final IAssistantFeedbackFavoriteService favoriteService;
    private final IAssistantFeedbackProcessRecordService processRecordService;
    private final IAssistantFeedbackTagService feedbackTagService;
    private final IAssistantFeedbackEsService feedbackEsService;
    private final OshUserMapper oshUserMapper;
    private final AssistantFeedbackQuerySupport feedbackQuerySupport;
    private final AssistantFeedbackViewAssembler feedbackViewAssembler;
    private final WebSocketNotifyService webSocketNotifyService;
    private final SearchEsProperties searchEsProperties;

    public AssistantFeedbackServiceImpl(IAssistantFeedbackCategoryService categoryService,
                                        IAssistantFeedbackLikeService likeService,
                                        IAssistantFeedbackFavoriteService favoriteService,
                                        IAssistantFeedbackProcessRecordService processRecordService,
                                        IAssistantFeedbackTagService feedbackTagService,
                                        IAssistantFeedbackEsService feedbackEsService,
                                        OshUserMapper oshUserMapper,
                                        AssistantFeedbackQuerySupport feedbackQuerySupport,
                                        AssistantFeedbackViewAssembler feedbackViewAssembler,
                                        WebSocketNotifyService webSocketNotifyService,
                                        SearchEsProperties searchEsProperties) {
        this.categoryService = categoryService;
        this.likeService = likeService;
        this.favoriteService = favoriteService;
        this.processRecordService = processRecordService;
        this.feedbackTagService = feedbackTagService;
        this.feedbackEsService = feedbackEsService;
        this.oshUserMapper = oshUserMapper;
        this.feedbackQuerySupport = feedbackQuerySupport;
        this.feedbackViewAssembler = feedbackViewAssembler;
        this.webSocketNotifyService = webSocketNotifyService;
        this.searchEsProperties = searchEsProperties;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AssistantFeedbackVO createFeedback(Long userId, AssistantFeedbackCreateDTO dto) {
        return createFeedback(userId, dto, false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AssistantFeedbackVO createFeedback(Long userId, AssistantFeedbackCreateDTO dto, boolean allowAdminOnly) {
        // 验证分类是否存在且可用
        AssistantFeedbackCategory category = categoryService.getById(dto.getCategoryId());
        if (category == null || category.getIsEnabled() == 0) {
            throw new ServiceException("分类不存在或已禁用");
        }

        // 检查是否为管理员专用分类
        if (!allowAdminOnly && category.getIsAdminOnly() == 1) {
            throw new ServiceException("该分类仅管理员可用");
        }

        AssistantFeedback feedback = new AssistantFeedback();
        feedback.setUserId(userId);
        feedback.setCategoryId(dto.getCategoryId());
        feedback.setTicketNo(generateTicketNo());
        feedback.setTitle(dto.getTitle().trim());
        feedback.setContent(dto.getContent().trim());
        feedback.setPagePath(StrUtil.isNotBlank(dto.getPagePath()) ? dto.getPagePath().trim() : null);
        feedback.setStatus(AssistantTicketStatus.PENDING.getCode());
        feedback.setResult("");
        feedback.setHandlerId(null);
        feedback.setHandlerName(null);
        feedback.setHandledTime(null);
        feedback.setCloseReason(null);
        feedback.setIsPinned(0);
        feedback.setPinOrder(0);
        feedback.setCommentCount(0);
        feedback.setViewCount(0);
        feedback.setLikeCount(0);
        feedback.setFavoriteCount(0);
        feedback.setHotScore(0);
        feedback.setDeleted(false);
        feedback.setCreateBy(userId);
        feedback.setUpdateBy(userId);

        save(feedback);
        feedbackTagService.bindFeedbackTags(feedback.getId(), dto.getTagIds(), userId);
        safeCreateProcessRecord(feedback.getId(), null, AssistantTicketStatus.PENDING.getCode(),
                userId, getUserDisplayName(getUserById(userId), "匿名用户"), "用户提交反馈");
        syncFeedbackToEs(feedback.getId());
        return feedbackViewAssembler.toFeedbackVO(feedback);
    }

    @Override
    public TableDataInfo getMyFeedback(Long userId) {
        Page<AssistantFeedback> page = lambdaQuery()
                .eq(AssistantFeedback::getUserId, userId)
                .eq(AssistantFeedback::getDeleteFlag, (byte) 0)
                .orderByDesc(AssistantFeedback::getCreateTime)
                .page(new Page<>(1, 1000));

        List<AssistantFeedbackVO> rows = page.getRecords().stream()
                .map(feedbackViewAssembler::toFeedbackVO)
                .collect(Collectors.toList());
        return new TableDataInfo(rows, page.getTotal());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AssistantFeedbackVO updateTicketStatus(Long ticketId, Long handlerId, AssistantTicketStatusUpdateDTO dto) {
        String targetStatus = AssistantTicketStatus.normalize(dto.getToStatus());
        if (!AssistantTicketStatus.isValid(targetStatus)) {
            throw new ServiceException("不支持的工单状态");
        }

        AssistantFeedback feedback = getActiveFeedback(ticketId);
        if (feedback == null) {
            throw new ServiceException("工单不存在");
        }

        String currentStatus = AssistantTicketStatus.normalize(feedback.getStatus());
        if (!AssistantTicketStatus.canTransfer(currentStatus, targetStatus)) {
            throw new ServiceException("当前状态不允许流转到目标状态");
        }

        OshUser handler = getUserById(handlerId);
        String handlerName = getUserDisplayName(handler, "管理员");
        String remark = StrUtil.trim(dto.getRemark());
        LocalDateTime handledTime = LocalDateTime.now();

        feedback.setStatus(targetStatus);
        feedback.setResult(StrUtil.blankToDefault(remark, ""));
        feedback.setHandlerId(handlerId);
        feedback.setHandlerName(handlerName);
        feedback.setUpdateBy(handlerId);
        this.updateById(feedback);
        safeCreateProcessRecord(ticketId, currentStatus, targetStatus, handlerId, handlerName, remark);
        feedback.setHandledTime(handledTime);
        feedback.setCloseReason(AssistantTicketStatus.CLOSED.getCode().equals(targetStatus) ? StrUtil.blankToDefault(remark, "") : null);
        notifySubmitterStatusChanged(feedback, currentStatus, targetStatus, remark);
        syncFeedbackToEs(ticketId);
        return feedbackViewAssembler.toFeedbackVO(feedback);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AssistantFeedbackVO confirmTicketStatus(Long feedbackId, Long userId, AssistantTicketStatusUpdateDTO dto) {
        String targetStatus = AssistantTicketStatus.normalize(dto.getToStatus());
        if (!AssistantTicketStatus.RESOLVED.getCode().equals(targetStatus)
                && !AssistantTicketStatus.REOPENED.getCode().equals(targetStatus)) {
            throw new ServiceException("仅支持确认已解决或反馈问题仍在");
        }

        AssistantFeedback feedback = getActiveFeedback(feedbackId);
        if (feedback == null) {
            throw new ServiceException("工单不存在");
        }
        if (!userId.equals(feedback.getUserId())) {
            throw new ServiceException("只有工单提交人可以确认处理结果");
        }

        String currentStatus = AssistantTicketStatus.normalize(feedback.getStatus());
        if (!AssistantTicketStatus.PENDING_CONFIRM.getCode().equals(currentStatus)) {
            throw new ServiceException("当前工单不处于待确认状态");
        }
        if (!AssistantTicketStatus.canTransfer(currentStatus, targetStatus)) {
            throw new ServiceException("当前状态不允许执行该确认操作");
        }

        OshUser currentUser = getUserById(userId);
        String operatorName = getUserDisplayName(currentUser, "提交人");
        String defaultRemark = AssistantTicketStatus.RESOLVED.getCode().equals(targetStatus) ? "提交人确认已解决" : "提交人反馈问题仍在";
        String remark = StrUtil.blankToDefault(StrUtil.trim(dto.getRemark()), defaultRemark);

        feedback.setStatus(targetStatus);
        feedback.setResult(remark);
        feedback.setUpdateBy(userId);
        updateById(feedback);
        safeCreateProcessRecord(feedbackId, currentStatus, targetStatus, userId, operatorName, remark);
        notifySubmitterStatusChanged(feedback, currentStatus, targetStatus, remark);
        syncFeedbackToEs(feedbackId);
        return feedbackViewAssembler.toFeedbackVO(feedback);
    }

    @Override
    public long countPendingConfirmTickets(Long userId) {
        if (userId == null) {
            return 0L;
        }
        return lambdaQuery()
                .eq(AssistantFeedback::getUserId, userId)
                .eq(AssistantFeedback::getDeleteFlag, (byte) 0)
                .eq(AssistantFeedback::getStatus, AssistantTicketStatus.PENDING_CONFIRM.getCode())
                .count();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void processPendingConfirmTickets() {
        List<AssistantFeedback> pendingConfirmFeedbackList = lambdaQuery()
                .eq(AssistantFeedback::getDeleteFlag, (byte) 0)
                .eq(AssistantFeedback::getStatus, AssistantTicketStatus.PENDING_CONFIRM.getCode())
                .list();
        pendingConfirmFeedbackList.forEach(this::handlePendingConfirmFeedback);
    }

    /**
     * 生成工单编号
     * 格式：TK + 日期(yyyyMMdd) + 时间戳后6位
     */
    private String generateTicketNo() {
        String date = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String timestamp = String.valueOf(System.currentTimeMillis());
        String suffix = timestamp.substring(Math.max(0, timestamp.length() - 6));
        return "TK" + date + suffix;
    }

    @Override
    public TableDataInfo pageFeedback(AssistantFeedbackPageDTO dto) {
        Page<AssistantFeedback> page = feedbackQuerySupport.pageFeedback(dto);
        Map<Long, List<AssistantFeedbackTagVO>> feedbackTagMap =
                feedbackViewAssembler.buildFeedbackTagMap(page.getRecords());

        List<AssistantFeedbackVO> rows = page.getRecords().stream()
                .map(feedback -> feedbackViewAssembler.toFeedbackVO(feedback, feedbackTagMap))
                .collect(Collectors.toList());

        return new TableDataInfo(rows, page.getTotal());
    }

    @Override
    public TableDataInfo pageFeedbackList(AssistantFeedbackPageDTO dto) {
        if (searchEsProperties.isEnabled()) {
            try {
                return buildFeedbackListTableDataByEs(dto);
            } catch (Exception exception) {
                log.warn("feedback page fallback to mysql after es failure, dto={}", dto, exception);
            }
        }
        return buildFeedbackListTableDataByDb(dto);
    }

    /**
     * 基于 Elasticsearch 构建反馈列表分页数据
     * <p>
     * 该方法通过 ES 搜索引擎进行反馈数据的分页查询，相比数据库查询具有以下优势：
     * 1. 支持全文检索、分词匹配，搜索性能更高
     * 2. 支持复杂的聚合分析场景
     * 3. 减轻数据库压力，提升系统整体吞吐量
     * </p>
     *
     * @param dto 反馈分页查询参数，包含关键词、状态、分类等筛选条件
     * @return 分页结果，包含反馈列表视图对象及总记录数
     */
    private TableDataInfo buildFeedbackListTableDataByEs(AssistantFeedbackPageDTO dto) {
        com.backstage.common.response.PageResponse<AssistantFeedback> pageResponse = feedbackEsService.searchFeedbacks(dto);
        Map<Long, OshUser> userMap = feedbackViewAssembler.buildUserMap(pageResponse.getRows());
        Map<Long, AssistantFeedbackCategory> categoryMap = feedbackViewAssembler.buildCategoryMap(pageResponse.getRows());
        Map<Long, List<AssistantFeedbackTagVO>> feedbackTagMap = feedbackViewAssembler.buildFeedbackTagMap(pageResponse.getRows());

        List<?> rows = pageResponse.getRows().stream()
                .map(feedback -> feedbackViewAssembler.toFeedbackListVO(feedback, userMap, categoryMap, feedbackTagMap))
                .collect(Collectors.toList());

        return new TableDataInfo(rows, pageResponse.getTotal());
    }

    private TableDataInfo buildFeedbackListTableDataByDb(AssistantFeedbackPageDTO dto) {
        Page<AssistantFeedback> page = feedbackQuerySupport.pageFeedback(dto);
        Map<Long, OshUser> userMap = feedbackViewAssembler.buildUserMap(page.getRecords());
        Map<Long, AssistantFeedbackCategory> categoryMap = feedbackViewAssembler.buildCategoryMap(page.getRecords());
        Map<Long, List<AssistantFeedbackTagVO>> feedbackTagMap =
                feedbackViewAssembler.buildFeedbackTagMap(page.getRecords());

        List<?> rows = page.getRecords().stream()
                .map(feedback -> feedbackViewAssembler.toFeedbackListVO(feedback, userMap, categoryMap, feedbackTagMap))
                .collect(Collectors.toList());

        return new TableDataInfo(rows, page.getTotal());
    }

    @Override
    public AssistantFeedbackDetailVO getFeedbackDetail(Long feedbackId) {
        return buildFeedbackDetail(feedbackId, true);
    }

    @Override
    public AssistantFeedbackDetailVO getFeedbackStatusSummary(Long feedbackId) {
        return buildFeedbackDetail(feedbackId, false);
    }

    @Override
    public List<AssistantFeedbackProcessRecordVO> listFeedbackProcessRecords(Long feedbackId) {
        AssistantFeedback feedback = getActiveFeedback(feedbackId);
        if (feedback == null) {
            throw new ServiceException("反馈不存在");
        }
        return feedbackViewAssembler.listProcessRecordsSafely(feedbackId);
    }

    private OshUser getUserById(Long userId) {
        return userId == null ? null : oshUserMapper.selectById(userId);
    }

    private AssistantFeedbackDetailVO buildFeedbackDetail(Long feedbackId, boolean incrementView) {
        AssistantFeedback feedback = getActiveFeedback(feedbackId);
        if (feedback == null) {
            throw new ServiceException("反馈不存在");
        }

        if (incrementView) {
            incrementViewCount(feedbackId);
        }

        int currentViewCount = feedback.getViewCount() == null ? 0 : feedback.getViewCount();
        List<AssistantFeedbackProcessRecordVO> processRecords = feedbackViewAssembler.listProcessRecordsSafely(feedbackId);
        AssistantFeedbackDetailVO detailVO = feedbackViewAssembler.toFeedbackDetailVO(
                feedback,
                processRecords,
                incrementView ? currentViewCount + 1 : currentViewCount
        );

        try {
            Long userId = UserContextUtil.getCurrentUserId();
            if (userId == null) {
                detailVO.setIsLiked(false);
                detailVO.setIsFavorited(false);
                return detailVO;
            }
            boolean isLiked = likeService.isLiked(feedbackId, userId);
            boolean isFavorited = favoriteService.isFavorited(feedbackId, userId);
            detailVO.setIsLiked(isLiked);
            detailVO.setIsFavorited(isFavorited);
        } catch (Exception exception) {
            log.warn("[buildFeedbackDetail] 获取用户互动状态失败: {}", exception.getMessage());
            detailVO.setIsLiked(false);
            detailVO.setIsFavorited(false);
        }

        return detailVO;
    }

    private AssistantFeedback getActiveFeedback(Long feedbackId) {
        return lambdaQuery()
                .eq(AssistantFeedback::getId, feedbackId)
                .eq(AssistantFeedback::getDeleteFlag, (byte) 0)
                .one();
    }

    private String getUserDisplayName(OshUser user, String fallbackName) {
        if (user == null) {
            return fallbackName;
        }
        return StrUtil.isNotBlank(user.getUsername()) ? user.getUsername() : "匿名用户";
    }

    private void handlePendingConfirmFeedback(AssistantFeedback feedback) {
        AssistantFeedbackProcessRecord pendingConfirmRecord = resolveLatestRecordByToStatus(feedback.getId(), AssistantTicketStatus.PENDING_CONFIRM.getCode());
        if (pendingConfirmRecord == null || pendingConfirmRecord.getCreateTime() == null) {
            return;
        }

        long pendingDays = ChronoUnit.DAYS.between(pendingConfirmRecord.getCreateTime(), LocalDateTime.now());
        if (pendingDays >= DEFAULT_PENDING_CONFIRM_WINDOW_DAYS) {
            autoConfirmFeedback(feedback);
            return;
        }
        if (pendingDays >= 6) {
            sendPendingConfirmReminderIfNeeded(feedback, FEEDBACK_PENDING_CONFIRM_REMINDER_DAY6, "请尽快确认工单处理结果，系统将在 1 天后自动确认已解决");
            return;
        }
        if (pendingDays >= 3) {
            sendPendingConfirmReminderIfNeeded(feedback, FEEDBACK_PENDING_CONFIRM_REMINDER_DAY3, "该工单已等待你确认 3 天，请尽快处理，避免进入系统自动确认");
        }
    }

    private void autoConfirmFeedback(AssistantFeedback feedback) {
        String currentStatus = AssistantTicketStatus.normalize(feedback.getStatus());
        if (!AssistantTicketStatus.PENDING_CONFIRM.getCode().equals(currentStatus)) {
            return;
        }
        String remark = "系统自动确认已解决";
        feedback.setStatus(AssistantTicketStatus.RESOLVED.getCode());
        feedback.setResult(remark);
        feedback.setUpdateBy(0L);
        updateById(feedback);
        safeCreateProcessRecord(feedback.getId(), currentStatus, AssistantTicketStatus.RESOLVED.getCode(), 0L, "系统", remark);
        notifySubmitterStatusChanged(feedback, currentStatus, AssistantTicketStatus.RESOLVED.getCode(), remark);
        sendAutoConfirmedNotification(feedback);
        syncFeedbackToEs(feedback.getId());
    }

    private AssistantFeedbackProcessRecord resolveLatestRecordByToStatus(Long feedbackId, String toStatus) {
        return Db.lambdaQuery(AssistantFeedbackProcessRecord.class)
                .eq(AssistantFeedbackProcessRecord::getFeedbackId, feedbackId)
                .eq(AssistantFeedbackProcessRecord::getToStatus, toStatus)
                .orderByDesc(AssistantFeedbackProcessRecord::getCreateTime)
                .last("LIMIT 1")
                .one();
    }

    private void sendPendingConfirmReminderIfNeeded(AssistantFeedback feedback, String notificationType, String content) {
        String bizId = String.valueOf(feedback.getId());
        boolean alreadySent = Db.lambdaQuery(WsNotifyMessage.class)
                .eq(WsNotifyMessage::getTargetUserId, feedback.getUserId())
                .eq(WsNotifyMessage::getType, notificationType)
                .eq(WsNotifyMessage::getBizId, bizId)
                .count() > 0;
        if (alreadySent) {
            return;
        }
        WsNotifyMessage message = buildNotification(notificationType, "你的反馈等待确认", content, feedback);
        webSocketNotifyService.send(feedback.getUserId(), message);
    }

    private void notifySubmitterStatusChanged(AssistantFeedback feedback, String fromStatus, String toStatus, String remark) {
        if (feedback.getUserId() == null) {
            return;
        }
        String fromStatusText = AssistantTicketStatus.getDescriptionByCode(fromStatus);
        String toStatusText = AssistantTicketStatus.getDescriptionByCode(toStatus);
        String title = "你的反馈有新进展";
        String content = "《" + feedback.getTitle() + "》" + fromStatusText + " → " + toStatusText;
        if (StrUtil.isNotBlank(remark)) {
            content = content + "，" + webSocketNotifyService.truncate(remark);
        }
        WsNotifyMessage message = buildNotification(FEEDBACK_STATUS_CHANGED, title, content, feedback);
        webSocketNotifyService.send(feedback.getUserId(), message);
    }

    private void sendAutoConfirmedNotification(AssistantFeedback feedback) {
        WsNotifyMessage message = buildNotification(
                FEEDBACK_AUTO_CONFIRMED,
                "工单已自动确认",
                "《" + feedback.getTitle() + "》超过确认时限，系统已自动确认为已解决",
                feedback
        );
        webSocketNotifyService.send(feedback.getUserId(), message);
    }

    private WsNotifyMessage buildNotification(String type, String title, String content, AssistantFeedback feedback) {
        WsNotifyMessage message = new WsNotifyMessage();
        message.setType(type);
        message.setTitle(title);
        message.setContent(content);
        message.setJumpUrl("/feedback/detail/" + feedback.getId());
        message.setBizId(String.valueOf(feedback.getId()));
        return message;
    }

    private void safeCreateProcessRecord(Long feedbackId, String fromStatus, String toStatus,
                                         Long operatorId, String operatorName, String remark) {
        try {
            processRecordService.createRecord(feedbackId, fromStatus, toStatus, operatorId, operatorName, remark);
        } catch (Exception exception) {
            log.warn("写入反馈处理记录失败，feedbackId={}, message={}", feedbackId, exception.getMessage());
        }
    }

    private void incrementViewCount(Long feedbackId) {
        AssistantFeedback feedback = getActiveFeedback(feedbackId);
        if (feedback == null) {
            return;
        }
        int newViewCount = (feedback.getViewCount() == null ? 0 : feedback.getViewCount()) + 1;
        int newHotScore = calculateHotScore(feedback, null, null, null, newViewCount);
        AssistantFeedback updateEntity = new AssistantFeedback();
        updateEntity.setId(feedbackId);
        updateEntity.setViewCount(newViewCount);
        updateEntity.setHotScore(newHotScore);
        updateById(updateEntity);
        syncFeedbackToEs(feedbackId);
    }

    /**
     * 计算热度分（支持部分字段更新）
     *
     * @param feedback         原反馈数据
     * @param newLikeCount     新点赞数（null表示不变）
     * @param newFavoriteCount 新收藏数（null表示不变）
     * @param newCommentCount  新评论数（null表示不变）
     * @param newViewCount     新浏览数（null表示不变）
     * @return 新的热度分
     */
    private int calculateHotScore(AssistantFeedback feedback, Integer newLikeCount, Integer newFavoriteCount,
                                  Integer newCommentCount, Integer newViewCount) {
        int likeCount = newLikeCount != null ? newLikeCount : (feedback.getLikeCount() == null ? 0 : feedback.getLikeCount());
        int favoriteCount = newFavoriteCount != null ? newFavoriteCount : (feedback.getFavoriteCount() == null ? 0 : feedback.getFavoriteCount());
        int commentCount = newCommentCount != null ? newCommentCount : (feedback.getCommentCount() == null ? 0 : feedback.getCommentCount());
        int viewCount = newViewCount != null ? newViewCount : (feedback.getViewCount() == null ? 0 : feedback.getViewCount());

        return FeedbackHotScoreCalculator.calculate(likeCount, favoriteCount, commentCount, viewCount);
    }

    private void syncFeedbackToEs(Long feedbackId) {
        if (!searchEsProperties.isEnabled() || feedbackId == null) {
            return;
        }
        try {
            feedbackEsService.upsertFeedbackById(feedbackId);
        } catch (Exception exception) {
            log.warn("sync feedback to es failed, feedbackId={}", feedbackId, exception);
        }
    }
}
