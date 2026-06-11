package com.backstage.system.request;

import com.backstage.common.annotation.OshResourceId;

import javax.validation.constraints.NotNull;

public class CourseSectionDeleteRequest {

    @NotNull(message = "课程id不能为空")
    @OshResourceId
    private Long courseId;

    @NotNull(message = "章节id不能为空")
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
