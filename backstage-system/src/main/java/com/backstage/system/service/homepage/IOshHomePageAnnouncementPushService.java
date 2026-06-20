package com.backstage.system.service.homepage;

import com.backstage.system.domain.announcement.vo.AnnouncementMarqueeVO;

import java.util.List;

/**
 * 首页公告服务。
 */
public interface IOshHomePageAnnouncementPushService {

    /**
     * 系统通知，按资源类型分组，每组最近 5 条。
     */
    List<AnnouncementMarqueeVO> getSystemNotice();

    /**
     * 业务动态，按资源类型分组，每组最近 5 条。
     */
    List<AnnouncementMarqueeVO> getBusinessDynamic();

    /**
     * 给所有在线已登录用户广播首页刷新事件。
     */
    void pushAllModulesAnnouncementsData();

    /**
     * 给指定用户推送一次首页刷新事件。
     */
    void pushAnnouncementsToUser(Long userId);
}
