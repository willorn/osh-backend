package com.backstage.system.domain.vo.tool;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(description = "全局次数购买详情")
public class ToolPurchaseDetailVO {

    @ApiModelProperty(value = "当前用户剩余次数", example = "12")
    private Integer remainingCount;

    public Integer getRemainingCount() {
        return remainingCount;
    }

    public void setRemainingCount(Integer remainingCount) {
        this.remainingCount = remainingCount;
    }
}
