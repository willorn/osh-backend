package com.backstage.system.request;

import com.backstage.common.annotation.OshResourceId;

import javax.validation.constraints.NotNull;

/**
 * 课程收藏请求
 */
public class CourseCollectionRequest {

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
