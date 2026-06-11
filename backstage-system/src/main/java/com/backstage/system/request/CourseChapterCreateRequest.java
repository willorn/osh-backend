package com.backstage.system.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.apache.commons.lang3.StringUtils;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.PositiveOrZero;

@ApiModel("一级章节创建/修改请求")
public class CourseChapterCreateRequest {


    @ApiModelProperty(value = "章节ID（有则更新，无则新增）", example = "200")
    private Long id;

    @ApiModelProperty(value = "课程ID", required = true, example = "100")
    private Long courseId;

    @ApiModelProperty(value = "章节标题", required = true, example = "第一章")
    @NotBlank(message = "章节标题不能为空")
    private String title;

    @ApiModelProperty(value = "排序值，越小越靠前", required = true, example = "1")
    @NotNull(message = "排序不能为空")
    @PositiveOrZero(message = "排序不能小于0")
    private Integer sort;

    @ApiModelProperty(value = "章节类型：留空=普通章；course_link=引入课程作为章", example = "course_link")
    private String type;

    @ApiModelProperty(value = "引入的课程ID（type=course_link 时必填）", example = "100")
    private Long linkedCourseId;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = StringUtils.trimToNull(type);
    }

    public Long getLinkedCourseId() {
        return linkedCourseId;
    }

    public void setLinkedCourseId(Long linkedCourseId) {
        this.linkedCourseId = linkedCourseId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCourseId() {
        return courseId;
    }

    public void setCourseId(Long courseId) {
        this.courseId = courseId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = StringUtils.trimToNull(title);
    }

    public Integer getSort() {
        return sort;
    }

    public void setSort(Integer sort) {
        this.sort = sort;
    }
}
