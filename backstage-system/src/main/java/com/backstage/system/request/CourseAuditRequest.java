package com.backstage.system.request;

import com.backstage.common.annotation.OshResourceId;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import javax.validation.constraints.NotNull;

@ApiModel("课程审核请求")
public class CourseAuditRequest {

    @ApiModelProperty(value = "课程ID", required = true, example = "100")
    @NotNull(message = "课程ID不能为空")
    @OshResourceId
    private Long courseId;

    public Long getCourseId() {
        return courseId;
    }

    public void setCourseId(Long courseId) {
        this.courseId = courseId;
    }
}
