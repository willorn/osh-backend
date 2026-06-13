package com.backstage.system.request;

import com.backstage.common.annotation.OshResourceId;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import javax.validation.constraints.NotNull;

@ApiModel("章节问题列表查询请求")
public class CourseSectionQuestionListRequest {

    @ApiModelProperty(value = "课程ID", required = true, example = "935")
    @NotNull(message = "课程ID不能为空")
    @OshResourceId
    private Long courseId;

    @ApiModelProperty(value = "章节ID", required = true, example = "2")
    @NotNull(message = "章节ID不能为空")
    private Long sectionId;

    public Long getCourseId() {
        return courseId;
    }

    public void setCourseId(Long courseId) {
        this.courseId = courseId;
    }

    public Long getSectionId() {
        return sectionId;
    }

    public void setSectionId(Long sectionId) {
        this.sectionId = sectionId;
    }
}
