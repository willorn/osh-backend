package com.backstage.system.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.apache.commons.lang3.StringUtils;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import javax.validation.constraints.PositiveOrZero;

/**
 * 引入课程作为小节的创建请求。
 * 在指定一级章（parentId）下新增一个 type=course_link 的小节，
 * 点击该小节标题跳转到 linkedCourseId 对应的课程详情页。
 */
@ApiModel("引入课程作为小节请求")
public class CourseLinkSectionCreateRequest {

    @ApiModelProperty(value = "课程ID", required = true, example = "100")
    @NotNull(message = "课程ID不能为空")
    private Long courseId;

    @ApiModelProperty(value = "父级章ID（一级章）", required = true, example = "200")
    @NotNull(message = "父级章ID不能为空")
    @Positive(message = "父级章ID必须为正数")
    private Long parentId;

    @ApiModelProperty(value = "小节标题", required = true, example = "引入的课程名")
    @NotBlank(message = "小节标题不能为空")
    private String title;

    @ApiModelProperty(value = "排序值，越小越靠前", required = true, example = "1")
    @NotNull(message = "排序不能为空")
    @PositiveOrZero(message = "排序不能小于0")
    private Integer sort;

    @ApiModelProperty(value = "引入的课程ID", required = true, example = "101")
    @NotNull(message = "引入的课程ID不能为空")
    private Long linkedCourseId;

    public Long getCourseId() {
        return courseId;
    }

    public void setCourseId(Long courseId) {
        this.courseId = courseId;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
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

    public Long getLinkedCourseId() {
        return linkedCourseId;
    }

    public void setLinkedCourseId(Long linkedCourseId) {
        this.linkedCourseId = linkedCourseId;
    }
}
