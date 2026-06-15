package com.backstage.system.request;

import com.backstage.common.request.PageRequest;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

/**
 * @Author: Hope_Lau
 * @createTime: 2026年03月30日 21:56:42
 * @version:
 * @Description:
 */
public class CourseSearchRequest extends PageRequest {

    private List<String> tags;
    private String keyword;
    private String resourceType;
    /**
     * 课程难度：1-新手入门 2-基础巩固 3-能力提升；null 表示不限
     */
    private Integer difficulty;
    private Boolean isFollowing;
    private Integer collectionFlag;
    /**
     * Course id input from UI (resource number search).
     */
    private String courseNo;
    /**
     * Parsed course id filter:
     * null => no filter
     * -1  => invalid input, force empty result
     * >0  => exact id filter
     */
    private Long courseIdFilter;
    /**
     * Internal switch set by backend controller based on permissions.
     * true: include all statuses; false/null: only published.
     */
    private Boolean includeUnpublished;

    /**
     * 是否只查已隐藏课程（status=7）。仅创始人有效，由后端控制器赋值。
     * true: 只返回已隐藏课程；false/null: 正常列表（排除已隐藏）。
     */
    private Boolean onlyHidden;

    public CourseSearchRequest() {
    }

    public CourseSearchRequest(List<String> tags, String keyword) {
        this.tags = tags;
        this.keyword = keyword;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = StringUtils.trimToNull(keyword);
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = StringUtils.trimToNull(resourceType);
    }

    public Integer getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(Integer difficulty) {
        this.difficulty = difficulty;
    }

    public Boolean getIsFollowing() {
        return isFollowing;
    }

    public void setIsFollowing(Boolean isFollowing) {
        this.isFollowing = isFollowing;
    }

    public Integer getCollectionFlag() {
        return collectionFlag;
    }

    public void setCollectionFlag(Integer collectionFlag) {
        this.collectionFlag = collectionFlag;
    }

    public String getCourseNo() {
        return courseNo;
    }

    public void setCourseNo(String courseNo) {
        this.courseNo = StringUtils.trimToNull(courseNo);
        this.courseIdFilter = parseCourseIdFilter(this.courseNo);
    }

    public Long getCourseIdFilter() {
        return courseIdFilter;
    }

    public void setCourseIdFilter(Long courseIdFilter) {
        this.courseIdFilter = courseIdFilter;
    }

    public Boolean getIncludeUnpublished() {
        return includeUnpublished;
    }

    public void setIncludeUnpublished(Boolean includeUnpublished) {
        this.includeUnpublished = includeUnpublished;
    }

    public Boolean getOnlyHidden() {
        return onlyHidden;
    }

    public void setOnlyHidden(Boolean onlyHidden) {
        this.onlyHidden = onlyHidden;
    }

    private Long parseCourseIdFilter(String value) {
        if (value == null) {
            return null;
        }
        if (!StringUtils.isNumeric(value)) {
            return -1L;
        }
        try {
            return Long.valueOf(value);
        } catch (Exception ex) {
            return -1L;
        }
    }
}
