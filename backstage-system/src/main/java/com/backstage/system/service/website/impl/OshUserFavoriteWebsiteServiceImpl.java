package com.backstage.system.service.website.impl;

import com.backstage.common.constant.OshUserConstants;
import com.backstage.common.core.page.TableDataInfo;
import com.backstage.common.threadlocal.ThreadLocalUtil;
import com.backstage.system.domain.vo.website.UserFavoriteWebsiteVO;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.backstage.system.domain.website.OshUserFavoriteWebsite;
import com.backstage.system.mapper.website.OshPracticalWebsiteMapper;
import com.backstage.system.mapper.website.OshUserFavoriteWebsiteMapper;
import com.backstage.system.service.website.OshUserFavoriteWebsiteService;
import com.backstage.system.utils.UserContextUtil;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

/**
 * 用户收藏网站 Service 实现
 */
@Service
public class OshUserFavoriteWebsiteServiceImpl extends ServiceImpl<OshUserFavoriteWebsiteMapper, OshUserFavoriteWebsite>
        implements OshUserFavoriteWebsiteService {

    @Autowired
    private OshUserFavoriteWebsiteMapper userFavoriteWebsiteMapper;

    @Autowired
    private OshPracticalWebsiteMapper practicalWebsiteMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int favoriteWebsite(Long websiteId) {
        Long userId = UserContextUtil.getCurrentUserId();
        if (websiteId == null || userId == null) throw new IllegalArgumentException("网站 ID 和用户 ID 不能为空");
        // 检查是否已收藏（delete_flag=0 的有效记录）
        int count = userFavoriteWebsiteMapper.getFavorited(websiteId, userId);
        if (count > 0) {
            throw new IllegalArgumentException("您已经收藏过该网站了");
        }
        practicalWebsiteMapper.addCollectionCount(websiteId);
        // ON DUPLICATE KEY UPDATE delete_flag=0，软删除后再收藏不会报唯一键冲突
        return userFavoriteWebsiteMapper.favoriteWebsite(websiteId, userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int cancelFavoriteWebsite(Long websiteId) {
        Long userId = UserContextUtil.getCurrentUserId();
        // 1. 验证参数
        if (websiteId == null || userId == null) {
            throw new IllegalArgumentException("网站 ID 和用户 ID 不能为空");
        }
        int count = userFavoriteWebsiteMapper.getFavorited(websiteId, userId);
        if (count == 0) {
            throw new IllegalArgumentException("您没有收藏该网站");
        }
        practicalWebsiteMapper.reduceCollectionCount(websiteId);
        return userFavoriteWebsiteMapper.cancelFavoriteWebsite(websiteId, userId);
    }

    @Override
    public TableDataInfo selectUserFavoriteList(Integer pageNum, Integer pageSize) {
        // 优先从 USER_ID key 取，与 OshAuthenticationFilter 写入顺序一致
        // 避免 USER_INFO 查库失败时 NPE
        Long userId = UserContextUtil.getCurrentUserId();
        if (userId == null) {
            return new TableDataInfo(Collections.emptyList(), 0);
        }
        // 开启分页
        PageHelper.startPage(pageNum, pageSize);
        // 执行查询
        List<UserFavoriteWebsiteVO> list = userFavoriteWebsiteMapper.selectUserFavoriteList(userId);
        PageInfo<UserFavoriteWebsiteVO> pageInfo = new PageInfo<>(list);
        return new TableDataInfo(pageInfo.getList(), pageInfo.getTotal());
    }

}

