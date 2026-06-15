package com.backstage.system.service.questionanswer.impl;

import com.backstage.common.core.domain.R;
import com.backstage.common.core.page.TableDataInfo;
import com.backstage.common.enums.QAQuestionSearchType;
import com.backstage.common.enums.ResourceTypeEnum;
import com.backstage.common.enums.ResultCode;
import com.backstage.common.utils.StringUtils;
import com.backstage.system.domain.questionanswer.Answer;
import com.backstage.system.domain.questionanswer.Question;
import com.backstage.system.domain.questionanswer.Tag;
import com.backstage.system.domain.questionanswer.vo.QueryQuestionDetailVO;
import com.backstage.system.domain.questionanswer.vo.QueryQuestionListVO;
import com.backstage.system.mapper.questionanswer.OshQAAnswerMapper;
import com.backstage.system.mapper.questionanswer.OshQAQuestionMapper;
import com.backstage.system.mapper.questionanswer.OshQATagMapper;
import com.backstage.system.service.questionanswer.IOshQAQuestionService;
import com.backstage.system.utils.ResourcePermissionUtil;
import com.backstage.system.utils.UserContextUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created with IntelliJ IDEA.
 * Description:
 * OshUser: 九转苍翎
 * Date: 2026/3/24
 * Time: 21:50
 */
@Service
public class OshQAQuestionServiceImpl implements IOshQAQuestionService {

    private static final byte QUESTION_STATUS_DRAFT = 0;
    private static final byte QUESTION_STATUS_PUBLISHED = 1;
    private static final byte QUESTION_STATUS_ANSWERED = 2;
    private static final byte ANSWER_STATUS_NORMAL = 0;
    private static final byte ANSWER_IS_NOT_SOLUTION = 0;
    private static final byte ANSWER_IS_SOLUTION = 1;
    private static final int DEFAULT_PAGE_NUM = 1;
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 50;

    @Autowired
    private OshQAQuestionMapper oshQaQuestionMapper;

    @Autowired
    private OshQAAnswerMapper oshQaAnswerMapper;

    @Autowired
    private OshQATagMapper oshQaTagMapper;




    @Override
    public R<String> addQuestion(Long userId, Long resourceNo, String resourceType, String content, Byte isPaidOnly, List<String> tags) {
        if (userId == null) return R.fail(ResultCode.FAILED_NOT_LOGIN.getMsg());
        if (!StringUtils.isNotEmpty(resourceType) || !StringUtils.isNotEmpty(content)) {
            return R.fail(ResultCode.FAILED_PARAMS_VALIDATE.getMsg());
        }
        ResourceTypeEnum resourceTypeEnum;
        try {
            resourceTypeEnum = ResourceTypeEnum.fromTypeCode(resourceType);
        } catch (IllegalArgumentException e) {
            return R.fail(ResultCode.FAILED_PARAMS_VALIDATE.getMsg());
        }
        if(resourceNo != null && !ResourcePermissionUtil.hasPermission(resourceTypeEnum,resourceNo)) {
            return R.fail(ResultCode.FAILED_USER_PERMISSION_DENIED.getMsg());
        }
        Question question = new Question();
        question.setUserId(userId);
        question.setResourceNo(resourceNo);
        question.setResourceType(resourceType);
        question.setContent(content);
        question.setIsPaidOnly(isPaidOnly);

        // --- 新增下面这两行，修复 Column 'create_by' cannot be null ---
        question.setCreateBy(userId);
        question.setUpdateBy(userId);
        // -------------------------------------------------------

        oshQaQuestionMapper.insert(question);

        if (tags != null && !tags.isEmpty()) {
            for (String tagName : tags) {
                Long tagId = resolveQATagId(tagName, userId);
                if (tagId != null) {
                    oshQaQuestionMapper.addQuestionTags(question.getId(), tagId, userId);
                }
            }
        }
        return R.ok(ResultCode.SUCCESS.getMsg());
    }

