package com.backstage.system.domain.assistant.dto;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Size;
import java.util.List;

/**
 * AI 助手反馈分页查询 DTO
 *
 * @author backstage
 */
public class AssistantFeedbackPageDTO {

    /**
     * 分类 ID（可选）
     */
    private Long categoryId;

    /**
     * 分类代码（可选）
     */
    private String categoryCode;

    /**
     * 状态（可选）
     */
    private String status;

    /**
     * 是否置顶（可选，0-否 1-是）
     */
    private Integer isPinned;

    /**
     * 关键词搜索（标题或内容），最大100字符
     */
    @Size(max = 100, message = "关键词最大长度为100")
    private String keyword;

    /**
     * 标签 ID 列表
     */
    private List<Long> tagIds;

    /**
     * 用户 ID（查询指定用户的反馈）
     */
    private Long userId;

    /**
     * 查询模式（all-全部 mine-我的 favorite-我的收藏）
     */
    private String queryMode;

    /**
     * 排序类型（可选：hot-最热，latest-最新，related-最相关，comment-最多评论）
     */
    private String sortType;

    /**
     * 页码（默认 1，最小1）
     */
    @Min(value = 1, message = "页码最小为1")
    private Integer pageNum = 1;

    /**
     * 每页数量（默认 10，范围1-100）
     */
    @Min(value = 1, message = "每页数量最小为1")
    @Max(value = 100, message = "每页数量最大为100")
    private Integer pageSize = 10;

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryCode() {
        return categoryCode;
    }

    public void setCategoryCode(String categoryCode) {
        this.categoryCode = categoryCode;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getIsPinned() {
        return isPinned;
    }

    public void setIsPinned(Integer isPinned) {
        this.isPinned = isPinned;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public List<Long> getTagIds() {
        return tagIds;
    }

    public void setTagIds(List<Long> tagIds) {
        this.tagIds = tagIds;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getQueryMode() {
        return queryMode;
    }

    public void setQueryMode(String queryMode) {
        this.queryMode = queryMode;
    }

    public String getSortType() {
        return sortType;
    }

    public void setSortType(String sortType) {
        this.sortType = sortType;
    }

    public Integer getPageNum() {
        return pageNum;
    }

    public void setPageNum(Integer pageNum) {
        this.pageNum = pageNum;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }
}
