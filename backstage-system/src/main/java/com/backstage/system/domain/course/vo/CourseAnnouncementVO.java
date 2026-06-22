package com.backstage.system.domain.course.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.time.LocalDateTime;

@ApiModel(description = "课程模块公告")
public class CourseAnnouncementVO {

    @ApiModelProperty(value = "公告ID", example = "1")
    private Long id;

    @ApiModelProperty(value = "公告标题", example = "课程模块维护通知")
    private String title;

    @ApiModelProperty(value = "公告跳转链接", example = "/course_detail/1000")
    private String link;

    @ApiModelProperty(value = "创建时间", example = "2026-05-21 10:00:00")
    private LocalDateTime createTime;

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

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}