    @Override
    public R<String> addToolQuestion(Long userId, Long toolId, String content, List<String> tags) {
        if (userId == null) return R.fail(ResultCode.FAILED_NOT_LOGIN.getMsg());
        if (toolId == null || !StringUtils.isNotEmpty(content)) {
            return R.fail(ResultCode.FAILED_PARAMS_VALIDATE.getMsg());
        }
        Question question = new Question();
        question.setUserId(userId);
        question.setResourceNo(toolId);
        question.setResourceType("tool");
        question.setContent(content);
        question.setIsPaidOnly((byte) 0);
        question.setCreateBy(userId);
        question.setUpdateBy(userId);
        oshQaQuestionMapper.insert(question);
        if (tags != null && !tags.isEmpty()) {
            for (String tagName : tags) {
                Long tagId = resolveQATagId(tagName, userId);
                if (tagId != null) {
                    oshQaQuestionMapper.addQuestionTags(question.getId(), tagId, userId);
                }
            }
        }
        return R.ok(ResultCode.SUCCESS.getMsg());
    }

    @Override
    public R<String> publishQuestion(Long userId, Long questionId) {
        if (userId == null) return R.fail(ResultCode.FAILED_NOT_LOGIN.getMsg());
        if (questionId == null) return R.fail(ResultCode.FAILED_PARAMS_VALIDATE.getMsg());
        LambdaQueryWrapper<Question> wrapper = new LambdaQueryWrapper<Question>()
                .select(Question::getUserId, Question::getStatus)
                .eq(Question::getId, questionId)
                .eq(Question::getDeleteFlag, 0);
        Question question = oshQaQuestionMapper.selectOne(wrapper);
        if (question == null) {
            return R.fail(ResultCode.FAILED_NOT_EXISTS.getMsg());
        }
        if (question.getUserId() == null || !question.getUserId().equals(userId)) {
            return R.fail(ResultCode.FAILED_USER_PERMISSION_DENIED.getMsg());
        }
        question.setStatus(QUESTION_STATUS_PUBLISHED);
        oshQaQuestionMapper.update(question, new LambdaQueryWrapper<Question>().eq(Question::getId, questionId));
        return R.ok(ResultCode.SUCCESS.getMsg());
    }

    @Override
    public R<List<QueryQuestionListVO>> myDraft(Long userId) {
        LambdaQueryWrapper<Question> wrapper = new LambdaQueryWrapper<Question>().eq(Question::getUserId, userId).eq(Question::getStatus, 0);
        List<Question> questions = oshQaQuestionMapper.selectList(wrapper);
        List<QueryQuestionListVO> queryQuestionListVOS = new ArrayList<>();
        for (Question question : questions) {
            QueryQuestionListVO queryQuestionListVO = new QueryQuestionListVO();
            BeanUtils.copyProperties(question, queryQuestionListVO);
            queryQuestionListVOS.add(queryQuestionListVO);
        }
        return R.ok(queryQuestionListVOS);
    }

    @Override
    public R<String> editQuestion(Long userId, Long questionId, Long resourceNo, String resourceType, String content, Byte isPaidOnly, List<String> tags) {
        if (userId == null) return R.fail(ResultCode.FAILED_NOT_LOGIN.getMsg());
        if (questionId == null || resourceNo == null || !StringUtils.isNotEmpty(resourceType) || !StringUtils.isNotEmpty(content)) {
            return R.fail(ResultCode.FAILED_PARAMS_VALIDATE.getMsg());
        }
        if(!ResourcePermissionUtil.hasPermission(ResourceTypeEnum.fromTypeCode(resourceType),resourceNo)) {
            return R.fail(ResultCode.FAILED_USER_PERMISSION_DENIED.getMsg());
        }
        LambdaQueryWrapper<Question> wrapper = new LambdaQueryWrapper<Question>().select(Question::getUserId).eq(Question::getId, questionId);
        Question question = oshQaQuestionMapper.selectOne(wrapper);
        if (question == null) {
            return R.fail(ResultCode.FAILED_NOT_EXISTS.getMsg());
        }
        if (question.getUserId() == null || !question.getUserId().equals(userId)) {
            return R.fail(ResultCode.FAILED_USER_PERMISSION_DENIED.getMsg());
        }
        question.setResourceNo(resourceNo);
        question.setResourceType(resourceType);
        question.setContent(content);
        question.setIsPaidOnly(isPaidOnly);
        oshQaQuestionMapper.update(question, new LambdaQueryWrapper<Question>().eq(Question::getId, questionId));
        if (tags != null) {
            oshQaQuestionMapper.deleteQuestionTags(questionId);
            for (String tagName : tags) {
                Long tagId = resolveQATagId(tagName, userId);
                if (tagId != null) {
                    oshQaQuestionMapper.addQuestionTags(questionId, tagId, userId);
                }
            }
        }
        return R.ok(ResultCode.SUCCESS.getMsg());
    }

