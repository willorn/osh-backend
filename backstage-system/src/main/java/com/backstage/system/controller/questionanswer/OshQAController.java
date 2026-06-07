package com.backstage.system.controller.questionanswer;

import com.backstage.common.annotation.Anonymous;
import com.backstage.common.annotation.OshUserEvent;
import com.backstage.common.annotation.OshUserLevel;
import com.backstage.common.constant.ResourceType;
import com.backstage.common.core.domain.R;
import com.backstage.common.core.page.TableDataInfo;
import com.backstage.system.domain.questionanswer.dto.*;
import com.backstage.system.domain.questionanswer.vo.QATagVO;
import com.backstage.system.domain.questionanswer.vo.QueryQuestionDetailVO;
import com.backstage.system.domain.questionanswer.vo.QueryQuestionListVO;
import com.backstage.system.mapper.course.OshCourseSectionMapper;
import com.backstage.system.service.IOshCourseService;
import com.backstage.system.service.questionanswer.IOshQAAnswerService;
import com.backstage.system.service.questionanswer.IOshQAQuestionService;
import com.backstage.system.service.questionanswer.IOshQATagService;
import com.backstage.system.utils.UserContextUtil;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * Description:
 * OshUser: 九转苍翎
 * Date: 2026/3/24
 * Time: 21:08
 */
@RestController
@RequestMapping("/pc/qna")
public class OshQAController {

    @Autowired
    private IOshQATagService IOshQATagService;
    @Autowired
    private IOshQAQuestionService IOshQAQuestionService;
    @Autowired
    private IOshQAAnswerService iOshQAAnswerService;
    @Autowired
    private IOshCourseService oshCourseService;
    @Autowired
    private OshCourseSectionMapper oshCourseSectionMapper;

    @ApiOperation("标签录入")
    @GetMapping("/tag/search")
    @OshUserEvent(module = "答疑模块", actionType = "查询", description = "标签录入")
    @PreAuthorize("hasAuthority('qna:tag:search')")
    public R<List<QATagVO>> searchTags(@RequestParam(value = "type",required = false) String type) {
        return IOshQATagService.searchTags(type);
    }

    @ApiOperation("新增问题")
    @PostMapping("/question/create")
    @OshUserEvent(module = "答疑模块", actionType = "新增", description = "新增问题", resourceType = ResourceType.QA_TAG_TYPE)
    @PreAuthorize("hasAuthority('qna:question:create')")
    public R<String> addQuestion(
            @RequestBody AddQuestionDTO addQuestionDTO) {
        Long userId = UserContextUtil.getCurrentUserId();
        if (userId == null) {
            return R.fail("请先登录");
        }
        // 权限判断：level >= 2 OR 已付费该课程（通过 resourceNo 即 sectionId 查 courseId）
        boolean hasPermission = false;
        try {
            int userLevel = UserContextUtil.getCurrentLevel();
            hasPermission = userLevel >= 2;
        } catch (Exception ignored) {}

        Long sectionId = addQuestionDTO == null ? null : addQuestionDTO.getResourceNo();
        if (!hasPermission) {
            // 尝试通过 sectionId 查 courseId，判断是否已付费
            if (sectionId != null) {
                try {
                    java.util.Map<String, Object> section = oshCourseSectionMapper.selectSectionById(sectionId);
                    if (section != null) {
                        Object courseIdObj = section.get("course_id");
                        if (courseIdObj != null) {
                            Long courseId = Long.parseLong(courseIdObj.toString());
                            hasPermission = oshCourseService.hasUserBoughtCourse(courseId, userId);
                        }
                    }
                } catch (Exception ignored) {}
            }
        }

        if (sectionId != null && !hasPermission) {
            return R.fail("需要 VIP 及以上等级，或已购买该课程，才能提问");
        }

        return IOshQAQuestionService.addQuestion(userId,
                addQuestionDTO == null ? null : addQuestionDTO.getResourceNo(),
                addQuestionDTO == null ? null : addQuestionDTO.getResourceType(),
                addQuestionDTO == null ? null : addQuestionDTO.getContent(),
                addQuestionDTO == null ? null : addQuestionDTO.getIsPaidOnly(),
                addQuestionDTO == null ? null : addQuestionDTO.getTags());
    }

