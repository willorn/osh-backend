package com.backstage.system.aspect.announcement;

import org.springframework.web.socket.WebSocketSession;

/**
 * 公告类 WebSocket 建连处理器。
 * <p>
 * 各模块实现本接口并注册为 Spring Bean 后，
 * 会在 WebSocket 建连成功时由统一切面依次触发。
 */
public interface AnnouncementWebSocketConnectHandler {

    /**
     * WebSocket 建连成功后的模块处理逻辑。
     *
     * @param userId 当前建连用户
     * @param session 当前 WebSocket 会话
     */
    void handle(Long userId, WebSocketSession session);
}
