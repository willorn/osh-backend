package com.backstage.system.domain.homepage.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.math.BigDecimal;

/**
 * 首页热门拼团 VO
 *
 * @author jayTatum
 */
@ApiModel(description = "首页热门拼团")
public class HotGroupVO {

    @ApiModelProperty("拼团活动ID")
    private Long groupId;

    @ApiModelProperty("商品ID")
    private Long goodsId;

    @ApiModelProperty("商品类型（course-课程）")
    private String type;

    @ApiModelProperty("商品标题（关联课程标题）")
    private String title;

    @ApiModelProperty("商品封面")
    private String cover;

    @ApiModelProperty("拼团价格")
    private BigDecimal groupPrice;

    @ApiModelProperty("原价")
    private BigDecimal originPrice;

    @ApiModelProperty("拼团人数要求（最低成团人数）")
    private Integer pNum;

    @ApiModelProperty("拼团人数上限")
    private Integer maxNum;

    @ApiModelProperty("拼团最低成团人数（group_min_num）")
    private Integer groupMinNum;

    @ApiModelProperty("拼团人数上限（group_max_num）")
    private Integer groupMaxNum;

    @ApiModelProperty("拼团开始时间")
    private String startTime;

    @ApiModelProperty("拼团结束时间")
    private String endTime;

    @ApiModelProperty("商品描述")
    private String description;

    @ApiModelProperty("当前参团人数")
    private Integer currentNum;

    @ApiModelProperty("热度分值")
    private java.math.BigDecimal hotScore;

    @ApiModelProperty("详情页跳转路径")
    private String detailUrl;

    // ========== getter / setter ==========

    public Long getGroupId() { return groupId; }
    public void setGroupId(Long groupId) { this.groupId = groupId; }

    public Long getGoodsId() { return goodsId; }
    public void setGoodsId(Long goodsId) { this.goodsId = goodsId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getCover() { return cover; }
    public void setCover(String cover) { this.cover = cover; }

    public BigDecimal getGroupPrice() { return groupPrice; }
    public void setGroupPrice(BigDecimal groupPrice) { this.groupPrice = groupPrice; }

    public BigDecimal getOriginPrice() { return originPrice; }
    public void setOriginPrice(BigDecimal originPrice) { this.originPrice = originPrice; }

    public Integer getpNum() { return pNum; }
    public void setpNum(Integer pNum) { this.pNum = pNum; }

    public Integer getMaxNum() { return maxNum; }
    public void setMaxNum(Integer maxNum) { this.maxNum = maxNum; }

    public Integer getGroupMinNum() { return groupMinNum; }
    public void setGroupMinNum(Integer groupMinNum) { this.groupMinNum = groupMinNum; }

    public Integer getGroupMaxNum() { return groupMaxNum; }
    public void setGroupMaxNum(Integer groupMaxNum) { this.groupMaxNum = groupMaxNum; }

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }

    public String getDetailUrl() { return detailUrl; }
    public void setDetailUrl(String detailUrl) { this.detailUrl = detailUrl; }

    public java.math.BigDecimal getHotScore() { return hotScore; }
    public void setHotScore(java.math.BigDecimal hotScore) { this.hotScore = hotScore; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Integer getCurrentNum() { return currentNum; }
    public void setCurrentNum(Integer currentNum) { this.currentNum = currentNum; }
}
