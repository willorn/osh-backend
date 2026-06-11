package com.backstage.system.domain.dto.website;

import com.backstage.common.annotation.OshResourceId;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import javax.validation.constraints.NotNull;

/**
 * @author xuanqing
 * @create 2026-04-11 20:59
 */
@ApiModel("网站评价提交DTO")
public class WebsiteRatingDTO {

    @ApiModelProperty(value = "网站ID", required = true)
    @NotNull(message = "网站ID不能为空")
    @OshResourceId
    private Long websiteId;

    @ApiModelProperty(value = "评价类型: 1-好评, 2-中评, 3-差评", required = true)
    @NotNull(message = "评价类型不能为空")
    private Integer ratingType;

    public Long getWebsiteId() {
        return websiteId;
    }

    public void setWebsiteId(Long websiteId) {
        this.websiteId = websiteId;
    }

    public Integer getRatingType() {
        return ratingType;
    }

    public void setRatingType(Integer ratingType) {
        this.ratingType = ratingType;
    }
    @Override
    public String toString() {
        return "WebsiteRating{" +
                "websiteId=" + websiteId +
                ", ratingType=" + ratingType +
                '}';
    }
}
