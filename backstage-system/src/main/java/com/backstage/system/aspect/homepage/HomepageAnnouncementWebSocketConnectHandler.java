package com.backstage.system.aspect.homepage;

import com.backstage.system.aspect.announcement.AnnouncementWebSocketConnectHandler;
import com.backstage.system.service.homepage.IOshHomePageAnnouncementPushService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

/**
 * 首页公告 WebSocket 建连处理器。
 */
@Order(100)
@Component
public class HomepageAnnouncementWebSocketConnectHandler implements AnnouncementWebSocketConnectHandler {

    @Autowired
    private IOshHomePageAnnouncementPushService announcementPushService;

    @Override
    public void handle(Long userId, WebSocketSession session) {
        if (userId == null) {
            return;
        }
        announcementPushService.pushAnnouncementsToUser(userId);
    }
}
