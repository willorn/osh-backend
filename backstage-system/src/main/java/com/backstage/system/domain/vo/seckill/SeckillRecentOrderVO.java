package com.backstage.system.domain.vo.seckill;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.Date;

/**
 * 秒杀最近成交记录 VO（用于首页滚动条展示）
 *
 * @author backstage
 * @date 2026-05-21
 */
@ApiModel(description = "秒杀最近成交记录")
public class SeckillRecentOrderVO {

    @ApiModelProperty("脱敏用户名（当前实现取登录名前2位+**）")
    private String username;

    @ApiModelProperty("商品标题")
    private String goodsTitle;

    @ApiModelProperty("商品类型：1-课程 2-书籍")
    private Integer goodsType;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty("下单时间")
    private Date createTime;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getGoodsTitle() { return goodsTitle; }
    public void setGoodsTitle(String goodsTitle) { this.goodsTitle = goodsTitle; }

    public Integer getGoodsType() { return goodsType; }
    public void setGoodsType(Integer goodsType) { this.goodsType = goodsType; }

    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
}
