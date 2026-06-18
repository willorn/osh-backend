package com.backstage.system.service.website;

import com.baomidou.mybatisplus.extension.service.IService;
import com.backstage.system.domain.website.OshWebsiteUserRating;

/**
 * 网站用户评价 Service 接口
 */
public interface OshWebsiteUserRatingService extends IService<OshWebsiteUserRating> {

    void submitRating(Long userId, Long websiteId, Integer ratingType);
}
