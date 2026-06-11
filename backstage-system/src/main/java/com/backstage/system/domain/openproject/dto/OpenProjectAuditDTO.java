package com.backstage.system.domain.openproject.dto;

import com.backstage.common.annotation.OshResourceId;

public class OpenProjectAuditDTO {

    /** 项目 ID */
    @OshResourceId
    private Long id;

    /** 审核结果：1-通过，2-拒绝 */
    private Integer status;

    /** 拒绝原因（status=2 时必填） */
    private String rejectReason;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public String getRejectReason() { return rejectReason; }
    public void setRejectReason(String rejectReason) { this.rejectReason = rejectReason; }
}
