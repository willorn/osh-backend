package com.backstage.system.service.impl.announcement;

import com.alibaba.fastjson2.JSON;
import com.backstage.system.domain.websocket.WsNotifyMessage;
import com.backstage.system.enums.announcement.AnnouncementWsEventTypeEnum;
import com.backstage.system.service.announcement.IAnnouncementRefreshMessageService;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 通用公告栏刷新消息构造 Service 实现。
 */
@Service
public class AnnouncementRefreshMessageServiceImpl implements IAnnouncementRefreshMessageService {

    @Override
    public WsNotifyMessage buildAnnouncementRefreshMessage(
            AnnouncementWsEventTypeEnum eventType,
            String module,
            String action,
            boolean refresh,
            String title,
            String noticeApi,
            String dynamicApi,
            boolean requireAuth
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("module", module);
        payload.put("action", action);
        payload.put("refresh", refresh);
        payload.put("noticeApi", noticeApi);
        payload.put("dynamicApi", dynamicApi);

        WsNotifyMessage message = new WsNotifyMessage();
        message.setType(eventType.getCode());
        message.setTitle(title);
        message.setBizId(module);
        message.setContent(JSON.toJSONString(payload));
        message.setRequireAuth(requireAuth);
        return message;
    }
}
