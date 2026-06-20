package com.backstage.system.service.announcement;

import com.backstage.system.domain.websocket.WsNotifyMessage;
import com.backstage.system.enums.announcement.AnnouncementWsEventTypeEnum;

/**
 * 通用公告栏刷新消息构造 Service。
 * <p>
 * 面向所有“公告栏 / 动态栏”类模块，统一构造 WebSocket 刷新消息，
 * 避免每个模块手写 type、bizId、content JSON 结构。
 */
public interface IAnnouncementRefreshMessageService {

    /**
     * 构造公告栏刷新消息。
     *
     * @param eventType 刷新事件枚举，例如 ANNOUNCEMENT_REFRESH
     * @param module 模块标识，例如 homepage / course / tool
     * @param title 消息标题
     * @param noticeApi 公告栏查询接口，可为空
     * @param dynamicApi 动态栏查询接口，可为空
     * @param requireAuth 是否需要登录后接收
     * @return 通用 WebSocket 刷新消息
     */
    WsNotifyMessage buildAnnouncementRefreshMessage(
            AnnouncementWsEventTypeEnum eventType,
            String module,
            String action,
            boolean refresh,
            String title,
            String noticeApi,
            String dynamicApi,
            boolean requireAuth
    );
}
