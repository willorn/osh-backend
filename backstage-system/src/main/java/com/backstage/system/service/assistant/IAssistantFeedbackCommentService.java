package com.backstage.system.service.assistant;

import com.backstage.system.domain.assistant.AssistantFeedbackComment;
import com.backstage.system.domain.assistant.vo.AssistantFeedbackCommentVO;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * AI 助手反馈评论 Service 接口
 *
 * @author backstage
 */
public interface IAssistantFeedbackCommentService extends IService<AssistantFeedbackComment> {

    /**
     * 获取反馈的评论列表（2级结构）
     *
     * @param feedbackId 反馈 ID
     * @param pageNum    页码
     * @param pageSize   每页数量
     * @return 评论列表
     */
    List<AssistantFeedbackCommentVO> listCommentsByFeedbackId(Long feedbackId, Integer pageNum, Integer pageSize);
}
