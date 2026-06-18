package com.backstage.system.request.tool;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.apache.commons.lang3.StringUtils;

import javax.validation.constraints.DecimalMin;
import java.math.BigDecimal;

/**
 * 工具点数套餐保存请求
 */
@ApiModel(description = "工具点数套餐保存请求")
public class ToolPackageSaveRequest {

    @ApiModelProperty(value = "套餐ID，新增不传，修改传入", example = "1")
    private Long id;

    @ApiModelProperty(value = "套餐名称，不传时按使用次数自动生成", example = "10次使用套餐")
    private String packageName;

    @ApiModelProperty(value = "购买后增加的使用次数", required = true, example = "10")
    private Integer useCount;

    @DecimalMin(value = "0.00", message = "现金价格不能小于0")
    @ApiModelProperty(value = "现金价格", example = "9.90")
    private BigDecimal price;

    @ApiModelProperty(value = "积分价格", example = "100")
    private Integer pointCost;

    @ApiModelProperty(value = "支付类型：1-现金，3-现金+积分", example = "1")
    private Integer payType;

    @ApiModelProperty(value = "状态：0-停用，1-启用", example = "1")
    private Integer status;

    @ApiModelProperty(value = "排序，值越大越靠前", example = "10")
    private Integer sortOrder;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = StringUtils.trimToNull(packageName);
    }

    public Integer getUseCount() {
        return useCount;
    }

    public void setUseCount(Integer useCount) {
        this.useCount = useCount;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Integer getPointCost() {
        return pointCost;
    }

    public void setPointCost(Integer pointCost) {
        this.pointCost = pointCost;
    }

    public Integer getPayType() {
        return payType;
    }

    public void setPayType(Integer payType) {
        this.payType = payType;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
}
