package com.backstage.system.service.questionanswer;

import com.backstage.common.core.domain.R;
import com.backstage.common.core.page.TableDataInfo;
import com.backstage.system.domain.questionanswer.vo.QueryQuestionDetailVO;
import com.backstage.system.domain.questionanswer.vo.QueryQuestionListVO;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * Description:
 * OshUser: 九转苍翎
 * Date: 2026/3/24
 * Time: 21:50
 */
public interface IOshQAQuestionService {

    R<String> addQuestion(Long userId, Long resourceNo, String resourceType, String content, Byte isPaidOnly, List<String> tags);

    R<String> addToolQuestion(Long userId, Long toolId, String content, List<String> tags);

    R<String> publishQuestion(Long userId, Long questionId);

    R<List<QueryQuestionListVO>> myDraft(Long currentUserId);

    R<String> editQuestion(Long userId, Long questionId, Long resourceNo, String resourceType, String content, Byte isPaidOnly, List<String> tags);

    R<String> deleteQuestion(Long userId, Long questionId);

    R<String> followQuestion(Long userId, Long questionId);

    R<String> cancelFollowQuestion(Long userId, Long questionId);

    TableDataInfo list(Long userId, Long resourceNo, String resourceType, String type, String keyword, Integer pageNum, Integer pageSize);

    R<String> solve(Long userId, Long questionId, Long answerId);

    R<String> cancelSolve(Long userId, Long questionId, Long answerId);

    R<QueryQuestionDetailVO> detail(Long id, Long questionId);

    R<String> vote(Long userId, Long answerId);

    R<String> cancelVote(Long userId, Long answerId);
}
