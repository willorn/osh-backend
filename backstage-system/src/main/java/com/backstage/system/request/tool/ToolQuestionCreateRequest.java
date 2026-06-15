package com.backstage.system.request.tool;

import com.backstage.common.annotation.OshResourceId;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;

@ApiModel(description = "工具提问请求")
public class ToolQuestionCreateRequest {

    @NotNull(message = "工具ID不能为空")
    @OshResourceId
    @ApiModelProperty(value = "工具ID", required = true, example = "10001")
    private Long toolId;

    @NotBlank(message = "问题内容不能为空")
    @ApiModelProperty(value = "问题内容", required = true, example = "这个工具为什么计算结果不对？")
    private String content;

    @ApiModelProperty(value = "标签名称列表", example = "[\"计算器\",\"工具使用\"]")
    private List<String> tags;

    public Long getToolId() {
        return toolId;
    }

    public void setToolId(Long toolId) {
        this.toolId = toolId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }
}
