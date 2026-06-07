package com.backstage.system.service.assistant;

import com.backstage.common.response.PageResponse;
import com.backstage.system.domain.assistant.AssistantFeedback;
import com.backstage.system.domain.assistant.dto.AssistantFeedbackPageDTO;

public interface IAssistantFeedbackEsService {

    PageResponse<AssistantFeedback> searchFeedbacks(AssistantFeedbackPageDTO dto);

    int syncAllFeedbacksToEs();

    void upsertFeedbackById(Long feedbackId);

    void deleteFeedbackById(Long feedbackId);

    /**
     * 重建 ES 索引（删除旧索引 → 创建新索引 → 全量同步数据）
     * <p>
     * 适用场景：
     * 1. mapping 变更（如字段类型从 text 改为 keyword）
     * 2. 分片数、副本数等 settings 调整
     * 3. 分析器配置变更
     * </p>
     *
     * @return 同步的文档总数
     */
    int rebuildIndex();
}
