package com.backstage.system.request.tool;

import com.backstage.common.annotation.OshResourceId;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import javax.validation.constraints.NotNull;

/**
 * 工具点赞/差评请求
 */
@ApiModel(description = "工具点赞/差评请求")
public class ToolVoteRequest {

    @NotNull(message = "工具ID不能为空")
    @ApiModelProperty(value = "工具ID", required = true, example = "10001")
    @OshResourceId
    private Long toolId;

    public Long getToolId() {
        return toolId;
    }

    public void setToolId(Long toolId) {
        this.toolId = toolId;
    }
}
