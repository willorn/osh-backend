package com.backstage.system.domain.vo.tool;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.math.BigDecimal;

@ApiModel(description = "工具计算器结果")
public class ToolCalculatorResultVO {

    @ApiModelProperty(value = "计算结果", example = "125")
    private BigDecimal result;

    public BigDecimal getResult() {
        return result;
    }

    public void setResult(BigDecimal result) {
        this.result = result;
    }
}
