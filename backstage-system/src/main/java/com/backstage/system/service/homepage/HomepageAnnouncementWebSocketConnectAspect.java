package com.backstage.system.service.homepage;

import com.backstage.common.websocket.OshWebSocketHandler;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

/**
 * 用户 WebSocket 建连后，立即推送一次首页公告数据。
 * 仅作用于首页公告模块，不改动其他业务推送链路。
 */
@Aspect
@Component
public class HomepageAnnouncementWebSocketConnectAspect {

    @Autowired
    private IOshHomePageAnnouncementPushService announcementPushService;

    @AfterReturning(
            value = "execution(* com.backstage.common.websocket.OshWebSocketHandler.afterConnectionEstablished(..)) && args(session)",
            argNames = "session")
    public void pushHomepageAnnouncementsOnConnect(WebSocketSession session) {
        if (session == null) {
            return;
        }

        Object userIdAttr = session.getAttributes().get("userId");
        if (userIdAttr instanceof Long) {
            announcementPushService.pushAnnouncementsToUser((Long) userIdAttr);
        }
    }
}
