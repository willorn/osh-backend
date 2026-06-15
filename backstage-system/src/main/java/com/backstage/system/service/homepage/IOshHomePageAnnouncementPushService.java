package com.backstage.system.service.homepage;

import com.backstage.system.domain.announcement.vo.AnnouncementMarqueeVO;

import java.util.List;
import java.util.Map;

/**
 * 首页公告服务。
 */
public interface IOshHomePageAnnouncementPushService {

    /**
     * 查询首页系统通知。
     *
     * @param limit 返回条数
     * @return 系统通知列表
     */
    Map<String, List<AnnouncementMarqueeVO>> getSystemNotice();

    /**
     * 查询首页业务动态。
     *
     * @param limit 返回条数
     * @return 业务动态列表
     */
    Map<String, List<AnnouncementMarqueeVO>> getBusinessDynamic();

    /**
     * 每小时广播首页公告刷新事件。
     */
    void pushAllModulesAnnouncementsData();

    /**
     * 用户 WebSocket 建连后推送一次首页公告刷新事件。
     *
     * @param userId 用户ID
     */
    void pushAnnouncementsToUser(Long userId);
}
