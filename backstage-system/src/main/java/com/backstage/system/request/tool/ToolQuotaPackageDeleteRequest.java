package com.backstage.system.request.tool;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import javax.validation.constraints.NotNull;

@ApiModel(description = "工具点数套餐删除请求")
public class ToolQuotaPackageDeleteRequest {

    @NotNull(message = "套餐ID不能为空")
    @ApiModelProperty(value = "套餐ID", required = true, example = "1001")
    private Long packageId;

    public Long getPackageId() {
        return packageId;
    }

    public void setPackageId(Long packageId) {
        this.packageId = packageId;
    }
}