    @Override
    public R<String> deleteQuestion(Long userId, Long questionId) {
        if (userId == null) return R.fail(ResultCode.FAILED_NOT_LOGIN.getMsg());
        if (questionId == null) return R.fail(ResultCode.FAILED_PARAMS_VALIDATE.getMsg());
        LambdaQueryWrapper<Question> wrapper = new LambdaQueryWrapper<Question>()
                .select(Question::getUserId)
                .eq(Question::getId, questionId)
                .eq(Question::getDeleteFlag, 0);
        Question question = oshQaQuestionMapper.selectOne(wrapper);
        if (question == null) {
            return R.fail(ResultCode.FAILED_NOT_EXISTS.getMsg());
        }
        if (question.getUserId() == null || !question.getUserId().equals(userId)) {
            return R.fail(ResultCode.FAILED_USER_PERMISSION_DENIED.getMsg());
        }
        question.setDeleteFlag((byte) 1);
        oshQaQuestionMapper.update(question, new LambdaQueryWrapper<Question>().eq(Question::getId, questionId));
        return R.ok(ResultCode.SUCCESS.getMsg());
    }

    @Override
    public R<String> followQuestion(Long userId, Long questionId) {
        if (userId == null) return R.fail(ResultCode.FAILED_NOT_LOGIN.getMsg());
        if (questionId == null) return R.fail(ResultCode.FAILED_PARAMS_VALIDATE.getMsg());
        Integer deleteFlag = oshQaQuestionMapper.getFollowInfoByUserIdAndQuestionId(userId, questionId);
        if (deleteFlag != null && deleteFlag == 0) {
            return R.fail(ResultCode.FAILED.getMsg());
        }
        LambdaQueryWrapper<Question> questionWrapper = new LambdaQueryWrapper<Question>()
                .eq(Question::getId, questionId)
                .eq(Question::getDeleteFlag, 0);
        Question question = oshQaQuestionMapper.selectOne(questionWrapper);
        if (question == null) {
            return R.fail(ResultCode.FAILED_NOT_EXISTS.getMsg());
        }
        oshQaQuestionMapper.followQuestion(userId, questionId, userId);
        oshQaQuestionMapper.incrementFollowCount(questionId);
        return R.ok(ResultCode.SUCCESS.getMsg());
    }

    @Override
    public R<String> cancelFollowQuestion(Long userId, Long questionId) {
        if (userId == null) return R.fail(ResultCode.FAILED_NOT_LOGIN.getMsg());
        if (questionId == null) return R.fail(ResultCode.FAILED_PARAMS_VALIDATE.getMsg());
        Integer deleteFlag = oshQaQuestionMapper.getFollowInfoByUserIdAndQuestionId(userId, questionId);
        if (deleteFlag != null && deleteFlag == 0) {
            LambdaQueryWrapper<Question> questionWrapper = new LambdaQueryWrapper<Question>()
                    .eq(Question::getId, questionId)
                    .eq(Question::getDeleteFlag, 0);
            Question question = oshQaQuestionMapper.selectOne(questionWrapper);
            if (question == null) {
                return R.fail(ResultCode.FAILED_NOT_EXISTS.getMsg());
            }
            oshQaQuestionMapper.cancelFollowQuestion(userId, questionId ,userId);
            oshQaQuestionMapper.decrementFollowCount(questionId);
            return R.ok(ResultCode.SUCCESS.getMsg());
        }
        return R.fail(ResultCode.FAILED.getMsg());
    }

