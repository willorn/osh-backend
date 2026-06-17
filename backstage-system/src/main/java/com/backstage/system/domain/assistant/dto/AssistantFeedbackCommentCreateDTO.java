package com.backstage.system.domain.assistant.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

/**
 * 反馈评论创建 DTO
 *
 * @author backstage
 */
@ApiModel("反馈评论创建DTO")
public class AssistantFeedbackCommentCreateDTO {

    @ApiModelProperty(value = "评论内容")
    @Size(max = 500, message = "评论内容不能超过500字")
    private String content;

    @ApiModelProperty(value = "评论图片URL列表（最多9张）")
    @Size(max = 9, message = "最多上传9张图片")
    private List<String> images;

    @ApiModelProperty(value = "父评论ID（0表示一级评论）", required = true)
    @NotNull(message = "父评论ID不能为空")
    private Long parentId;

    @ApiModelProperty(value = "回复的用户ID")
    private Long replyToUserId;

    @ApiModelProperty(value = "回复的用户名")
    private String replyToUserName;

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
}
