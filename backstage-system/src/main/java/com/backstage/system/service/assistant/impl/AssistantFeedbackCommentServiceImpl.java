package com.backstage.system.service.assistant.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.backstage.common.exception.ServiceException;
import com.backstage.system.domain.assistant.AssistantFeedback;
import com.backstage.system.domain.assistant.AssistantFeedbackComment;
import com.backstage.system.domain.assistant.dto.AssistantFeedbackCommentCreateDTO;
import com.backstage.system.domain.assistant.vo.AssistantFeedbackCommentVO;
import com.backstage.system.domain.user.OshUser;
import com.backstage.system.mapper.assistant.AssistantFeedbackCommentMapper;
import com.backstage.system.mapper.user.OshUserMapper;
import com.backstage.system.service.assistant.IAssistantFeedbackCommentService;
import com.backstage.system.service.assistant.IAssistantFeedbackService;
import com.backstage.system.utils.UserContextUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * AI 助手反馈评论 Service 实现
 *
 * @author backstage
 */
@Service
public class AssistantFeedbackCommentServiceImpl
        extends ServiceImpl<AssistantFeedbackCommentMapper, AssistantFeedbackComment>
        implements IAssistantFeedbackCommentService {

    public AssistantFeedbackCommentServiceImpl(IAssistantFeedbackService feedbackService, OshUserMapper oshUserMapper) {
        this.feedbackService = feedbackService;
        this.oshUserMapper = oshUserMapper;
    }

    private final IAssistantFeedbackService feedbackService;
    private final OshUserMapper oshUserMapper;

    @Override
    public List<AssistantFeedbackCommentVO> listCommentsByFeedbackId(Long feedbackId, Integer pageNum, Integer pageSize) {
        if (getActiveFeedback(feedbackId) == null) {
            throw new ServiceException("反馈不存在");
        }
        // 查询一级评论（分页）
        Page<AssistantFeedbackComment> page = lambdaQuery()
                .eq(AssistantFeedbackComment::getFeedbackId, feedbackId)
                .eq(AssistantFeedbackComment::getCommentLevel, 1)
                .orderByAsc(AssistantFeedbackComment::getCreateTime)
                .page(new Page<>(pageNum, pageSize));

        List<AssistantFeedbackComment> rootComments = page.getRecords();
        if (rootComments.isEmpty()) {
            return Collections.emptyList();
        }

        // 查询所有一级评论的二级评论
        List<Long> rootIds = rootComments.stream()
                .map(AssistantFeedbackComment::getId)
                .collect(Collectors.toList());

        List<AssistantFeedbackComment> replies = lambdaQuery()
                .in(AssistantFeedbackComment::getRootId, rootIds)
                .eq(AssistantFeedbackComment::getCommentLevel, 2)
                .orderByAsc(AssistantFeedbackComment::getCreateTime)
                .list();

        Map<Long, OshUser> userMap = buildUserMap(rootComments, replies);

        List<AssistantFeedbackCommentVO> result = page.getRecords().stream()
                .map(comment -> toCommentVO(comment, userMap))
                .collect(Collectors.toList());

        // 按根评论 ID 分组
        Map<Long, List<AssistantFeedbackCommentVO>> repliesMap = replies.stream()
                .map(reply -> toCommentVO(reply, userMap))
                .collect(Collectors.groupingBy(AssistantFeedbackCommentVO::getRootId));

        // 将二级评论填充到一级评论中
        result.forEach(comment -> comment.setReplies(repliesMap.get(comment.getId())));

        return result;
    }

    private AssistantFeedbackCommentVO toCommentVO(AssistantFeedbackComment comment, Map<Long, OshUser> userMap) {
        AssistantFeedbackCommentVO commentVO = new AssistantFeedbackCommentVO();
        BeanUtil.copyProperties(comment, commentVO);

        // 解析图片JSON数组
        if (StrUtil.isNotBlank(comment.getImages())) {
            try {
                JSONArray jsonArray = JSON.parseArray(comment.getImages());
                List<String> imageList = jsonArray.toJavaList(String.class);
                commentVO.setImages(imageList);
            } catch (Exception e) {
                // JSON解析失败，设置为空列表
                commentVO.setImages(Collections.emptyList());
            }
        } else {
            commentVO.setImages(Collections.emptyList());
        }

        OshUser user = userMap.get(comment.getUserId());
        if (user != null) {
            commentVO.setUserName(StrUtil.isNotBlank(user.getUsername()) ? user.getUsername() : "匿名用户");
            commentVO.setUserAvatar(user.getAvatar());
        }
        return commentVO;
    }

    private Map<Long, OshUser> buildUserMap(List<AssistantFeedbackComment> rootComments, List<AssistantFeedbackComment> replies) {
        Set<Long> userIds = rootComments.stream()
                .map(AssistantFeedbackComment::getUserId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        userIds.addAll(replies.stream()
                .map(AssistantFeedbackComment::getUserId)
                .filter(id -> id != null)
                .collect(Collectors.toSet()));
        if (userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return oshUserMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(OshUser::getId, Function.identity()));
    }

    private AssistantFeedback getActiveFeedback(Long feedbackId) {
        return feedbackService.lambdaQuery()
                .eq(AssistantFeedback::getId, feedbackId)
                .eq(AssistantFeedback::getDeleteFlag, (byte) 0)
                .one();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createComment(Long feedbackId, AssistantFeedbackCommentCreateDTO dto) {
        // 校验反馈是否存在
        AssistantFeedback feedback = getActiveFeedback(feedbackId);
        if (feedback == null) {
            throw new ServiceException("反馈不存在");
        }

        // 获取当前用户ID
        Long currentUserId = UserContextUtil.getCurrentUserId();
        if (currentUserId == null) {
            throw new ServiceException("请先登录");
        }

        // 校验内容和图片不能同时为空
        boolean hasContent = dto.getContent() != null && !dto.getContent().trim().isEmpty();
        boolean hasImages = dto.getImages() != null && !dto.getImages().isEmpty();
        if (!hasContent && !hasImages) {
            throw new ServiceException("评论内容和图片不能同时为空");
        }

        // 校验图片数量
        if (dto.getImages() != null && dto.getImages().size() > 9) {
            throw new ServiceException("最多只能上传9张图片");
        }

        // 构建评论对象
        AssistantFeedbackComment comment = new AssistantFeedbackComment();
        comment.setFeedbackId(feedbackId);
        comment.setUserId(currentUserId);
        comment.setContent(dto.getContent());

        // 图片列表转JSON字符串
        if (dto.getImages() != null && !dto.getImages().isEmpty()) {
            comment.setImages(JSON.toJSONString(dto.getImages()));
        }

        comment.setParentId(dto.getParentId() != null ? dto.getParentId() : 0L);
        comment.setReplyToUserId(dto.getReplyToUserId());
        comment.setReplyToUserName(dto.getReplyToUserName());

        // 判断评论层级
        if (dto.getParentId() == null || dto.getParentId() == 0) {
            // 一级评论
            comment.setCommentLevel(1);
            comment.setRootId(0L);
        } else {
            // 二级评论
            comment.setCommentLevel(2);
            // 查找根评论ID
            AssistantFeedbackComment parentComment = getById(dto.getParentId());
            if (parentComment == null) {
                throw new ServiceException("父评论不存在");
            }
            comment.setRootId(parentComment.getCommentLevel() == 1 ? parentComment.getId() : parentComment.getRootId());
        }

        comment.setIsAdminReply(0);
        comment.setCreateTime(LocalDateTime.now());
        comment.setCreateBy(currentUserId);

        // 保存评论
        save(comment);

        // 更新反馈的评论数
        feedbackService.lambdaUpdate()
                .eq(AssistantFeedback::getId, feedbackId)
                .setSql("comment_count = comment_count + 1")
                .update();

        return comment.getId();
    }
}