    @Override
    public TableDataInfo list(Long userId, Long resourceNo, String resourceType, String type, String keyword, Integer pageNum, Integer pageSize) {
        pageNum = pageNum == null || pageNum < 1 ? DEFAULT_PAGE_NUM : pageNum;
        pageSize = pageSize == null || pageSize < 1 ? DEFAULT_PAGE_SIZE : Math.min(pageSize, MAX_PAGE_SIZE);
        // 兼容旧数据：'course' 和 '课程' 都能查到
        final String normalizedResourceType;
        if ("course".equalsIgnoreCase(resourceType)) {
            normalizedResourceType = null; // 先不用 resourceType 过滤，下面用 IN 查询
        } else {
            normalizedResourceType = resourceType;
        }

        LambdaQueryWrapper<Question> wrapper = new LambdaQueryWrapper<Question>()
                // 1. 必须加：只查未删除的数据
                .eq(Question::getDeleteFlag, 0)
                .ne(Question::getStatus, QUESTION_STATUS_DRAFT)
                .eq(resourceNo != null, Question::getResourceNo, resourceNo)
                .eq(StringUtils.isNotEmpty(normalizedResourceType), Question::getResourceType, normalizedResourceType)
                .like(StringUtils.isNotEmpty(keyword), Question::getContent, keyword)
                .orderByDesc(Question::getViewCount);

        // 兼容 course 和 课程 两种历史数据
        if ("course".equalsIgnoreCase(resourceType)) {
            wrapper.in(Question::getResourceType, java.util.Arrays.asList("course", "课程"));
        }

        // 2. 这里的 type 判空处理
        if (StringUtils.isNotEmpty(type)) {
            if (QAQuestionSearchType.MY_QUESTIONS.getType().equals(type)) {
                if (userId == null) {
                    return new TableDataInfo(new ArrayList<>(), 0L);
                }
                wrapper.eq(Question::getUserId, userId);
            } else if (QAQuestionSearchType.MY_FOLLOWS.getType().equals(type)) {
                if (userId == null) {
                    return new TableDataInfo(new ArrayList<>(), 0L);
                }
                List<Long> followQuestionIds = oshQaQuestionMapper.getFollowQuestionIds(userId);
                if (CollectionUtils.isNotEmpty(followQuestionIds)) {
                    wrapper.in(Question::getId, followQuestionIds);
                } else {
                    wrapper.eq(Question::getId, -1L); // 没关注则查不到
                }
            } else if (QAQuestionSearchType.UNANSWERED.getType().equals(type)) {
                wrapper.eq(Question::getStatus, QUESTION_STATUS_PUBLISHED);
            } else if (QAQuestionSearchType.ANSWERED.getType().equals(type)) {
                wrapper.eq(Question::getStatus, QUESTION_STATUS_ANSWERED);
            }
        }

        // 3. 开始分页
        PageHelper.startPage(pageNum, pageSize);
        List<Question> questions = oshQaQuestionMapper.selectList(wrapper);

        // 4. 重要：先用原始 list 构建 PageInfo，确保 total 正确
        PageInfo<Question> entityPageInfo = new PageInfo<>(questions);

        // 5. 转换 VO
        List<QueryQuestionListVO> voList = questions.stream().map(question -> {
            QueryQuestionListVO vo = new QueryQuestionListVO();
            BeanUtils.copyProperties(question, vo);
            // 这里可以顺便处理下用户信息、时间格式等
            return vo;
        }).collect(Collectors.toList());

        // 6. 返回时手动把原 PageInfo 的 total 塞进去
        return new TableDataInfo(voList, entityPageInfo.getTotal());
    }

    @Override
    public R<String> solve(Long userId, Long questionId, Long answerId) {
        if (userId == null) return R.fail(ResultCode.FAILED_NOT_LOGIN.getMsg());
        if (questionId == null || answerId == null) return R.fail(ResultCode.FAILED_PARAMS_VALIDATE.getMsg());
        LambdaQueryWrapper<Answer> answerWrapper = new LambdaQueryWrapper<Answer>()
                .select(Answer::getQuestionId, Answer::getIsSolution, Answer::getStatus)
                .eq(Answer::getId, answerId)
                .eq(Answer::getDeleteFlag, 0);
        Answer answer = oshQaAnswerMapper.selectOne(answerWrapper);
        if (answer == null) {
            return R.fail(ResultCode.FAILED_NOT_EXISTS.getMsg());
        }
        if (!answer.getQuestionId().equals(questionId)) {
            return R.fail(ResultCode.FAILED.getMsg());
        }
        if (answer.getStatus() != null && answer.getStatus() != ANSWER_STATUS_NORMAL) {
            return R.fail(ResultCode.FAILED.getMsg());
        }
        LambdaQueryWrapper<Question> questionWrapper = new LambdaQueryWrapper<Question>()
                .select(Question::getUserId, Question::getStatus)
                .eq(Question::getId, questionId)
                .eq(Question::getDeleteFlag, 0);
        Question question = oshQaQuestionMapper.selectOne(questionWrapper);
        if (question == null) {
            return R.fail(ResultCode.FAILED_NOT_EXISTS.getMsg());
        }
        if (Byte.valueOf(ANSWER_IS_SOLUTION).equals(answer.getIsSolution())) {
            return R.fail(ResultCode.FAILED_USER_ANSWER_ALREADY_MARKED.getMsg());
        }
        Long solutionCount = oshQaAnswerMapper.selectCount(new LambdaQueryWrapper<Answer>()
                .eq(Answer::getQuestionId, questionId)
                .eq(Answer::getIsSolution, ANSWER_IS_SOLUTION)
                .eq(Answer::getDeleteFlag, 0));
        if (solutionCount > 0) {
            return R.fail(ResultCode.FAILED_USER_ANSWER_ALREADY_MARKED.getMsg());
        }
        if (question.getUserId().equals(userId) || UserContextUtil.getCurrentLevel() > 4) {
            answer.setIsSolution(ANSWER_IS_SOLUTION);
            oshQaAnswerMapper.update(answer, answerWrapper);
            question.setStatus(QUESTION_STATUS_ANSWERED);
            oshQaQuestionMapper.update(question, questionWrapper);
            return R.ok(ResultCode.SUCCESS.getMsg());
        }
        return R.fail(ResultCode.FAILED_USER_PERMISSION_DENIED.getMsg());
    }

