package com.backstage.system.request.tool;

import com.backstage.common.annotation.OshResourceId;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@ApiModel(description = "工具计算器请求")
public class ToolCalculatorRequest {

    @NotNull(message = "工具ID不能为空")
    @OshResourceId
    @ApiModelProperty(value = "工具ID", required = true, example = "10001")
    private Long toolId;

    @NotNull(message = "第一个数字不能为空")
    @ApiModelProperty(value = "第一个数字", required = true, example = "123")
    private BigDecimal leftValue;

    @NotNull(message = "第二个数字不能为空")
    @ApiModelProperty(value = "第二个数字", required = true, example = "2")
    private BigDecimal rightValue;

    @NotBlank(message = "运算符不能为空")
    @ApiModelProperty(value = "运算符，只支持 + - * /", required = true, example = "+")
    private String operator;

    public Long getToolId() {
        return toolId;
    }

    public void setToolId(Long toolId) {
        this.toolId = toolId;
    }

    public BigDecimal getLeftValue() {
        return leftValue;
    }

    public void setLeftValue(BigDecimal leftValue) {
        this.leftValue = leftValue;
    }

    public BigDecimal getRightValue() {
        return rightValue;
    }

    public void setRightValue(BigDecimal rightValue) {
        this.rightValue = rightValue;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }
}
