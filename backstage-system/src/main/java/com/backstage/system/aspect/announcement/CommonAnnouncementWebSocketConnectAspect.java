package com.backstage.system.aspect.announcement;

import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.Collections;
import java.util.List;

/**
 * 通用公告类 WebSocket 建连切面。
 * <p>
 * 统一负责在 WebSocket 建连成功后提取 userId，
 * 并按注册顺序依次触发所有模块处理器。
 */
@Aspect
@Component
public class CommonAnnouncementWebSocketConnectAspect {

    @Autowired(required = false)
    private List<AnnouncementWebSocketConnectHandler> handlers = Collections.emptyList();

    @AfterReturning(
            value = "execution(* com.backstage.common.websocket.OshWebSocketHandler.afterConnectionEstablished(..)) && args(session)",
            argNames = "session")
    public void pushAnnouncementsOnConnect(WebSocketSession session) {
        if (session == null || handlers == null || handlers.isEmpty()) {
            return;
        }

        Object userIdAttr = session.getAttributes().get("userId");
        if (!(userIdAttr instanceof Long)) {
            return;
        }

        Long userId = (Long) userIdAttr;
        for (AnnouncementWebSocketConnectHandler handler : handlers) {
            if (handler == null) {
                continue;
            }
            handler.handle(userId, session);
        }
    }
}
