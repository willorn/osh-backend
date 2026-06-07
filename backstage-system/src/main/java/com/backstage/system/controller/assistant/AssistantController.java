package com.backstage.system.controller.assistant;

import com.backstage.common.annotation.Anonymous;
import com.backstage.common.constant.HttpStatus;
import com.backstage.common.core.controller.BaseController;
import com.backstage.common.core.domain.R;
import com.backstage.common.core.page.TableDataInfo;
import com.backstage.common.exception.ServiceException;
import com.backstage.system.domain.assistant.dto.*;
import com.backstage.system.domain.assistant.vo.AssistantAnswerVO;
import com.backstage.system.domain.assistant.vo.AssistantFeedbackTagVO;
import com.backstage.system.domain.assistant.vo.AssistantFeedbackVO;
import com.backstage.system.domain.assistant.vo.AssistantInitVO;
import com.backstage.system.service.assistant.IAssistantFeedbackService;
import com.backstage.system.service.assistant.IAssistantFeedbackTagService;
import com.backstage.system.service.assistant.IAssistantService;
import com.backstage.system.utils.UserContextUtil;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/pc/assistant")
public class AssistantController extends BaseController {

    public AssistantController(IAssistantService assistantService, IAssistantFeedbackService assistantFeedbackService, IAssistantFeedbackTagService assistantFeedbackTagService) {
        this.assistantService = assistantService;
        this.assistantFeedbackService = assistantFeedbackService;
        this.assistantFeedbackTagService = assistantFeedbackTagService;
    }

    private final IAssistantService assistantService;
    private final IAssistantFeedbackService assistantFeedbackService;
    private final IAssistantFeedbackTagService assistantFeedbackTagService;

    @Anonymous
    @GetMapping("/init")
    public R<AssistantInitVO> init(@RequestParam(value = "courseId", required = false) Long courseId) {
        return R.ok(assistantService.getInit(courseId,
                UserContextUtil.getCurrentUserIdSafely(),
                UserContextUtil.getCurrentLevelSafely()));
    }

    //    TODO 等RAG 课程好了之后替换掉这个接口
    @Anonymous
    @PostMapping("/site-qa/ask")
    public R<AssistantAnswerVO> askSiteQuestion(@Validated @RequestBody AssistantSiteQaAskDTO dto) {
        return R.ok(assistantService.answerSiteQuestion(dto.getQuestion()));
    }

    @PostMapping("/course-qa/ask")
    public R<AssistantAnswerVO> askCourseQuestion(@Validated @RequestBody AssistantCourseQaAskDTO dto) {
        Long userId = UserContextUtil.getCurrentUserIdSafely();
        Integer userLevel = UserContextUtil.getCurrentLevelSafely();
        AssistantInitVO init = assistantService.getInit(dto.getCourseId(), userId, userLevel);

        if (userId == null) {
            return R.fail(HttpStatus.UNAUTHORIZED, "请先登录后再使用课程问答");
        }
        if (!Boolean.TRUE.equals(init.getCourseQaEnabled())) {
            return R.fail(HttpStatus.FORBIDDEN, init.getCourseQaReason());
        }

        return R.ok(assistantService.answerCourseQuestion(dto.getCourseId(), dto.getQuestion()));
    }

    @PostMapping("/feedback/create")
    public R<AssistantFeedbackVO> createFeedback(@Validated @RequestBody AssistantFeedbackCreateDTO dto) {
        Long userId = UserContextUtil.getCurrentUserIdSafely();
        if (userId == null) {
            return R.fail(HttpStatus.UNAUTHORIZED, "请先登录后再提交反馈");
        }
        return R.ok(assistantFeedbackService.createFeedback(userId, dto), "提交成功");
    }

    @PostMapping("/feedback/tag/create")
    public R<AssistantFeedbackTagVO> createFeedbackTag(@Validated @RequestBody AssistantFeedbackTagCreateDTO dto) {
        Long userId = UserContextUtil.getCurrentUserIdSafely();
        if (userId == null) {
            return R.fail(HttpStatus.UNAUTHORIZED, "请先登录后再创建标签");
        }
        return R.ok(assistantFeedbackTagService.createOrGetTag(dto, userId), "标签已添加");
    }

    @PostMapping("/feedback/page")
    public TableDataInfo pageFeedback(@RequestBody AssistantFeedbackPageDTO dto) {
        Long userId = UserContextUtil.getCurrentUserIdSafely();
        if (userId == null) {
            throw new ServiceException("请先登录后查看反馈", HttpStatus.UNAUTHORIZED);
        }
        dto.setUserId(userId);
        return assistantFeedbackService.pageFeedbackList(dto);
    }

    @GetMapping("/feedback/pending-confirm/count")
    public R<Long> getPendingConfirmCount() {
        Long userId = UserContextUtil.getCurrentUserIdSafely();
        if (userId == null) {
            return R.fail(HttpStatus.UNAUTHORIZED, "请先登录后查看待确认工单");
        }
        return R.ok(assistantFeedbackService.countPendingConfirmTickets(userId));
    }

    @PostMapping("/feedback/{id}/confirm")
    public R<AssistantFeedbackVO> confirmFeedbackStatus(@PathVariable("id") Long feedbackId,
                                                        @Validated @RequestBody AssistantTicketStatusUpdateDTO dto) {
        Long userId = UserContextUtil.getCurrentUserIdSafely();
        if (userId == null) {
            return R.fail(HttpStatus.UNAUTHORIZED, "请先登录后再确认工单结果");
        }
        return R.ok(assistantFeedbackService.confirmTicketStatus(feedbackId, userId, dto), "操作成功");
    }
}
