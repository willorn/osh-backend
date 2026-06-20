package com.backstage.system.service.impl.homepage;

import com.backstage.common.enums.AnnouncementChannelEnum;
import com.backstage.system.domain.announcement.vo.AnnouncementMarqueeVO;
import com.backstage.system.domain.websocket.WsNotifyMessage;
import com.backstage.system.enums.announcement.AnnouncementWsEventTypeEnum;
import com.backstage.system.enums.homepage.HomepageResourceTypeEnum;
import com.backstage.system.mapper.homepage.OshHomePageAnnouncementMapper;
import com.backstage.system.service.announcement.IAnnouncementRefreshMessageService;
import com.backstage.system.service.homepage.IOshHomePageAnnouncementPushService;
import com.backstage.system.service.websocket.WebSocketNotifyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 首页公告服务实现。
 */
@Service
public class OshHomePageAnnouncementPushServiceImpl implements IOshHomePageAnnouncementPushService {

    private static final Logger log = LoggerFactory.getLogger(OshHomePageAnnouncementPushServiceImpl.class);

    private static final int LIMIT_PER_RESOURCE = 5;
    private static final String HOMEPAGE_MODULE = "homepage";
    private static final String HOMEPAGE_ACTION = "refresh";
    private static final boolean HOMEPAGE_REFRESH = true;
    private static final String NOTICE_API = "/pc/homepage/announcement/notice";
    private static final String DYNAMIC_API = "/pc/homepage/announcement/dynamic";
    private static final String REFRESH_TITLE = "首页公告刷新";

    @Autowired
    private OshHomePageAnnouncementMapper homepageAnnouncementMapper;

    @Autowired
    private WebSocketNotifyService webSocketNotifyService;

    @Autowired
    private IAnnouncementRefreshMessageService announcementRefreshMessageService;

    @Override
    public List<AnnouncementMarqueeVO> getSystemNotice() {
        return queryByChannel(AnnouncementChannelEnum.SYSTEM_NOTICE.getCode());
    }

    @Override
    public List<AnnouncementMarqueeVO> getBusinessDynamic() {
        return queryByChannel(AnnouncementChannelEnum.USER_NOTICE.getCode());
    }

    @Override
    public void pushAllModulesAnnouncementsData() {
        WsNotifyMessage message = buildRefreshMessage();
        webSocketNotifyService.broadcast(message);
        log.info("[homepage-announcement] broadcast refresh event, type={}, bizId={}, requireAuth={}",
                message.getType(), message.getBizId(), message.getRequireAuth());
    }

    @Override
    public void pushAnnouncementsToUser(Long userId) {
        if (userId == null) {
            return;
        }
        WsNotifyMessage message = buildRefreshMessage();
        webSocketNotifyService.send(userId, message);
        log.info("[homepage-announcement] push refresh event to userId={}, type={}, bizId={}, requireAuth={}",
                userId, message.getType(), message.getBizId(), message.getRequireAuth());
    }

    private List<AnnouncementMarqueeVO> queryByChannel(int channel) {
        List<AnnouncementMarqueeVO> results = new ArrayList<>();
        for (HomepageResourceTypeEnum resourceTypeEnum : HomepageResourceTypeEnum.values()) {
            List<AnnouncementMarqueeVO> rows = homepageAnnouncementMapper.selectByResourceTypeAndChannel(
                    resourceTypeEnum.getCode(), channel, LIMIT_PER_RESOURCE);
            if (rows != null && !rows.isEmpty()) {
                results.addAll(rows);
            }
        }
        return results;
    }

    private WsNotifyMessage buildRefreshMessage() {
        return announcementRefreshMessageService.buildAnnouncementRefreshMessage(
                AnnouncementWsEventTypeEnum.ANNOUNCEMENT_REFRESH,
                HOMEPAGE_MODULE,
                HOMEPAGE_ACTION,
                HOMEPAGE_REFRESH,
                REFRESH_TITLE,
                NOTICE_API,
                DYNAMIC_API,
                true
        );
    }
}
