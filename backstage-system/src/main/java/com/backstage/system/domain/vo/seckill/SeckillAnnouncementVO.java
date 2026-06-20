package com.backstage.system.domain.vo.seckill;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.Date;

/**
 * 秒杀公告/动态 VO（从 osh_announcement 表查询返回）
 *
 * @author backstage
 * @date 2026-05-22
 */
@ApiModel(description = "秒杀公告/动态")
public class SeckillAnnouncementVO {

    @ApiModelProperty("公告ID")
    private Long id;

    @ApiModelProperty("标题内容（公告栏：商品秒杀文案；动态栏：用户购买文案）")
    private String title;

    @ApiModelProperty("跳转链接")
    private String link;

    @ApiModelProperty("图标 emoji")
    private String icon;

    @ApiModelProperty("栏目：1-系统动态（公告栏） 2-业务动态（动态栏）")
    private Integer channel;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty("创建时间")
    private Date createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getLink() { return link; }
    public void setLink(String link) { this.link = link; }

    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }

    public Integer getChannel() { return channel; }
    public void setChannel(Integer channel) { this.channel = channel; }

    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
}
