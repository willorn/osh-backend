package com.backstage.system.domain.vo.tool;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(description = "当前用户工具点数")
public class ToolQuotaCurrentVO {

    @ApiModelProperty(value = "当前剩余工具点数", example = "10")
    private Integer remainingCount;

    @ApiModelProperty(value = "累计获得工具点数", example = "50")
    private Integer totalBuyCount;

    @ApiModelProperty(value = "累计消耗工具点数", example = "40")
    private Integer usedCount;

    public Integer getRemainingCount() {
        return remainingCount;
    }

    public void setRemainingCount(Integer remainingCount) {
        this.remainingCount = remainingCount;
    }

    public Integer getTotalBuyCount() {
        return totalBuyCount;
    }

    public void setTotalBuyCount(Integer totalBuyCount) {
        this.totalBuyCount = totalBuyCount;
    }

    public Integer getUsedCount() {
        return usedCount;
    }

    public void setUsedCount(Integer usedCount) {
        this.usedCount = usedCount;
    }
}
