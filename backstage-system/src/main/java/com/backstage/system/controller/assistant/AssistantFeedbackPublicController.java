package com.backstage.system.controller.assistant;

import com.backstage.common.annotation.Anonymous;
import com.backstage.common.core.controller.BaseController;
import com.backstage.common.core.domain.R;
import com.backstage.common.core.page.TableDataInfo;
import com.backstage.common.enums.AnnouncementChannelEnum;
import com.backstage.common.enums.AnnouncementModuleEnum;
import com.backstage.system.domain.announcement.vo.AnnouncementMarqueeVO;
import com.backstage.system.domain.assistant.dto.AssistantFeedbackPageDTO;
import com.backstage.system.domain.assistant.vo.*;
import com.backstage.system.service.announcement.IAnnouncementMarqueeQueryService;
import com.backstage.system.service.assistant.*;
import com.backstage.system.utils.UserContextUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * AI 助手反馈公开接口 Controller（公开接口所以家@Anonymous）
 *
 * @author backstage
 */
@Anonymous
@Api(tags = "AI助手反馈-公开接口")
@RestController
@RequestMapping("/pc/feedback")
public class AssistantFeedbackPublicController extends BaseController {

    public AssistantFeedbackPublicController(IAssistantFeedbackCategoryService categoryService,
                                             IAssistantFeedbackService feedbackService,
                                             IAssistantFeedbackCommentService commentService,
                                             IAssistantFeedbackTagService feedbackTagService,
                                             IAnnouncementMarqueeQueryService announcementMarqueeQueryService,
                                             IAssistantFeedbackDynamicService feedbackDynamicService) {
        this.categoryService = categoryService;
        this.feedbackService = feedbackService;
        this.commentService = commentService;
        this.feedbackTagService = feedbackTagService;
        this.announcementMarqueeQueryService = announcementMarqueeQueryService;
        this.feedbackDynamicService = feedbackDynamicService;
    }

    private final IAssistantFeedbackCategoryService categoryService;
    private final IAssistantFeedbackService feedbackService;
    private final IAssistantFeedbackCommentService commentService;
    private final IAssistantFeedbackTagService feedbackTagService;
    private final IAnnouncementMarqueeQueryService announcementMarqueeQueryService;
    private final IAssistantFeedbackDynamicService feedbackDynamicService;

    /**
     * 反馈公告列表。
     * <p>前端固定渲染为不可点击的纯文本跑马灯，支持两列布局（系统通知 + 业务公告）。</p>
     *
     * @param limit   返回条数
     * @param channel 公告频道（1=系统通知, 2=业务公告），不传默认 1
     */
    @ApiOperation("查询反馈公告")
    @GetMapping("/announcement/list")
    public R<List<AnnouncementMarqueeVO>> listAnnouncements(
            @RequestParam(value = "limit", defaultValue = "10") Integer limit,
            @RequestParam(value = "channel", required = false) Integer channel) {
        return R.ok(announcementMarqueeQueryService.list(
                AnnouncementModuleEnum.FEEDBACK, AnnouncementChannelEnum.fromCode(channel), limit));
    }

    /**
     * 查询反馈互动动态列表。
     * <p>返回最近的点赞、收藏等互动事件，用于跑马灯展示。</p>
     *
     * @param limit 返回条数，默认 10
     * @return 互动动态列表
     */
    @ApiOperation("查询反馈互动动态")
    @GetMapping("/dynamics/list")
    public R<List<FeedbackDynamicVO>> listDynamics(
            @RequestParam(value = "limit", defaultValue = "10") Integer limit) {
        return R.ok(feedbackDynamicService.listRecentDynamics(limit));
    }

    /**
     * 获取反馈分类列表（用户可见）
     */
    @ApiOperation("获取反馈分类列表")
    @GetMapping("/category/list")
    public R<List<AssistantFeedbackCategoryVO>> listCategories() {
        List<AssistantFeedbackCategoryVO> categories = categoryService.listUserCategories();
        return R.ok(categories);
    }

    /**
     * 获取反馈标签列表
     */
    @ApiOperation("获取反馈标签列表")
    @GetMapping("/tag/list")
    public R<List<AssistantFeedbackTagVO>> listTags(@RequestParam(required = false) String keyword) {
        return R.ok(feedbackTagService.listEnabledTags(keyword));
    }

    /**
     * 分页查询反馈列表。
     * <p>
     * 本接口标注 {@link Anonymous}，认证过滤器会直接放行而不写入 ThreadLocal。
     * 这里通过 {@link UserContextUtil#getCurrentUserIdSafely()} 做 Token 软认证回退识别，
     * 以支撑"我的反馈 / 我的收藏"两个 tab 在同一个匿名接口下识别登录用户。
     * </p>
     */
    @ApiOperation("分页查询反馈列表")
    @PostMapping("/page")
    public TableDataInfo pageFeedback(@RequestBody AssistantFeedbackPageDTO dto) {
        dto.setUserId(UserContextUtil.getCurrentUserIdSafely());
        return feedbackService.pageFeedbackList(dto);
    }

    /**
     * 获取反馈详情
     */
    @ApiOperation("获取反馈详情")
    @GetMapping("/detail/{id}")
    public R<AssistantFeedbackDetailVO> getFeedbackDetail(@PathVariable("id") Long id) {
        AssistantFeedbackDetailVO detail = feedbackService.getFeedbackDetail(id);
        return R.ok(detail);
    }

    @ApiOperation("获取工单状态摘要")
    @GetMapping("/status-summary/{id}")
    public R<AssistantFeedbackDetailVO> getFeedbackStatusSummary(@PathVariable("id") Long id) {
        return R.ok(feedbackService.getFeedbackStatusSummary(id));
    }

    /**
     * 获取反馈处理记录时间线
     */
    @ApiOperation("获取反馈处理记录")
    @GetMapping("/{id}/process-record/list")
    public R<List<AssistantFeedbackProcessRecordVO>> listProcessRecords(@PathVariable("id") Long feedbackId) {
        return R.ok(feedbackService.listFeedbackProcessRecords(feedbackId));
    }

    /**
     * 分页查询反馈的评论列表
     */
    @ApiOperation("分页查询反馈的评论列表")
    @GetMapping("/{id}/comment/list")
    public R<List<AssistantFeedbackCommentVO>> listComments(
            @PathVariable("id") Long feedbackId,
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {
        List<AssistantFeedbackCommentVO> comments = commentService.listCommentsByFeedbackId(feedbackId, pageNum, pageSize);
        return R.ok(comments);
    }
}
