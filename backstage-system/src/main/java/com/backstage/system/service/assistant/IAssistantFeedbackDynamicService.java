package com.backstage.system.service.assistant;

import com.backstage.system.domain.assistant.vo.FeedbackDynamicVO;

import java.util.List;

/**
 * 反馈互动动态服务接口
 * <p>
 * 负责查询和组装反馈相关的实时互动事件（点赞、收藏等），
 * 用于前端跑马灯展示。
 *
 * @author backstage
 */
public interface IAssistantFeedbackDynamicService {

    /**
     * 查询最近互动动态
     * <p>
     * 联合查询点赞和收藏记录，关联用户信息和反馈标题，
     * 按时间倒序返回指定数量的动态事件。
     *
     * @param limit 返回数量限制
     * @return 动态事件列表
     */
    List<FeedbackDynamicVO> listRecentDynamics(Integer limit);
}