    @Override
    public R<String> cancelSolve(Long userId, Long questionId, Long answerId) {
        if (userId == null) return R.fail(ResultCode.FAILED_NOT_LOGIN.getMsg());
        if (questionId == null || answerId == null) return R.fail(ResultCode.FAILED_PARAMS_VALIDATE.getMsg());
        LambdaQueryWrapper<Answer> answerWrapper = new LambdaQueryWrapper<Answer>()
                .select(Answer::getQuestionId, Answer::getIsSolution)
                .eq(Answer::getId, answerId)
                .eq(Answer::getDeleteFlag, 0);
        Answer answer = oshQaAnswerMapper.selectOne(answerWrapper);
        if (answer == null) {
            return R.fail(ResultCode.FAILED_NOT_EXISTS.getMsg());
        }
        if (!questionId.equals(answer.getQuestionId())) {
            return R.fail(ResultCode.FAILED.getMsg());
        }
        LambdaQueryWrapper<Question> questionWrapper = new LambdaQueryWrapper<Question>()
                .select(Question::getUserId, Question::getStatus)
                .eq(Question::getId, questionId)
                .eq(Question::getDeleteFlag, 0);
        Question question = oshQaQuestionMapper.selectOne(questionWrapper);
        if (question == null) {
            return R.fail(ResultCode.FAILED_NOT_EXISTS.getMsg());
        }
        if (!question.getUserId().equals(userId) && UserContextUtil.getCurrentLevel() <= 4) {
            return R.fail(ResultCode.FAILED_USER_PERMISSION_DENIED.getMsg());
        }

        answer.setIsSolution(ANSWER_IS_NOT_SOLUTION);
        oshQaAnswerMapper.update(answer, answerWrapper);
        question.setStatus(QUESTION_STATUS_ANSWERED);
        oshQaQuestionMapper.update(question, questionWrapper);
        return R.ok(ResultCode.SUCCESS.getMsg());
    }

    @Override
    public R<QueryQuestionDetailVO> detail(Long id, Long questionId) {
        if (questionId == null) return R.fail(ResultCode.FAILED_PARAMS_VALIDATE.getMsg());
        QueryQuestionDetailVO queryQuestionDetailVO = new QueryQuestionDetailVO();
        LambdaQueryWrapper<Question> questionWrapper = new LambdaQueryWrapper<Question>()
                .eq(Question::getId, questionId)
                .eq(Question::getDeleteFlag, 0)
                .ne(Question::getStatus, QUESTION_STATUS_DRAFT);
        Question question = oshQaQuestionMapper.selectOne(questionWrapper);
        if (question == null) {
            return R.fail(ResultCode.FAILED_NOT_EXISTS.getMsg());
        }
        BeanUtils.copyProperties(question, queryQuestionDetailVO);
        List<Long> tagIds = oshQaTagMapper.selectTagIdsByQuestionId(questionId);
        List<Tag> tags = null;
        if (!CollectionUtils.isEmpty(tagIds)) {
            tags = oshQaTagMapper.selectList(new LambdaQueryWrapper<Tag>().in(Tag::getId, tagIds));
        }
        if (tags != null) {
            ArrayList<String> tagNames = new ArrayList<>();
            for (Tag tag : tags) {
                tagNames.add(tag.getName());
            }
            queryQuestionDetailVO.setTags(tagNames);
        } else {
            queryQuestionDetailVO.setTags(null);
        }
        List<Answer> answers = oshQaAnswerMapper.selectList(new LambdaQueryWrapper<Answer>()
                .eq(Answer::getQuestionId, questionId)
                .eq(Answer::getDeleteFlag, 0)
                .eq(Answer::getStatus, ANSWER_STATUS_NORMAL)
                .orderByDesc(Answer::getIsSolution)
                .orderByDesc(Answer::getVoteCount));
        queryQuestionDetailVO.setAnswers(answers);
        oshQaQuestionMapper.incrementViewCount(questionId);
        return R.ok(queryQuestionDetailVO);
    }

