package com.backstage.system.service.assistant.impl;

import com.backstage.system.domain.assistant.AssistantFeedback;
import com.backstage.system.domain.assistant.vo.FeedbackDynamicVO;
import com.backstage.system.domain.user.OshUser;
import com.backstage.system.mapper.assistant.AssistantFeedbackDynamicMapper;
import com.backstage.system.service.assistant.IAssistantFeedbackDynamicService;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 反馈互动动态服务实现
 *
 * @author backstage
 */
@Service
public class AssistantFeedbackDynamicServiceImpl implements IAssistantFeedbackDynamicService {

    private static final Logger log = LoggerFactory.getLogger(AssistantFeedbackDynamicServiceImpl.class);

    /**
     * 互动类型枚举
     */
    private static final String TYPE_LIKE = "LIKE";
    private static final String TYPE_FAVORITE = "FAVORITE";

    public AssistantFeedbackDynamicServiceImpl(AssistantFeedbackDynamicMapper feedbackDynamicMapper) {
        this.feedbackDynamicMapper = feedbackDynamicMapper;
    }

    private final AssistantFeedbackDynamicMapper feedbackDynamicMapper;

    @Override
    public List<FeedbackDynamicVO> listRecentDynamics(Integer limit) {
        // 1. 使用 XML Mapper 查询（MyBatis 自动映射到 VO，无需手动转换）
        List<FeedbackDynamicVO> dynamics = feedbackDynamicMapper.selectRecentDynamics(limit, limit, limit);

        // 2. 批量查询关联数据（用户名 + 反馈标题）
        if (!dynamics.isEmpty()) {
            Set<Long> userIds = dynamics.stream()
                    .map(FeedbackDynamicVO::getUserId)
                    .collect(Collectors.toSet());

            Set<Long> feedbackIds = dynamics.stream()
                    .map(FeedbackDynamicVO::getFeedbackId)
                    .collect(Collectors.toSet());

            // 批量查询用户信息
            Map<Long, OshUser> userMap = Db.lambdaQuery(OshUser.class)
                    .in(OshUser::getId, userIds)
                    .list()
                    .stream()
                    .collect(Collectors.toMap(OshUser::getId, u -> u, (a, b) -> a));

            // 批量查询反馈信息
            Map<Long, AssistantFeedback> feedbackMap = Db.lambdaQuery(AssistantFeedback.class)
                    .in(AssistantFeedback::getId, feedbackIds)
                    .list()
                    .stream()
                    .collect(Collectors.toMap(AssistantFeedback::getId, f -> f, (a, b) -> a));

            // 3. 组装动态文案
            dynamics.forEach(vo -> {
                OshUser user = userMap.get(vo.getUserId());
                AssistantFeedback feedback = feedbackMap.get(vo.getFeedbackId());

                String userName = user != null ? user.getUsername() : "某用户";
                String feedbackTitle = feedback != null ? feedback.getTitle() : "某反馈";

                // 根据类型组装文案
                if (TYPE_LIKE.equals(vo.getType())) {
                    vo.setTitle(userName + " 点赞了《" + truncate(feedbackTitle, 20) + "》");
                } else {
                    vo.setTitle(userName + " 收藏了《" + truncate(feedbackTitle, 20) + "》");
                }
            });
        }

        log.info("查询到 {} 条最近互动动态", dynamics.size());
        return dynamics;
    }

    /**
     * 截断字符串，避免文案过长
     *
     * @param str       原始字符串
     * @param maxLength 最大长度
     * @return 截断后的字符串
     */
    private String truncate(String str, int maxLength) {
        if (str == null) {
            return "";
        }
        if (str.length() <= maxLength) {
            return str;
        }
        return str.substring(0, maxLength) + "...";
    }
}
