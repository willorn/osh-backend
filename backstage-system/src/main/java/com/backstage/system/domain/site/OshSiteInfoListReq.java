package com.backstage.system.domain.site;

import java.util.List;

public class OshSiteInfoListReq extends OshSiteInfo {

    private Long pageNum;

    private Long pageSize;

    private List<String> resourceFilters;

    public List<String> getResourceFilters() {
        return resourceFilters;
    }

    public void setResourceFilters(List<String> resourceFilters) {
        this.resourceFilters = resourceFilters;
    }

    public Long getPageNum() {
        return pageNum;
    }

    public void setPageNum(Long pageNum) {
        this.pageNum = pageNum;
    }

    public Long getPageSize() {
        return pageSize;
    }

    public void setPageSize(Long pageSize) {
        this.pageSize = pageSize;
    }
}
