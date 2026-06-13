package com.backstage.system.domain.openproject.dto;

import java.util.List;

public class OpenProjectQueryDTO {
    private String keyword;
    private List<Long> tagIds;
    private Long sourceId;
    private Integer pageNum = 1;
    private Integer pageSize = 10;

    /**
     * 排序字段：star_count / fork_count / last_commit_time / create_time（默认）
     */
    private String sortField = "create_time";

    /**
     * 排序方向：desc（默认）/ asc
     */
    private String sortOrder = "desc";

    /**
     * 是否只看我收藏的：true=只看收藏，false/null=全部
     */
    private Boolean onlyFavorite;

    public Boolean getOnlyFavorite() { return onlyFavorite; }
    public void setOnlyFavorite(Boolean onlyFavorite) { this.onlyFavorite = onlyFavorite; }

    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }

    public List<Long> getTagIds() { return tagIds; }
    public void setTagIds(List<Long> tagIds) { this.tagIds = tagIds; }

    public Long getSourceId() { return sourceId; }
    public void setSourceId(Long sourceId) { this.sourceId = sourceId; }

    public Integer getPageNum() { return pageNum; }
    public void setPageNum(Integer pageNum) { this.pageNum = pageNum; }

    public Integer getPageSize() { return pageSize; }
    public void setPageSize(Integer pageSize) { this.pageSize = pageSize; }

    public String getSortField() { return sortField; }
    public void setSortField(String sortField) { this.sortField = sortField; }

    public String getSortOrder() { return sortOrder; }
    public void setSortOrder(String sortOrder) { this.sortOrder = sortOrder; }
}