    @Override
    public R<String> vote(Long userId, Long answerId) {
        if (userId == null) return R.fail(ResultCode.FAILED_NOT_LOGIN.getMsg());
        if (answerId == null) return R.fail(ResultCode.FAILED_PARAMS_VALIDATE.getMsg());
        Integer deleteFlag = oshQaAnswerMapper.getVoteInfoByUserIdAndAnswerId(userId, answerId);
        if (deleteFlag != null && deleteFlag == 0) {
            return R.fail(ResultCode.FAILED.getMsg());
        }
        LambdaQueryWrapper<Answer> answerWrapper = new LambdaQueryWrapper<Answer>()
                .select(Answer::getId)
                .eq(Answer::getId, answerId)
                .eq(Answer::getDeleteFlag, 0)
                .eq(Answer::getStatus, ANSWER_STATUS_NORMAL);
        Answer answer = oshQaAnswerMapper.selectOne(answerWrapper);
        if (answer == null) {
            return R.fail(ResultCode.FAILED_NOT_EXISTS.getMsg());
        }
        oshQaAnswerMapper.voteAnswer(userId, answerId, userId);
        oshQaAnswerMapper.incrementVoteCount(answerId);
        return R.ok(ResultCode.SUCCESS.getMsg());
    }

    @Override
    public R<String> cancelVote(Long userId, Long answerId) {
        if (userId == null) return R.fail(ResultCode.FAILED_NOT_LOGIN.getMsg());
        if (answerId == null) return R.fail(ResultCode.FAILED_PARAMS_VALIDATE.getMsg());
        Integer deleteFlag = oshQaAnswerMapper.getVoteInfoByUserIdAndAnswerId(userId, answerId);
        if (deleteFlag != null && deleteFlag == 0) {
            LambdaQueryWrapper<Answer> answerWrapper = new LambdaQueryWrapper<Answer>()
                    .select(Answer::getId)
                    .eq(Answer::getId, answerId)
                    .eq(Answer::getDeleteFlag, 0)
                    .eq(Answer::getStatus, ANSWER_STATUS_NORMAL);
            Answer answer = oshQaAnswerMapper.selectOne(answerWrapper);
            if (answer == null) {
                return R.fail(ResultCode.FAILED_NOT_EXISTS.getMsg());
            }
            oshQaAnswerMapper.cancelVoteAnswer(userId, answerId, userId);
            oshQaAnswerMapper.decrementVoteCount(answerId);
            return R.ok(ResultCode.SUCCESS.getMsg());
        }
        return R.fail(ResultCode.FAILED.getMsg());
    }

    /**
     * 根据标签名解析标签ID：存在则复用，不存在则自动创建。
     * 与课程模块的 resolveCourseTag 逻辑保持一致。
     */
    private Long resolveQATagId(String tagName, Long userId) {
        if (tagName == null || tagName.trim().isEmpty()) {
            return null;
        }
        String name = tagName.trim();
        Tag existing = oshQaTagMapper.selectTagByName(name);
        if (existing != null) {
            return existing.getId();
        }
        // 不存在则新建，createBy/updateBy 由 MyBatis-Plus 自动填充
        Tag tag = new Tag();
        tag.setName(name);
        tag.setType("custom");
        tag.setUseCount(0);
        try {
            oshQaTagMapper.insert(tag);
            return tag.getId();
        } catch (org.springframework.dao.DuplicateKeyException ex) {
            // 并发场景下可能已被其他请求插入，重新查一次
            Tag retry = oshQaTagMapper.selectTagByName(name);
            return retry != null ? retry.getId() : null;
        }
    }
}
