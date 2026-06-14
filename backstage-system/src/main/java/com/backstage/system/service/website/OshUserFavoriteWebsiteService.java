package com.backstage.system.service.website;

import com.backstage.common.core.page.TableDataInfo;
import com.baomidou.mybatisplus.extension.service.IService;
import com.backstage.system.domain.website.OshUserFavoriteWebsite;

/**
 * 用户收藏网站 Service 接口
 */
public interface OshUserFavoriteWebsiteService extends IService<OshUserFavoriteWebsite> {
    /**
     * 用户收藏网站
     * @param websiteId 网站 ID
     * @return 影响行数（1=成功，0=失败）
     */
    int favoriteWebsite(Long websiteId);

    /**
     * 取消收藏网站
     * @param websiteId 网站 ID
     * @return 影响行数（1=成功，0=失败）
     */
    int cancelFavoriteWebsite(Long websiteId);


    /**
     * 查询用户的收藏网站列表（分页）
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @return 分页结果
     */
    TableDataInfo selectUserFavoriteList(Integer pageNum, Integer pageSize);
}
