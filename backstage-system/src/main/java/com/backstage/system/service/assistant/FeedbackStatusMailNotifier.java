package com.backstage.system.service.assistant;

import com.backstage.system.domain.assistant.AssistantFeedback;

/**
 * 反馈状态邮件通知器。
 *
 * @author backstage
 */
public interface FeedbackStatusMailNotifier {

    /**
     * 在反馈状态更新后，向提交人发送邮件通知。
     *
     * @param feedback    反馈工单
     * @param fromStatus  变更前状态
     * @param toStatus    变更后状态
     * @param remark      处理说明
     * @param handlerName 处理人名称
     */
    void notifyStatusChanged(AssistantFeedback feedback,
                             String fromStatus,
                             String toStatus,
                             String remark,
                             String handlerName);
}
