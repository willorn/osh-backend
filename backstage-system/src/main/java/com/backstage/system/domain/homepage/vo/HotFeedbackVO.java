package com.backstage.system.domain.homepage.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 首页热门用户反馈 VO
 *
 * @author jayTatum
 */
@ApiModel(description = "首页热门用户反馈")
public class HotFeedbackVO {

    @ApiModelProperty("反馈ID")
    private Long id;

    @ApiModelProperty("反馈标题")
    private String title;

    @ApiModelProperty("反馈内容摘要")
    private String summary;

    @ApiModelProperty("分类名称")
    private String category;

    @ApiModelProperty("分类图标 emoji")
    private String categoryIcon;

    @ApiModelProperty("状态：PENDING/PROCESSING/RESOLVED/CLOSED/PENDING_USER_CONFIRM")
    private String status;

    @ApiModelProperty("提交用户名")
    private String username;

    @ApiModelProperty("第一个标签名称")
    private String tagName;

    @ApiModelProperty("点赞数")
    private Integer likeCount;

    @ApiModelProperty("浏览数")
    private Integer viewCount;

    @ApiModelProperty("收藏数")
    private Integer collectCount;

    @ApiModelProperty("详情页跳转路径")
    private String detailUrl;

    // ========== getter / setter ==========

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getCategoryIcon() { return categoryIcon; }
    public void setCategoryIcon(String categoryIcon) { this.categoryIcon = categoryIcon; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getTagName() { return tagName; }
    public void setTagName(String tagName) { this.tagName = tagName; }

    public Integer getLikeCount() { return likeCount; }
    public void setLikeCount(Integer likeCount) { this.likeCount = likeCount; }

    public Integer getViewCount() { return viewCount; }
    public void setViewCount(Integer viewCount) { this.viewCount = viewCount; }

    public Integer getCollectCount() { return collectCount; }
    public void setCollectCount(Integer collectCount) { this.collectCount = collectCount; }

    public String getDetailUrl() { return detailUrl; }
    public void setDetailUrl(String detailUrl) { this.detailUrl = detailUrl; }
}
