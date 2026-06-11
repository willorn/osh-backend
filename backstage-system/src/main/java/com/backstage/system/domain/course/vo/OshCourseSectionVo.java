package com.backstage.system.domain.course.vo;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class OshCourseSectionVo {

    private Long id;

    private Long courseId;

    private Long parentId;

    private String title;

    private Integer sort;

    private Integer freeFlag;

    private Integer duration;

    private String mediaUrl;

    private String textContent;

    private Long docId;

    private String anchorType;

    private String anchorStart;

    private String anchorEnd;

    private String excerptTitle;

    private String cover;

    private String videoCodec;

    private Integer videoBitrate;

    private String videoResolution;

    private Long fileSize;

    private String subtitleUrl;

    private String type;

    private Long linkedCourseId;

    private Integer status;

    private Long examId;

    private Integer deleteFlag;

    private String createBy;

    private Date createTime;

    private String updateBy;

    private Date updateTime;

    private List<OshCourseSectionVo> children = new ArrayList<>();

    public OshCourseSectionVo() {
    }

    public OshCourseSectionVo(Long id, Long courseId, Long parentId, String title, Integer sort, Integer freeFlag, Integer duration, String mediaUrl, String textContent, String cover, String videoCodec, Integer videoBitrate, String videoResolution, Long fileSize, String subtitleUrl, String type, Integer status, Long examId, Integer deleteFlag, String createBy, Date createTime, String updateBy, Date updateTime, List<OshCourseSectionVo> children) {
        this.id = id;
        this.courseId = courseId;
        this.parentId = parentId;
        this.title = title;
        this.sort = sort;
        this.freeFlag = freeFlag;
        this.duration = duration;
        this.mediaUrl = mediaUrl;
        this.textContent = textContent;
        this.cover = cover;
        this.videoCodec = videoCodec;
        this.videoBitrate = videoBitrate;
        this.videoResolution = videoResolution;
        this.fileSize = fileSize;
        this.subtitleUrl = subtitleUrl;
        this.type = type;
        this.status = status;
        this.examId = examId;
        this.deleteFlag = deleteFlag;
        this.createBy = createBy;
        this.createTime = createTime;
        this.updateBy = updateBy;
        this.updateTime = updateTime;
        this.children = children;
    }

    public String getTextContent() {
        return textContent;
    }

    public void setTextContent(String textContent) {
        this.textContent = textContent;
    }

    public Long getDocId() {
        return docId;
    }

    public void setDocId(Long docId) {
        this.docId = docId;
    }

    public String getAnchorType() {
        return anchorType;
    }

    public void setAnchorType(String anchorType) {
        this.anchorType = anchorType;
    }

    public String getAnchorStart() {
        return anchorStart;
    }

    public void setAnchorStart(String anchorStart) {
        this.anchorStart = anchorStart;
    }

    public String getAnchorEnd() {
        return anchorEnd;
    }

    public void setAnchorEnd(String anchorEnd) {
        this.anchorEnd = anchorEnd;
    }

    public String getExcerptTitle() {
        return excerptTitle;
    }

    public void setExcerptTitle(String excerptTitle) {
        this.excerptTitle = excerptTitle;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCourseId() {
        return courseId;
    }

    public void setCourseId(Long courseId) {
        this.courseId = courseId;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Integer getSort() {
        return sort;
    }

    public void setSort(Integer sort) {
        this.sort = sort;
    }

    public Integer getFreeFlag() {
        return freeFlag;
    }

    public void setFreeFlag(Integer freeFlag) {
        this.freeFlag = freeFlag;
    }

    public Integer getDuration() {
        return duration;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    public String getMediaUrl() {
        return mediaUrl;
    }

    public void setMediaUrl(String mediaUrl) {
        this.mediaUrl = mediaUrl;
    }

    public String getCover() {
        return cover;
    }

    public void setCover(String cover) {
        this.cover = cover;
    }

    public String getVideoCodec() {
        return videoCodec;
    }

    public void setVideoCodec(String videoCodec) {
        this.videoCodec = videoCodec;
    }

    public Integer getVideoBitrate() {
        return videoBitrate;
    }

    public void setVideoBitrate(Integer videoBitrate) {
        this.videoBitrate = videoBitrate;
    }

    public String getVideoResolution() {
        return videoResolution;
    }

    public void setVideoResolution(String videoResolution) {
        this.videoResolution = videoResolution;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getSubtitleUrl() {
        return subtitleUrl;
    }

    public void setSubtitleUrl(String subtitleUrl) {
        this.subtitleUrl = subtitleUrl;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Long getLinkedCourseId() {
        return linkedCourseId;
    }

    public void setLinkedCourseId(Long linkedCourseId) {
        this.linkedCourseId = linkedCourseId;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Long getExamId() {
        return examId;
    }

    public void setExamId(Long examId) {
        this.examId = examId;
    }

    public Integer getDeleteFlag() {
        return deleteFlag;
    }

    public void setDeleteFlag(Integer deleteFlag) {
        this.deleteFlag = deleteFlag;
    }

    public String getCreateBy() {
        return createBy;
    }

    public void setCreateBy(String createBy) {
        this.createBy = createBy;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public String getUpdateBy() {
        return updateBy;
    }

    public void setUpdateBy(String updateBy) {
        this.updateBy = updateBy;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public List<OshCourseSectionVo> getChildren() {
        return children;
    }

    public void setChildren(List<OshCourseSectionVo> children) {
        this.children = children;
    }

    @Override
    public String toString() {
        return "OshCourseSectionVo{" +
                "id=" + id +
                ", courseId=" + courseId +
                ", parentId=" + parentId +
                ", title='" + title + '\'' +
                ", sort=" + sort +
                ", freeFlag=" + freeFlag +
                ", duration=" + duration +
                ", mediaUrl='" + mediaUrl + '\'' +
                ", textContent='" + textContent + '\'' +
                ", cover='" + cover + '\'' +
                ", videoCodec='" + videoCodec + '\'' +
                ", videoBitrate=" + videoBitrate +
                ", videoResolution='" + videoResolution + '\'' +
                ", fileSize=" + fileSize +
                ", subtitleUrl='" + subtitleUrl + '\'' +
                ", type='" + type + '\'' +
                ", status=" + status +
                ", examId=" + examId +
                ", deleteFlag=" + deleteFlag +
                ", createBy='" + createBy + '\'' +
                ", createTime=" + createTime +
                ", updateBy='" + updateBy + '\'' +
                ", updateTime=" + updateTime +
                ", children=" + children +
                '}';
    }
}
