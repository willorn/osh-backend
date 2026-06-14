package com.backstage.system.domain.info_gap;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 信息差主表
 */
@TableName("osh_info_gap")
public class OshInfoGap {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String no;
    private Long userId;
    private String userName;
    private String title;
    private String tag;
    private String content;
    private Integer goodCount;
    private Integer middleCount;
    private Integer badCount;
    private Integer collectCount;
    private Integer viewCount;
    private Integer status; // 0-草稿, 2-待审核, 4-已发布, 6-已下架
    private Long updateBy;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleteFlag;

    // Getter and Setter
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNo() { return no; }
    public void setNo(String no) { this.no = no; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getTag() { return tag; }
    public void setTag(String tag) { this.tag = tag; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Integer getGoodCount() { return goodCount; }
    public void setGoodCount(Integer goodCount) { this.goodCount = goodCount; }
    public Integer getMiddleCount() { return middleCount; }
    public void setMiddleCount(Integer middleCount) { this.middleCount = middleCount; }
    public Integer getBadCount() { return badCount; }
    public void setBadCount(Integer badCount) { this.badCount = badCount; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }

    public Integer getCollectCount() {
        return collectCount;
    }

    public void setCollectCount(Integer collectCount) {
        this.collectCount = collectCount;
    }

    public Integer getViewCount() {
        return viewCount;
    }

    public void setViewCount(Integer viewCount) {
        this.viewCount = viewCount;
    }

    public Integer getDeleteFlag() {
        return deleteFlag;
    }

    public void setDeleteFlag(Integer deleteFlag) {
        this.deleteFlag = deleteFlag;
    }

    public Long getUpdateBy() {
        return updateBy;
    }

    public void setUpdateBy(Long updateBy) {
        this.updateBy = updateBy;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }
}
