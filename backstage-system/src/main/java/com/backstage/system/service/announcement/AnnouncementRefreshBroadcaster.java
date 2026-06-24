package com.backstage.system.service.announcement;

import com.alibaba.fastjson2.JSON;
import com.backstage.common.enums.AnnouncementModuleEnum;
import com.backstage.system.domain.websocket.WsNotifyMessage;
import com.backstage.system.service.websocket.WebSocketNotifyService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.HashMap;
import java.util.Map;

/**
 * 公告栏刷新广播器。
 * <p>
 * 统一封装前端 {@code HomepageAnnouncementBoard} 监听的 WebSocket 刷新协议，
 * 避免各业务模块散落拼装 {@code ANNOUNCEMENT_REFRESH} 消息体。
 *
 * @author backstage
 */
@Service
public class AnnouncementRefreshBroadcaster {

    /**
     * 前端公告栏通用刷新消息类型
     */
    private static final String ANNOUNCEMENT_REFRESH_TYPE = "ANNOUNCEMENT_REFRESH";

    /**
     * 前端公告栏通用刷新动作
     */
    private static final String REFRESH_ACTION = "refresh";

    /**
     * WebSocket 通知服务
     */
    private final WebSocketNotifyService webSocketNotifyService;

    public AnnouncementRefreshBroadcaster(WebSocketNotifyService webSocketNotifyService) {
        this.webSocketNotifyService = webSocketNotifyService;
    }

    /**
     * 广播指定模块的公告栏刷新事件。
     *
     * @param module 公告所属模块
     */
    public void broadcastRefresh(AnnouncementModuleEnum module) {
        if (module == null) {
            return;
        }
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    doBroadcastRefresh(module);
                }
            });
            return;
        }
        doBroadcastRefresh(module);
    }

    private void doBroadcastRefresh(AnnouncementModuleEnum module) {
        WsNotifyMessage message = new WsNotifyMessage();
        message.setType(ANNOUNCEMENT_REFRESH_TYPE);
        message.setTitle(module.getDesc() + "已更新");
        message.setContent(buildRefreshContent(module));
        message.setJumpUrl(null);
        webSocketNotifyService.broadcast(message);
    }

    private String buildRefreshContent(AnnouncementModuleEnum module) {
        Map<String, Object> content = new HashMap<>();
        content.put("module", module.getCode());
        content.put("action", REFRESH_ACTION);
        content.put("refresh", true);
        return JSON.toJSONString(content);
    }
}
