package com.backstage.system.domain.course;

import com.backstage.common.annotation.Excel;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 课程信息对象 osh_course
 *
 * @author ruoyi
 * @date 2026-01-XX
 */
@ApiModel(description = "课程信息")
public class OshCourse {
    private static final long serialVersionUID = 1L;

    @ApiModelProperty("课程 ID")
    private Long id;

    @ApiModelProperty("资源编号")
    private String no;

    @Excel(name = "课程标题")
    @ApiModelProperty("课程标题")
    private String title;

    @Excel(name = "课程封面图 URL")
    @ApiModelProperty("课程封面图 URL")
    private String cover;

    @Excel(name = "课程介绍")
    @ApiModelProperty("课程介绍/试看内容")
    private String intro;

    @Excel(name = "课程服务内容")
    @ApiModelProperty("课程服务内容")
    private String serviceContent;

    @Excel(name = "当前价格")
    @ApiModelProperty("当前价格")
    private BigDecimal price;

    @Excel(name = "原价")
    @ApiModelProperty("原价/市场价")
    @JsonProperty("tPrice")
    private BigDecimal tPrice;

    @Excel(name = "课程类型")
    @ApiModelProperty("课程类型：media-视频课，live-直播课，text-图文课")
    private String type;

    @Excel(name = "章节数量")
    @ApiModelProperty("章节数量")
    private Integer subCount;

    @Excel(name = "备注")
    @ApiModelProperty("备注")
    private String remark;

    @Excel(name = "总时长(分钟)")
    @ApiModelProperty("总时长 (分钟)")
    private Integer totalDuration;

    @Excel(name = "视频总数")
    @ApiModelProperty("视频总数")
    private Integer videoCount;

    @Excel(name = "销量")
    @ApiModelProperty("销量")
    private Integer salesCount;

    @Excel(name = "浏览数")
    @ApiModelProperty("浏览数")
    private Long viewCount;

    @Excel(name = "免费视频数")
    @ApiModelProperty("免费视频数")
    private Integer freeLessonCount;

    @Excel(name = "点赞数")
    @ApiModelProperty("点赞数")
    private Integer likeCount;

    @Excel(name = "评论数")
    @ApiModelProperty("评论数")
    private Integer commentCount;

    @Excel(name = "提问数")
    @ApiModelProperty("提问数")
    private Integer questionCount;

    @Excel(name = "收藏数")
    @ApiModelProperty("收藏数")
    private Integer collectionCount;


    @Excel(name = "平均评分")
    @ApiModelProperty("平均评分")
    private BigDecimal ratingScore;

    @Excel(name = "免费类型")
    @ApiModelProperty("免费类型：0-完全免费 1-部分免费 2-限时免费 3-不免费")
    private Integer freeType;

    @Excel(name = "售后答疑天数")
    @ApiModelProperty("售后答疑天数")
    private Integer afterServiceDays;

    @Excel(name = "状态")
    @ApiModelProperty("状态：0-草稿 1-待审核 2-已发布 3-已下架")
    private Integer status;

    @ApiModelProperty("删除标记")
    private Integer deleteFlag;

    @ApiModelProperty("搜索值")
    private String searchValue;

    @Excel(name = "创建人")
    @ApiModelProperty("创建人")
    private String createBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty("创建时间")
    private Date createTime;

    @Excel(name = "修改人")
    @ApiModelProperty("修改人")
    private String updateBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty("更新时间")
    private Date updateTime;

    @Excel(name = "考试ID")
    @ApiModelProperty("考试ID")
    private Integer examId;

    @ApiModelProperty("资源类型")
    private String resourceType;

    @ApiModelProperty("服务周期（月）")
    private Integer servicePeriod;

    @ApiModelProperty("课程等级")
    private Integer level;

    @ApiModelProperty("试看内容/试用内容")
    private String tryContent;

    @ApiModelProperty("标签ID列表（数组）")
    private Long[] tagIds;




    public OshCourse() {
    }

    public Integer getFreeLessonCount() {
        return freeLessonCount;
    }



    public void setFreeLessonCount(Integer freeLessonCount) {
        this.freeLessonCount = freeLessonCount;
    }

    public Integer getExamId() {
        return examId;
    }

