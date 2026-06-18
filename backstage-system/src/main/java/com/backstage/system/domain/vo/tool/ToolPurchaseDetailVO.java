package com.backstage.system.domain.vo.tool;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(description = "工具点数购买详情")
public class ToolPurchaseDetailVO {

    @ApiModelProperty(value = "当前用户剩余次数", example = "12")
    private Integer remainingCount;

    @ApiModelProperty(value = "当前用户剩余积分", example = "1280")
    private Long remainingPoints;

    public Integer getRemainingCount() {
        return remainingCount;
    }

    public void setRemainingCount(Integer remainingCount) {
        this.remainingCount = remainingCount;
    }

    public Long getRemainingPoints() {
        return remainingPoints;
    }

    public void setRemainingPoints(Long remainingPoints) {
        this.remainingPoints = remainingPoints;
    }
}
