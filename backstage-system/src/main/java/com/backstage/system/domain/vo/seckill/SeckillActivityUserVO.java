package com.backstage.system.domain.vo.seckill;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 用户端秒杀活动响应 VO
 * 只返回用户需要看到的字段，不含管理字段
 *
 * @author backstage
 * @date 2026-04-28
 */
@ApiModel(description = "用户端秒杀活动响应")
public class SeckillActivityUserVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("活动ID")
    private Long id;

    @ApiModelProperty("活动标题")
    private String title;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty("活动开始时间")
    private Date startTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty("活动结束时间")
    private Date endTime;

    @ApiModelProperty("活动状态：1-未开始 2-进行中 3-已结束")
    private Integer status;

    @ApiModelProperty("支付超时时间（分钟），告知用户抢到后需在多少分钟内付款")
    private Integer payTimeoutMin;

    @ApiModelProperty("活动商品明细列表（用户端只展示库存、价格等必要字段）")
    private List<SeckillActivityItemVO> items;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public Date getStartTime() { return startTime; }
    public void setStartTime(Date startTime) { this.startTime = startTime; }

    public Date getEndTime() { return endTime; }
    public void setEndTime(Date endTime) { this.endTime = endTime; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public Integer getPayTimeoutMin() { return payTimeoutMin; }
    public void setPayTimeoutMin(Integer payTimeoutMin) { this.payTimeoutMin = payTimeoutMin; }

    public List<SeckillActivityItemVO> getItems() { return items; }
    public void setItems(List<SeckillActivityItemVO> items) { this.items = items; }
}
