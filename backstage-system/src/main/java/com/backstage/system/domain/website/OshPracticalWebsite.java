package com.backstage.system.domain.website;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 实用网站表
 * @TableName osh_practical_website
 */
@TableName(value ="osh_practical_website")
public class OshPracticalWebsite {
    /**
     * 主键 ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 资源编号，格式：前缀(2位) + 2位字母 + 2位数字 + 2位字母，如 wsAb34dF
     */
    private String no;

    /**
     * 网站名称
     */
    private String name;

    /**
     * 网站链接
     */
    private String url;

    /**
     * 网站描述
     */
    private String description;

    /**
     * 网站 Logo 地址
     */
    private String logoUrl;

    /**
     * 状态：2-待审核，4-已发布（审核通过），6-已拒绝
     */
    private Integer status;

    /**
     * 点击次数
     */
    private Integer clickCount;

    /**
     * 创建者
     */
    private String createBy;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新者
     */
    private String updateBy;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 备注
     */
    private String remark;

    /**
     * 删除标志：0-正常，1-删除
     */
    private Integer deleteFlag;

    /**
     * 审核人
     */
    private String auditBy;

    /**
     * 审核时间
     */
    private Date auditTime;

    /**
     * 拒绝原因
     */
    private String rejectReason;
    /** 好评数量 */
    private Integer goodCount;

    /** 中评数量 */
    private Integer midCount;

    /** 差评数量 */
    private Integer badCount;

    /** 收藏数量 */
    private Integer collectionCount;

    /** 评分 */
    private BigDecimal ratingScore;

    /** 等级/级别（数据库字段：level，tinyint类型） */
    private Integer level;

    /** 付费类型（数据库字段：payment_type，tinyint类型） */
    private Integer paymentType;

    /** 免费类型（数据库字段：free_type，tinyint类型） */
    private Integer freeType;

    /**
     * 主键 ID
     */
    public Long getId() {
        return id;
    }

    /**
     * 主键 ID
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * 资源编号
     */
    public String getNo() {
        return no;
    }

    /**
     * 资源编号
     */
    public void setNo(String no) {
        this.no = no;
    }

    /**
     * 网站名称
     */
    public String getName() {
        return name;
    }

    /**
     * 网站名称
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * 网站链接
     */
    public String getUrl() {
        return url;
    }

    /**
     * 网站链接
     */
    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * 网站描述
     */
    public String getDescription() {
        return description;
    }

    /**
     * 网站描述
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * 网站 Logo 地址
     */
    public String getLogoUrl() {
        return logoUrl;
    }

    /**
     * 网站 Logo 地址
     */
    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    /**
     * 状态：2-待审核，4-已发布（审核通过），6-已拒绝
     */
    public Integer getStatus() {
        return status;
    }

    /**
     * 状态：2-待审核，4-已发布（审核通过），6-已拒绝
     */
    public void setStatus(Integer status) {
        this.status = status;
    }

    /**
     * 点击次数
     */
    public Integer getClickCount() {
        return clickCount;
    }

    /**
     * 点击次数
     */
    public void setClickCount(Integer clickCount) {
        this.clickCount = clickCount;
    }

    /**
     * 创建者
     */
    public String getCreateBy() {
        return createBy;
    }

    /**
     * 创建者
     */
    public void setCreateBy(String createBy) {
        this.createBy = createBy;
    }

    /**
     * 创建时间
     */
    public Date getCreateTime() {
        return createTime;
    }

    /**
     * 创建时间
     */
    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    /**
     * 更新者
     */
    public String getUpdateBy() {
        return updateBy;
    }

    /**
     * 更新者
     */
    public void setUpdateBy(String updateBy) {
        this.updateBy = updateBy;
    }

    /**
     * 更新时间
     */
    public Date getUpdateTime() {
        return updateTime;
    }

    /**
     * 更新时间
     */
    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    /**
     * 备注
     */
    public String getRemark() {
        return remark;
    }

    /**
     * 备注
     */
    public void setRemark(String remark) {
        this.remark = remark;
    }

    /**
     * 删除标志：0-正常，1-删除
     */
    public Integer getDeleteFlag() {
        return deleteFlag;
    }

    /**
     * 删除标志：0-正常，1-删除
     */
    public void setDeleteFlag(Integer deleteFlag) {
        this.deleteFlag = deleteFlag;
    }

    /**
     * 审核人
     */
    public String getAuditBy() {
        return auditBy;
    }

    /**
     * 审核人
     */
    public void setAuditBy(String auditBy) {
        this.auditBy = auditBy;
    }

    /**
     * 审核时间
     */
    public Date getAuditTime() {
        return auditTime;
    }

    /**
     * 审核时间
     */
    public void setAuditTime(Date auditTime) {
        this.auditTime = auditTime;
    }

    /**
     * 拒绝原因
     */
    public String getRejectReason() {
        return rejectReason;
    }

    /**
     * 拒绝原因
     */
    public void setRejectReason(String rejectReason) {
        this.rejectReason = rejectReason;
    }

    /** 好评数量 */
    public Integer getGoodCount() {
        return goodCount;
    }


    public void setGoodCount(Integer goodCount) {
        this.goodCount = goodCount;
    }

    public Integer getMidCount() {
        return midCount;
    }

    public void setMidCount(Integer midCount) {
        this.midCount = midCount;
    }

    public Integer getBadCount() {
        return badCount;
    }

    public void setBadCount(Integer badCount) {
        this.badCount = badCount;
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
    public Integer getLevel() {
        return level;
    }

    public void setLevel(Integer level) {
        this.level = level;
    }

    public Integer getPaymentType() {
        return paymentType;
    }

    public void setPaymentType(Integer paymentType) {
        this.paymentType = paymentType;
    }

    public Integer getFreeType() {
        return freeType;
    }

    public void setFreeType(Integer freeType) {
        this.freeType = freeType;
    }

    @Override
    public String toString() {
        return "OshPracticalWebsite{" +
                "id=" + id +
                ", no='" + no + '\'' +
                ", name='" + name + '\'' +
                ", url='" + url + '\'' +
                ", description='" + description + '\'' +
                ", logoUrl='" + logoUrl + '\'' +
                ", status=" + status +
                ", clickCount=" + clickCount +
                ", createBy='" + createBy + '\'' +
                ", createTime=" + createTime +
                ", updateBy='" + updateBy + '\'' +
                ", updateTime=" + updateTime +
                ", remark='" + remark + '\'' +
                ", deleteFlag=" + deleteFlag +
                ", auditBy='" + auditBy + '\'' +
                ", auditTime=" + auditTime +
                ", rejectReason='" + rejectReason + '\'' +
                ", goodCount=" + goodCount +
                ", midCount=" + midCount +
                ", badCount=" + badCount +
                ", collectionCount=" + collectionCount +
                ", ratingScore=" + ratingScore +
                ", level=" + level +
                ", paymentType=" + paymentType +
                ", freeType=" + freeType +
                '}';
    }

}