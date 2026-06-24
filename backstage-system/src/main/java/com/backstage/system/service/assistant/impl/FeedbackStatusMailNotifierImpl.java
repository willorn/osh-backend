package com.backstage.system.service.assistant.impl;

import cn.hutool.core.util.StrUtil;
import com.backstage.common.async.AsyncExecutorNames;
import com.backstage.common.async.AsyncTaskSupport;
import com.backstage.system.domain.assistant.AssistantFeedback;
import com.backstage.system.domain.assistant.AssistantTicketStatus;
import com.backstage.system.domain.user.OshUser;
import com.backstage.system.mapper.user.OshUserMapper;
import com.backstage.system.service.assistant.FeedbackStatusMailNotifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.annotation.Resource;
import java.util.concurrent.Executor;

/**
 * 反馈状态邮件通知实现。
 *
 * @author backstage
 */
@Service
public class FeedbackStatusMailNotifierImpl implements FeedbackStatusMailNotifier {

    private static final Logger log = LoggerFactory.getLogger(FeedbackStatusMailNotifierImpl.class);

    @Resource
    private OshUserMapper oshUserMapper;

    @Resource
    private JavaMailSender javaMailSender;

    @Resource
    private AsyncTaskSupport asyncTaskSupport;

    @Resource
    @Qualifier(AsyncExecutorNames.NOTIFICATION)
    private Executor notificationTaskExecutor;

    @Value("${email.from}")
    private String from;

    @Override
    public void notifyStatusChanged(AssistantFeedback feedback,
                                    String fromStatus,
                                    String toStatus,
                                    String remark,
                                    String handlerName) {
        if (feedback == null || feedback.getId() == null || feedback.getUserId() == null) {
            return;
        }
        OshUser submitter = oshUserMapper.selectById(feedback.getUserId());
        if (submitter == null || StrUtil.isBlank(submitter.getEmail())) {
            log.info("反馈状态邮件通知跳过，提交人邮箱为空，feedbackId={}, userId={}", feedback.getId(), feedback.getUserId());
            return;
        }
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    dispatchMailAsync(feedback, submitter, fromStatus, toStatus, remark, handlerName);
                }
            });
            return;
        }
        dispatchMailAsync(feedback, submitter, fromStatus, toStatus, remark, handlerName);
    }

    private void dispatchMailAsync(AssistantFeedback feedback,
                                   OshUser submitter,
                                   String fromStatus,
                                   String toStatus,
                                   String remark,
                                   String handlerName) {
        asyncTaskSupport.runAsync(() -> sendMail(feedback, submitter, fromStatus, toStatus, remark, handlerName),
                        notificationTaskExecutor)
                .exceptionally(exception -> {
                    log.warn("反馈状态邮件异步发送失败，feedbackId={}, email={}, error={}",
                            feedback.getId(), submitter.getEmail(), exception.getMessage(), exception);
                    return null;
                });
    }

    private void sendMail(AssistantFeedback feedback,
                          OshUser submitter,
                          String fromStatus,
                          String toStatus,
                          String remark,
                          String handlerName) {
        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setFrom(from);
        mail.setTo(submitter.getEmail());
        mail.setSubject(buildSubject(feedback, toStatus));
        mail.setText(buildContent(feedback, submitter, fromStatus, toStatus, remark, handlerName));
        javaMailSender.send(mail);
        log.info("反馈状态邮件发送成功，feedbackId={}, toStatus={}, email={}",
                feedback.getId(), toStatus, submitter.getEmail());
    }

    private String buildSubject(AssistantFeedback feedback, String toStatus) {
        return "【反馈状态更新】《" + safeTitle(feedback.getTitle()) + "》已更新为" + AssistantTicketStatus.getDescriptionByCode(toStatus);
    }

    private String buildContent(AssistantFeedback feedback,
                                OshUser submitter,
                                String fromStatus,
                                String toStatus,
                                String remark,
                                String handlerName) {
        StringBuilder builder = new StringBuilder();
        builder.append("您好，").append(resolveSubmitterName(submitter)).append("：\n\n");
        builder.append("您提交的反馈《").append(safeTitle(feedback.getTitle())).append("》有新的处理进展。\n\n");
        builder.append("反馈标题：").append(safeTitle(feedback.getTitle())).append("\n");
        builder.append("状态变更：")
                .append(AssistantTicketStatus.getDescriptionByCode(fromStatus))
                .append(" -> ")
                .append(AssistantTicketStatus.getDescriptionByCode(toStatus))
                .append("\n");
        builder.append("当前状态：").append(AssistantTicketStatus.getDescriptionByCode(toStatus)).append("\n");
        builder.append("处理人：").append(StrUtil.blankToDefault(StrUtil.trim(handlerName), "系统")).append("\n");
        if (StrUtil.isNotBlank(remark)) {
            builder.append("处理说明：").append(remark.trim()).append("\n");
        }
        builder.append("查看链接：http://localhost:3000/feedback/detail/").append(feedback.getId()).append("\n\n");
        builder.append(resolveStatusHint(toStatus)).append("\n\n");
        builder.append("此邮件由系统自动发送，请勿直接回复。");
        return builder.toString();
    }

    private String resolveStatusHint(String toStatus) {
        if (AssistantTicketStatus.PENDING_CONFIRM.getCode().equals(toStatus)) {
            return "当前反馈已进入“待用户确认”状态，请及时前往确认处理结果。";
        }
        if (AssistantTicketStatus.REJECTED.getCode().equals(toStatus)) {
            return "当前反馈已被驳回，如需继续推进，请根据处理说明补充信息后重新处理。";
        }
        if (AssistantTicketStatus.CLOSED.getCode().equals(toStatus)) {
            return "当前反馈已关闭，如需继续讨论，可联系管理员重新打开。";
        }
        if (AssistantTicketStatus.REOPENED.getCode().equals(toStatus)) {
            return "当前反馈已重新打开，系统将继续跟进处理。";
        }
        return "你可以点击上方链接查看反馈详情与完整处理记录。";
    }

    private String resolveSubmitterName(OshUser submitter) {
        return StrUtil.blankToDefault(StrUtil.trim(submitter.getUsername()), "用户");
    }

    private String safeTitle(String title) {
        return StrUtil.blankToDefault(StrUtil.trim(title), "未命名反馈");
    }
}
