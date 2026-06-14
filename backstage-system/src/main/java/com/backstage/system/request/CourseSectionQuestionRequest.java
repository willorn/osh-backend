package com.backstage.system.request;

import com.backstage.common.annotation.OshResourceId;
import org.apache.commons.lang3.StringUtils;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * @Author: hope
 * @createTime: 2026年04月01日 23:08:23
 * @version:
 * @Description:
 */
public class CourseSectionQuestionRequest {

    @NotNull(message = "课程ID不能为空")
    @OshResourceId
    private Long courseId;
    @NotNull(message = "课程小节ID不能为空")
    private Long sectionId;
    @NotBlank(message = "问题标题不能为空")
    private String title;

    @NotBlank(message = "问题内容不能为空")
    private String content;

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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = StringUtils.trimToNull(title);
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = StringUtils.trimToNull(content);
    }
}
