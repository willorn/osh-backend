package com.backstage.system.request.tool;

import com.backstage.common.annotation.OshResourceId;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import javax.validation.constraints.NotNull;

/**
 * 工具使用次数扣减请求
 */
@ApiModel(description = "工具使用次数扣减请求")
public class ToolUsageConsumeRequest {

    @NotNull(message = "工具ID不能为空")
    @ApiModelProperty(value = "工具ID", required = true, example = "3")
    @OshResourceId
    private Long toolId;

    @ApiModelProperty(value = "本次使用唯一标识，预留用于幂等扣减", example = "3-1710000000000")
    private String usageKey;

    public Long getToolId() {
        return toolId;
    }

    public void setToolId(Long toolId) {
        this.toolId = toolId;
    }

    public String getUsageKey() {
        return usageKey;
    }

    public void setUsageKey(String usageKey) {
        this.usageKey = usageKey;
    }
}