    public void setExamId(Integer examId) {
        this.examId = examId;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public Integer getServicePeriod() {
        return servicePeriod;
    }

    public void setServicePeriod(Integer servicePeriod) {
        this.servicePeriod = servicePeriod;
    }

    public Integer getLevel() {
        return level;
    }

    public void setLevel(Integer level) {
        this.level = level;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNo() {
        return no;
    }

    public void setNo(String no) {
        this.no = no;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCover() {
        return cover;
    }

    public void setCover(String cover) {
        this.cover = cover;
    }



    public String getServiceContent() {
        return serviceContent;
    }

    public void setServiceContent(String serviceContent) {
        this.serviceContent = serviceContent;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getTPrice() {
        return tPrice;
    }

    public void setTPrice(BigDecimal tPrice) {
        this.tPrice = tPrice;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Integer getSubCount() {
        return subCount;
    }

    public void setSubCount(Integer subCount) {
        this.subCount = subCount;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public Integer getTotalDuration() {
        return totalDuration;
    }

    public void setTotalDuration(Integer totalDuration) {
        this.totalDuration = totalDuration;
    }

    public Integer getVideoCount() {
        return videoCount;
    }

    public void setVideoCount(Integer videoCount) {
        this.videoCount = videoCount;
    }

    public Integer getSalesCount() {
        return salesCount;
    }

    public void setSalesCount(Integer salesCount) {
        this.salesCount = salesCount;
    }

    public Long getViewCount() {
        return viewCount;
    }

    public void setViewCount(Long viewCount) {
        this.viewCount = viewCount;
    }

    public Integer getLikeCount() {
        return likeCount;
    }

    public void setLikeCount(Integer likeCount) {
        this.likeCount = likeCount;
    }

    public Integer getCommentCount() {
        return commentCount;
    }

    public void setCommentCount(Integer commentCount) {
        this.commentCount = commentCount;
    }

    public Integer getQuestionCount() {
        return questionCount;
    }

    public void setQuestionCount(Integer questionCount) {
        this.questionCount = questionCount;
    }

    public Integer getCollectionCount() {
        return collectionCount;
    }

    public void setCollectionCount(Integer collectionCount) {
        this.collectionCount = collectionCount;
    }

    public BigDecimal getRatingScore() {
        return ratingScore;
    }

    public void setRatingScore(BigDecimal ratingScore) {
        this.ratingScore = ratingScore;
    }

    public Integer getFreeType() {
        return freeType;
    }

    public void setFreeType(Integer freeType) {
        this.freeType = freeType;
    }

    public Integer getAfterServiceDays() {
        return afterServiceDays;
    }

    public void setAfterServiceDays(Integer afterServiceDays) {
        this.afterServiceDays = afterServiceDays;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Integer getDeleteFlag() {
        return deleteFlag;
    }

    public void setDeleteFlag(Integer deleteFlag) {
        this.deleteFlag = deleteFlag;
    }

    public String getSearchValue() {
        return searchValue;
    }

    public void setSearchValue(String searchValue) {
        this.searchValue = searchValue;
    }

    public String getCreateBy() {
        return createBy;
    }

    public void setCreateBy(String createBy) {
        this.createBy = createBy;
    }

    public String getUpdateBy() {
        return updateBy;
    }

    public void setUpdateBy(String updateBy) {
        this.updateBy = updateBy;
    }

    public String getIntro() {
        return intro;
    }

    public void setIntro(String intro) {
        this.intro = intro;
    }

    public String getTryContent() {
        return tryContent;
    }

    public void setTryContent(String tryContent) {
        this.tryContent = tryContent;
    }

    public Long[] getTagIds() {
        return tagIds;
    }

    public void setTagIds(Long[] tagIds) {
        this.tagIds = tagIds;
    }

    public BigDecimal gettPrice() {
        return tPrice;
    }

    public void settPrice(BigDecimal tPrice) {
        this.tPrice = tPrice;
    }

    @Override
    public String toString() {
        return "OshCourse{" +
                "id=" + id +
                ", no='" + no + '\'' +
                ", title='" + title + '\'' +
                ", cover='" + cover + '\'' +
                ", intro='" + intro + '\'' +
                ", serviceContent='" + serviceContent + '\'' +
                ", price=" + price +
                ", tPrice=" + tPrice +
                ", type='" + type + '\'' +
                ", subCount=" + subCount +
                ", remark='" + remark + '\'' +
                ", totalDuration=" + totalDuration +
                ", videoCount=" + videoCount +
                ", salesCount=" + salesCount +
                ", viewCount=" + viewCount +
                ", freeLessonCount=" + freeLessonCount +
                ", likeCount=" + likeCount +
                ", commentCount=" + commentCount +
                ", questionCount=" + questionCount +
                ", collectionCount=" + collectionCount +
                ", ratingScore=" + ratingScore +
                ", freeType=" + freeType +
                ", afterServiceDays=" + afterServiceDays +
                ", status=" + status +
                ", deleteFlag=" + deleteFlag +
                ", searchValue='" + searchValue + '\'' +
                ", createBy='" + createBy + '\'' +
                ", createTime=" + createTime +
                ", updateBy='" + updateBy + '\'' +
                ", updateTime=" + updateTime +
                ", examId=" + examId +
                '}';
    }

    public OshCourse(Long id, String title, String cover, String intro, String serviceContent, BigDecimal price, BigDecimal tPrice, String type, Integer subCount, String remark, Integer totalDuration, Integer videoCount, Integer salesCount, Long viewCount, Integer freeLessonCount, Integer likeCount, Integer commentCount, BigDecimal ratingScore, Integer freeType, Integer afterServiceDays, Integer status, String searchValue, String createBy, Date createTime, String updateBy, Date updateTime, Integer examId) {
        this.id = id;
        this.title = title;
        this.cover = cover;
        this.intro = intro;
        this.serviceContent = serviceContent;
        this.price = price;
        this.tPrice = tPrice;
        this.type = type;
        this.subCount = subCount;
        this.remark = remark;
        this.totalDuration = totalDuration;
        this.videoCount = videoCount;
        this.salesCount = salesCount;
        this.viewCount = viewCount;
        this.freeLessonCount = freeLessonCount;
        this.likeCount = likeCount;
        this.commentCount = commentCount;
        this.ratingScore = ratingScore;
        this.freeType = freeType;
        this.afterServiceDays = afterServiceDays;
        this.status = status;
        this.searchValue = searchValue;
        this.createBy = createBy;
        this.createTime = createTime;
        this.updateBy = updateBy;
        this.updateTime = updateTime;
        this.examId = examId;
    }
}