    @ApiOperation("发布问题")
    @PostMapping("/question/publish")
    @OshUserEvent(module = "答疑模块", actionType = "新增", description = "发布问题", resourceType = ResourceType.QA_QUESTION_TYPE)
    @PreAuthorize("hasAuthority('qna:question:publish')")
    public R<String> publishQuestion(
            @RequestBody PublishQuestionDTO publishQuestionDTO) {
        return IOshQAQuestionService.publishQuestion(UserContextUtil.getCurrentUserId(), publishQuestionDTO == null ? null : publishQuestionDTO.getQuestionId());
    }

    @ApiOperation("我的草稿")
    @GetMapping("/question/my/draft")
    @OshUserEvent(module = "答疑模块", actionType = "查询", description = "我的草稿", resourceType = ResourceType.QA_QUESTION_TYPE)
    @PreAuthorize("hasAuthority('qna:question:myDraft')")
    public R<List<QueryQuestionListVO>> myDraft() {
        return IOshQAQuestionService.myDraft(UserContextUtil.getCurrentUserId());
    }

    @ApiOperation("编辑问题")
    @PostMapping("/question/my/draft/edit")
    @OshUserEvent(module = "答疑模块", actionType = "编辑", description = "编辑问题", resourceType = ResourceType.QA_QUESTION_TYPE)
    @PreAuthorize("hasAuthority('qna:question:myDraftEdit')")
    public R<String> editQuestion(
            @RequestBody EditQuestionDTO editQuestionDTO) {
        return IOshQAQuestionService.editQuestion(UserContextUtil.getCurrentUserId(),
                editQuestionDTO == null ? null : editQuestionDTO.getQuestionId(),
                editQuestionDTO == null ? null : editQuestionDTO.getResourceNo(),
                editQuestionDTO == null ? null : editQuestionDTO.getResourceType(),
                editQuestionDTO == null ? null : editQuestionDTO.getContent(),
                editQuestionDTO == null ? null : editQuestionDTO.getIsPaidOnly(),
                editQuestionDTO == null ? null : editQuestionDTO.getTags());
    }

    @ApiOperation("删除问题")
    @PostMapping("/question/delete")
    @OshUserEvent(module = "答疑模块", actionType = "删除", description = "删除问题", resourceType = ResourceType.QA_QUESTION_TYPE)
    @PreAuthorize("hasAuthority('qna:question:delete')")
    public R<String> deleteQuestion(
            @RequestBody DeleteQuestionDTO deleteQuestionDTO) {
        return IOshQAQuestionService.deleteQuestion(UserContextUtil.getCurrentUserId(), deleteQuestionDTO == null ? null : deleteQuestionDTO.getQuestionId());
    }

    @ApiOperation("关注问题")
    @PostMapping("/question/follow")
    @OshUserEvent(module = "答疑模块", actionType = "关注", description = "关注问题", resourceType = ResourceType.QA_QUESTION_TYPE)
    @PreAuthorize("hasAuthority('qna:question:follow')")
    public R<String> followQuestion(
            @RequestBody FollowQuestionDTO followQuestionDTO) {
        return IOshQAQuestionService.followQuestion(UserContextUtil.getCurrentUserId(), followQuestionDTO == null ? null : followQuestionDTO.getQuestionId());
    }

    @ApiOperation("取消关注问题")
    @PostMapping("/question/follow/cancel")
    @OshUserEvent(module = "答疑模块", actionType = "取消关注", description = "取消关注问题", resourceType = ResourceType.QA_QUESTION_TYPE)
    @PreAuthorize("hasAuthority('qna:question:cancelFollow')")
    public R<String> cancelFollowQuestion(
            @RequestBody FollowQuestionDTO followQuestionDTO) {
        return IOshQAQuestionService.cancelFollowQuestion(UserContextUtil.getCurrentUserId(), followQuestionDTO == null ? null : followQuestionDTO.getQuestionId());
    }

