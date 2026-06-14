package com.backstage.system.request;

import com.backstage.common.annotation.OshResourceId;
import org.apache.commons.lang3.StringUtils;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 课程问题回答请求
 */
public class CourseQuestionAnswerRequest {

    @NotNull(message = "问题ID不能为空")
    @OshResourceId
    private Long questionId;

    @NotBlank(message = "回答内容不能为空")
    private String content;

    public Long getQuestionId() {
        return questionId;
    }

    public void setQuestionId(Long questionId) {
        this.questionId = questionId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = StringUtils.trimToNull(content);
    }
}
