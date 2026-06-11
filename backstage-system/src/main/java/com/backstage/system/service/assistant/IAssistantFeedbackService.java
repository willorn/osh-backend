package com.backstage.system.service.assistant;

import com.backstage.common.core.page.TableDataInfo;
import com.backstage.system.domain.assistant.AssistantFeedback;
import com.backstage.system.domain.assistant.dto.AssistantFeedbackCreateDTO;
import com.backstage.system.domain.assistant.dto.AssistantFeedbackPageDTO;
import com.backstage.system.domain.assistant.dto.AssistantTicketStatusUpdateDTO;
import com.backstage.system.domain.assistant.vo.AssistantFeedbackDetailVO;
import com.backstage.system.domain.assistant.vo.AssistantFeedbackProcessRecordVO;
import com.backstage.system.domain.assistant.vo.AssistantFeedbackVO;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * AI 助手反馈服务接口
 *
 * @author backstage
 */
public interface IAssistantFeedbackService extends IService<AssistantFeedback> {

    /**
     * 创建反馈工单
     *
     * @param userId 用户 ID
     * @param dto    反馈创建 DTO
     * @return 创建的反馈工单
     */
    AssistantFeedbackVO createFeedback(Long userId, AssistantFeedbackCreateDTO dto);

    /**
     * 创建反馈工单
     *
     * @param userId 用户 ID
     * @param dto 反馈创建 DTO
     * @param allowAdminOnly 是否允许管理员专用分类
     * @return 创建的反馈工单
     */
    AssistantFeedbackVO createFeedback(Long userId, AssistantFeedbackCreateDTO dto, boolean allowAdminOnly);

    /**
     * 获取我的反馈列表（分页）
     *
     * @param userId 用户 ID
     * @return 分页结果
     */
    TableDataInfo getMyFeedback(Long userId);

    /**
     * 更新工单状态
     *
     * @param ticketId  工单 ID
     * @param handlerId 处理人 ID
     * @param dto       状态更新 DTO
     * @return 更新后的工单信息
     */
    AssistantFeedbackVO updateTicketStatus(Long ticketId, Long handlerId, AssistantTicketStatusUpdateDTO dto);

    /**
     * 提交人确认工单结果。
     *
     * @param feedbackId 工单 ID
     * @param userId     当前用户 ID
     * @param dto        确认请求
     * @return 更新后的工单信息
     */
    AssistantFeedbackVO confirmTicketStatus(Long feedbackId, Long userId, AssistantTicketStatusUpdateDTO dto);

    /**
     * 统计当前用户待确认工单数。
     *
     * @param userId 用户 ID
     * @return 待确认工单数
     */
    long countPendingConfirmTickets(Long userId);

    /**
     * 处理待确认工单的提醒与自动确认。
     */
    void processPendingConfirmTickets();

    /**
     * 分页查询反馈列表（公开接口）
     *
     * @param dto 查询条件
     * @return 分页结果
     */
    TableDataInfo pageFeedback(AssistantFeedbackPageDTO dto);

    /**
     * 前台反馈列表（轻量字段）
     *
     * @param dto 查询条件
     * @return 分页结果
     */
    TableDataInfo pageFeedbackList(AssistantFeedbackPageDTO dto);

    /**
     * 获取反馈详情
     *
     * @param feedbackId 反馈 ID
     * @return 反馈详情
     */
    AssistantFeedbackDetailVO getFeedbackDetail(Long feedbackId);

    /**
     * 获取不增加浏览量的工单状态摘要。
     *
     * @param feedbackId 工单 ID
     * @return 工单摘要
     */
    AssistantFeedbackDetailVO getFeedbackStatusSummary(Long feedbackId);

    /**
     * 查询反馈处理记录。
     *
     * @param feedbackId 反馈 ID
     * @return 处理记录时间线
     */
    List<AssistantFeedbackProcessRecordVO> listFeedbackProcessRecords(Long feedbackId);

}
