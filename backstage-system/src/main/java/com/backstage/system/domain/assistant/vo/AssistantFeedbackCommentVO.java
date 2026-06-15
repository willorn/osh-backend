package com.backstage.system.domain.assistant.vo;

import java.time.LocalDateTime;
import java.util.List;

/**
 * AI 助手反馈评论响应 VO
 *
 * @author backstage
 */
public class AssistantFeedbackCommentVO {

    /**
     * 评论 ID
     */
    private Long id;

    /**
     * 反馈 ID
     */
    private Long feedbackId;

    /**
     * 评论用户 ID
     */
    private Long userId;

    /**
     * 评论用户名
     */
    private String userName;

    /**
     * 评论用户头像
     */
    private String userAvatar;

    /**
     * 评论内容
     */
    private String content;

    /**
     * 评论图片列表
     */
    private List<String> images;

    /**
     * 父评论 ID（0 表示一级评论）
     */
    private Long parentId;

    /**
     * 根评论 ID（用于二级评论）
     */
    private Long rootId;

    /**
     * 回复的用户 ID
     */
    private Long replyToUserId;

    /**
     * 回复的用户名
     */
    private String replyToUserName;

    /**
     * 评论层级（1-一级评论，2-二级评论/回复）
     */
    private Integer commentLevel;

    /**
     * 是否管理员回复（0-否 1-是）
     */
    private Integer isAdminReply;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 二级评论列表（仅一级评论有此字段）
     */
    private List<AssistantFeedbackCommentVO> replies;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getFeedbackId() {
        return feedbackId;
    }

    public void setFeedbackId(Long feedbackId) {
        this.feedbackId = feedbackId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserAvatar() {
        return userAvatar;
    }

    public void setUserAvatar(String userAvatar) {
        this.userAvatar = userAvatar;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public List<String> getImages() {
        return images;
    }

    public void setImages(List<String> images) {
        this.images = images;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public Long getRootId() {
        return rootId;
    }

    public void setRootId(Long rootId) {
        this.rootId = rootId;
    }

    public Long getReplyToUserId() {
        return replyToUserId;
    }

    public void setReplyToUserId(Long replyToUserId) {
        this.replyToUserId = replyToUserId;
    }

    public String getReplyToUserName() {
        return replyToUserName;
    }

    public void setReplyToUserName(String replyToUserName) {
        this.replyToUserName = replyToUserName;
    }

    public Integer getCommentLevel() {
        return commentLevel;
    }

    public void setCommentLevel(Integer commentLevel) {
        this.commentLevel = commentLevel;
    }

    public Integer getIsAdminReply() {
        return isAdminReply;
    }

    public void setIsAdminReply(Integer isAdminReply) {
        this.isAdminReply = isAdminReply;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public List<AssistantFeedbackCommentVO> getReplies() {
        return replies;
    }

    public void setReplies(List<AssistantFeedbackCommentVO> replies) {
        this.replies = replies;
    }
}
