package com.backstage.system.domain.vo.website;

import java.io.Serializable;
import java.util.List;

/**
 * 实用网站批量导入结果 VO
 */
public class WebsiteImportResultVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 导入成功条数 */
    private int successCount;

    /** 导入失败条数 */
    private int failCount;

    /** 失败明细列表 */
    private List<FailDetail> failDetails;

    public WebsiteImportResultVO() {
    }

    public WebsiteImportResultVO(int successCount, int failCount, List<FailDetail> failDetails) {
        this.successCount = successCount;
        this.failCount = failCount;
        this.failDetails = failDetails;
    }

    public int getSuccessCount() {
        return successCount;
    }

    public void setSuccessCount(int successCount) {
        this.successCount = successCount;
    }

    public int getFailCount() {
        return failCount;
    }

    public void setFailCount(int failCount) {
        this.failCount = failCount;
    }

    public List<FailDetail> getFailDetails() {
        return failDetails;
    }

    public void setFailDetails(List<FailDetail> failDetails) {
        this.failDetails = failDetails;
    }

    /**
     * 单行失败详情
     */
    public static class FailDetail implements Serializable {

        private static final long serialVersionUID = 1L;

        /** Excel 行号（从 2 开始，含表头） */
        private int rowNum;

        /** 失败的网站名称（便于定位） */
        private String websiteName;

        /** 失败原因 */
        private String reason;

        public FailDetail(int rowNum, String websiteName, String reason) {
            this.rowNum = rowNum;
            this.websiteName = websiteName;
            this.reason = reason;
        }

        public int getRowNum() {
            return rowNum;
        }

        public void setRowNum(int rowNum) {
            this.rowNum = rowNum;
        }

        public String getWebsiteName() {
            return websiteName;
        }

        public void setWebsiteName(String websiteName) {
            this.websiteName = websiteName;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }

        @Override
        public String toString() {
            return "FailDetail{rowNum=" + rowNum + ", websiteName='" + websiteName + "', reason='" + reason + "'}";
        }
    }

    @Override
    public String toString() {
        return "WebsiteImportResultVO{successCount=" + successCount +
                ", failCount=" + failCount +
                ", failDetails=" + failDetails + '}';
    }
}
