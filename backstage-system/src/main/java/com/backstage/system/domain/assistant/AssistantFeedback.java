package com.backstage.system.domain.assistant;

import com.backstage.common.core.domain.entity.OSHBaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * AI 助手反馈工单实体
 *
 * @author backstage
 */
@TableName("assistant_feedback")
public class AssistantFeedback extends OSHBaseEntity {

    /**
     * 工单 ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 提交用户 ID
     */
    private Long userId;

    /**
     * 分类 ID
     */
    private Long categoryId;

    /**
     * 工单编号
     */
    private String ticketNo;

    /**
     * 反馈标题
     */
    private String title;

    /**
     * 反馈详细内容
     */
    private String content;

    /**
     * 反馈图片JSON数组（最多9张）
     */
    private String images;

    /**
     * 反馈状态（PENDING、PROCESSING、RESOLVED、CLOSED）
     */
    private String status;

    /**
     * 是否置顶（0-否 1-是）
     */
    private Integer isPinned;

    /**
     * 置顶排序（1-3，0 表示不置顶）
     */
    private Integer pinOrder;

    /**
     * 评论数量
     */
    private Integer commentCount;

    /**
     * 浏览次数
     */
    private Integer viewCount;

    /**
     * 点赞数量
     */
    private Integer likeCount;

    /**
     * 收藏数量
     */
    private Integer favoriteCount;

    /**
     * 处理结果说明
     */
    private String result;

    /**
     * 反馈来源页面路径
     */
    private String pagePath;

    /**
     * 处理人 ID
     */
    private Long handlerId;

    /**
     * 处理人名称
     */
    private String handlerName;

    /**
     * 最近处理时间
     */
    private LocalDateTime handledTime;

    /**
     * 关闭原因
     */
    private String closeReason;

    /**
     * 热度分（冗余字段，用于排序）
     * 计算方式：互动分 * 4 + 有效浏览 * 1
     * 有效浏览：超过互动分20倍的部分只算10%
     */
    private Integer hotScore;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getTicketNo() {
        return ticketNo;
    }

    public void setTicketNo(String ticketNo) {
        this.ticketNo = ticketNo;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getImages() {
        return images;
    }

    public void setImages(String images) {
        this.images = images;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getIsPinned() {
        return isPinned;
    }

    public void setIsPinned(Integer isPinned) {
        this.isPinned = isPinned;
    }

    public Integer getPinOrder() {
        return pinOrder;
    }

    public void setPinOrder(Integer pinOrder) {
        this.pinOrder = pinOrder;
    }

    public Integer getCommentCount() {
        return commentCount;
    }

    public void setCommentCount(Integer commentCount) {
        this.commentCount = commentCount;
    }

    public Integer getViewCount() {
        return viewCount;
    }

    public void setViewCount(Integer viewCount) {
        this.viewCount = viewCount;
    }

    public Integer getLikeCount() {
        return likeCount;
    }

    public void setLikeCount(Integer likeCount) {
        this.likeCount = likeCount;
    }

    public Integer getFavoriteCount() {
        return favoriteCount;
    }

    public void setFavoriteCount(Integer favoriteCount) {
        this.favoriteCount = favoriteCount;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getPagePath() {
        return pagePath;
    }

    public void setPagePath(String pagePath) {
        this.pagePath = pagePath;
    }

    public Long getHandlerId() {
        return handlerId;
    }

    public void setHandlerId(Long handlerId) {
        this.handlerId = handlerId;
    }

    public String getHandlerName() {
        return handlerName;
    }

    public void setHandlerName(String handlerName) {
        this.handlerName = handlerName;
    }

    public LocalDateTime getHandledTime() {
        return handledTime;
    }

    public void setHandledTime(LocalDateTime handledTime) {
        this.handledTime = handledTime;
    }

    public String getCloseReason() {
        return closeReason;
    }

    public void setCloseReason(String closeReason) {
        this.closeReason = closeReason;
    }

    public Integer getHotScore() {
        return hotScore;
    }

    public void setHotScore(Integer hotScore) {
        this.hotScore = hotScore;
    }
}
