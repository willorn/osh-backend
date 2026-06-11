package com.backstage.system.domain.dto.website;

import com.backstage.common.annotation.OshResourceId;

/**
 * @author xuanqing
 * @create 2026-04-02 18:41
 */

import java.io.Serializable;

/**
 * 网站审核 DTO
 */
public class WebsiteAuditDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 网站 ID
     */
    @OshResourceId
    private Long websiteId;

    /**
     * 审核结果：1-通过，2-拒绝
     */
    private Integer status;

    /**
     * 拒绝原因（仅拒绝时必填）
     */
    private String rejectReason;

    // Getter & Setter
    public Long getWebsiteId() {
        return websiteId;
    }

    public void setWebsiteId(Long websiteId) {
        this.websiteId = websiteId;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getRejectReason() {
        return rejectReason;
    }

    public void setRejectReason(String rejectReason) {
        this.rejectReason = rejectReason;
    }

    // toString
    @Override
    public String toString() {
        return "WebsiteAuditDTO{" +
                "websiteId=" + websiteId +
                ", status=" + status +
                ", rejectReason='" + rejectReason + '\'' +
                '}';
    }
}
