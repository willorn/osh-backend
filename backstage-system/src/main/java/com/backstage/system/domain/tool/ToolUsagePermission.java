package com.backstage.system.domain.tool;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 工具使用权限校验结果
 */
@ApiModel(description = "工具使用权限校验结果")
public class ToolUsagePermission {

    @ApiModelProperty(value = "是否允许使用工具", example = "true")
    private Boolean useAllowed;

    @ApiModelProperty(value = "是否允许扣减次数", example = "false")
    private Boolean deductAllowed;

    @ApiModelProperty(value = "当前用户剩余使用次数", example = "0")
    private Integer remainingCount;

    @ApiModelProperty(value = "校验提示信息", example = "工具点数不足")
    private String message;

    public Boolean getUseAllowed() {
        return useAllowed;
    }

    public void setUseAllowed(Boolean useAllowed) {
        this.useAllowed = useAllowed;
    }

    public Boolean getDeductAllowed() {
        return deductAllowed;
    }

    public void setDeductAllowed(Boolean deductAllowed) {
        this.deductAllowed = deductAllowed;
    }

    public Integer getRemainingCount() {
        return remainingCount;
    }

    public void setRemainingCount(Integer remainingCount) {
        this.remainingCount = remainingCount;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
