package com.backstage.system.domain.audit;

import com.backstage.common.annotation.OshResourceId;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@ApiModel(description = "资源审核请求")
public class ResourceAuditApproveRequest {

    @ApiModelProperty(value = "资源类型：course / qa_question / qa_answer / book / tool / website / open_project", required = true, example = "open_project")
    @NotBlank(message = "资源类型不能为空")
    private String resourceType;

    @ApiModelProperty(value = "资源ID", required = true, example = "10001")
    @NotNull(message = "资源ID不能为空")
    @OshResourceId
    private Long resourceId;

    @ApiModelProperty(value = "审核后资源状态：4-已发布，6-已下架", example = "4")
    private Integer status;

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public Long getResourceId() {
        return resourceId;
    }

    public void setResourceId(Long resourceId) {
        this.resourceId = resourceId;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