    @ApiOperation("问题列表")
    @PostMapping("/question/list")
    @OshUserEvent(module = "答疑模块", actionType = "查询", description = "问题列表", resourceType = ResourceType.QA_QUESTION_TYPE)
    @Anonymous
    public TableDataInfo list(@RequestBody(required = false) QueryQuestionListDTO queryQuestionListDTO) {
        if (queryQuestionListDTO == null) {
            queryQuestionListDTO = new QueryQuestionListDTO();
        }
        return IOshQAQuestionService.list(UserContextUtil.getCurrentUserIdSafely(), queryQuestionListDTO.getResourceNo(),
                queryQuestionListDTO.getResourceType(), queryQuestionListDTO.getType(), queryQuestionListDTO.getKeyword(), queryQuestionListDTO.getPageNum(), queryQuestionListDTO.getPageSize());
    }

    @ApiOperation("回答")
    @PostMapping("/answer/createPost")
    @OshUserEvent(module = "答疑模块", actionType = "新增", description = "回答", resourceType = ResourceType.QA_ANSWER_TYPE)
    @OshUserLevel(value = 2)
    public R<String> answer(@RequestBody AnswerDTO answerDTO) {
        return iOshQAAnswerService.answer(UserContextUtil.getCurrentUserId(), answerDTO == null ? null : answerDTO.getQuestionId(), answerDTO == null ? null : answerDTO.getContent());
    }

    @ApiOperation("采纳回答")
    @PostMapping("/question/solve")
    @OshUserEvent(module = "答疑模块", actionType = "修改", description = "采纳回答", resourceType = ResourceType.QA_ANSWER_TYPE)
    @OshUserLevel(value = 2)
    public R<String> solve(@RequestBody SolveQuestionDTO solveQuestionDTO) {
        return IOshQAQuestionService.solve(UserContextUtil.getCurrentUserId(), solveQuestionDTO == null ? null : solveQuestionDTO.getQuestionId(), solveQuestionDTO == null ? null : solveQuestionDTO.getAnswerId());
    }

    @ApiOperation("取消采纳回答")
    @PostMapping("/question/cancel/solve")
    @OshUserEvent(module = "答疑模块", actionType = "修改", description = "取消采纳回答", resourceType = ResourceType.QA_ANSWER_TYPE)
    @OshUserLevel(value = 2)
    public R<String> cancelSolve(@RequestBody SolveQuestionDTO solveQuestionDTO) {
        return IOshQAQuestionService.cancelSolve(UserContextUtil.getCurrentUserId(), solveQuestionDTO == null ? null : solveQuestionDTO.getQuestionId(), solveQuestionDTO == null ? null : solveQuestionDTO.getAnswerId());
    }

    @ApiOperation("问题详情")
    @PostMapping("/question/detail")
    @OshUserEvent(module = "答疑模块", actionType = "查询", description = "查询问题详情", resourceType = ResourceType.QA_QUESTION_TYPE)
    @Anonymous
    public R<QueryQuestionDetailVO> detail(@RequestBody(required = false) QueryQuestionDetailDTO queryQuestionDetailDTO) {
        Long questionId = queryQuestionDetailDTO == null ? null : queryQuestionDetailDTO.getQuestionId();
        return IOshQAQuestionService.detail(UserContextUtil.getCurrentUserIdSafely(), questionId);
    }

    @ApiOperation("给回答点赞")
    @PostMapping("/answer/vote")
    @OshUserEvent(module = "答疑模块", actionType = "点赞", description = "给回答点赞", resourceType = ResourceType.QA_ANSWER_TYPE)
    @OshUserLevel(value = 2)
    public R<String> vote(@RequestBody AnswerVoteDTO answerVoteDTO) {
        return IOshQAQuestionService.vote(UserContextUtil.getCurrentUserId(), answerVoteDTO == null ? null : answerVoteDTO.getAnswerId());
    }

    @ApiOperation("取消给回答点赞")
    @PostMapping("/answer/vote/cancel")
    @OshUserEvent(module = "答疑模块", actionType = "取消点赞", description = "取消给回答点赞", resourceType = ResourceType.QA_ANSWER_TYPE)
    @OshUserLevel(value = 2)
    public R<String> cancelVote(@RequestBody AnswerVoteDTO answerVoteDTO) {
        return IOshQAQuestionService.cancelVote(UserContextUtil.getCurrentUserId(), answerVoteDTO == null ? null : answerVoteDTO.getAnswerId());
    }
}
