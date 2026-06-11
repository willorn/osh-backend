package com.backstage.system.domain.assistant.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.Date;

/**
 * 反馈互动动态 VO
 * <p>
 * 用于展示反馈列表页的实时互动事件（点赞、收藏等），
 * 格式兼容 AnnouncementMarquee 组件，可直接用于跑马灯渲染。
 *
 * @author backstage
 */
@ApiModel(description = "反馈互动动态")
public class FeedbackDynamicVO {

    @ApiModelProperty("动态 ID")
    private Long id;

    @ApiModelProperty("动态内容（如：张三 点赞了 《xxx 反馈》）")
    private String title;

    @ApiModelProperty("互动类型：LIKE-点赞, FAVORITE-收藏")
    private String type;

    @ApiModelProperty("用户 ID")
    private Long userId;

    @ApiModelProperty("反馈 ID")
    private Long feedbackId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty("互动时间")
    private Date createTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getFeedbackId() {
        return feedbackId;
    }

    public void setFeedbackId(Long feedbackId) {
        this.feedbackId = feedbackId;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }
}
