package com.backstage.system.request;

import com.backstage.common.annotation.OshResourceId;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.apache.commons.lang3.StringUtils;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.PositiveOrZero;

@ApiModel("视频小节创建请求")
public class CourseVideoSectionCreateRequest {

    @ApiModelProperty(value = "课程ID", required = true, example = "100")
    @NotNull(message = "课程ID不能为空")
    @OshResourceId
    private Long courseId;

    @ApiModelProperty(value = "父章节ID，必须是一级章节ID", required = true, example = "10")
    @NotNull(message = "父章节ID不能为空")
    private Long parentId;

    @ApiModelProperty(value = "小节标题", required = true, example = "第一节")
    @NotBlank(message = "小节标题不能为空")
    private String title;

    @ApiModelProperty(value = "排序值，越小越靠前", required = true, example = "1")
    @NotNull(message = "排序不能为空")
    @PositiveOrZero(message = "排序不能小于0")
    private Integer sort;

    @ApiModelProperty(value = "是否免费：0-否，1-是；不传默认0", example = "0")
    @PositiveOrZero(message = "是否免费标记不能小于0")
    private Integer freeFlag;

    @ApiModelProperty(value = "视频时长，单位秒", example = "600")
    @PositiveOrZero(message = "时长不能小于0")
    private Integer duration;

    @ApiModelProperty(value = "视频OSS地址", required = true, example = "https://oss.example.com/video.mp4")
    private String mediaUrl;

    @ApiModelProperty(value = "封面图地址", example = "https://oss.example.com/cover.png")
    private String cover;

    @ApiModelProperty(value = "视频描述", example = "1080P 视频")
    private String videoDesc;

    @ApiModelProperty(value = "视频配套文本内容", example = "这里是视频对应的补充说明")
    private String textContent;

    @ApiModelProperty(value = "文件大小，单位字节", required = true, example = "123456")
    @NotNull(message = "文件大小不能为空")
    @PositiveOrZero(message = "文件大小不能小于0")
    private Long fileSize;

    @ApiModelProperty(value = "文档模式：CREATE(新建文档) / BIND_EXISTING(绑定已有文档)", example = "CREATE")
    private String docMode;

    @ApiModelProperty(value = "已有文档ID（docMode=BIND_EXISTING 时可传）", example = "189223901234567890")
    private Long docId;

    @ApiModelProperty(value = "文档标题（可选，不传默认使用小节标题）", example = "第一节讲义")
    private String docTitle;

    @ApiModelProperty(value = "片段类型：full(整篇) / range(片段)", example = "full")
    private String anchorType;

    @ApiModelProperty(value = "片段起始锚点（range 时建议填写）", example = "回调签名校验")
    private String anchorStart;

    @ApiModelProperty(value = "片段结束锚点（range 时建议填写）", example = "签名算法说明")
    private String anchorEnd;

    @ApiModelProperty(value = "片段摘要标题", example = "签名校验片段")
    private String excerptTitle;

    @ApiModelProperty(value = "小节ID，有值则更新，无值则新增")
    private Long id;

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
        this.title = StringUtils.trimToNull(title);
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
        this.mediaUrl = StringUtils.trimToNull(mediaUrl);
    }

    public String getCover() {
        return cover;
    }

    public void setCover(String cover) {
        this.cover = StringUtils.trimToNull(cover);
    }

    public String getVideoDesc() {
        return videoDesc;
    }

    public void setVideoDesc(String videoDesc) {
        this.videoDesc = StringUtils.trimToNull(videoDesc);
    }

    public String getTextContent() {
        return textContent;
    }

    public void setTextContent(String textContent) {
        this.textContent = StringUtils.trimToNull(textContent);
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getDocMode() {
        return docMode;
    }

    public void setDocMode(String docMode) {
        this.docMode = StringUtils.trimToNull(docMode);
    }

    public Long getDocId() {
        return docId;
    }

    public void setDocId(Long docId) {
        this.docId = docId;
    }

    public String getDocTitle() {
        return docTitle;
    }

    public void setDocTitle(String docTitle) {
        this.docTitle = StringUtils.trimToNull(docTitle);
    }

    public String getAnchorType() {
        return anchorType;
    }

    public void setAnchorType(String anchorType) {
        this.anchorType = StringUtils.trimToNull(anchorType);
    }

    public String getAnchorStart() {
        return anchorStart;
    }

    public void setAnchorStart(String anchorStart) {
        this.anchorStart = StringUtils.trimToNull(anchorStart);
    }

    public String getAnchorEnd() {
        return anchorEnd;
    }

    public void setAnchorEnd(String anchorEnd) {
        this.anchorEnd = StringUtils.trimToNull(anchorEnd);
    }

    public String getExcerptTitle() {
        return excerptTitle;
    }

    public void setExcerptTitle(String excerptTitle) {
        this.excerptTitle = StringUtils.trimToNull(excerptTitle);
    }

}
