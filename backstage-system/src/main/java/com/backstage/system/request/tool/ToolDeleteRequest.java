package com.backstage.system.request.tool;

import com.backstage.common.annotation.OshResourceId;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import javax.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 工具删除请求
 */
@ApiModel(description = "工具删除请求")
public class ToolDeleteRequest {

    @NotEmpty(message = "工具ID不能为空")
    @ApiModelProperty(value = "工具ID列表", required = true, example = "[10001,10002]")
    @OshResourceId
    private List<Long> ids;

    public List<Long> getIds() {
        return ids;
    }

    public void setIds(List<Long> ids) {
        this.ids = ids;
    }
}
