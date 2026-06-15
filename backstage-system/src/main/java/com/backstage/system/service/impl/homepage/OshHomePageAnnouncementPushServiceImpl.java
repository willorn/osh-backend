package com.backstage.system.service.impl.homepage;

import com.alibaba.fastjson2.JSON;
import com.backstage.system.domain.announcement.vo.AnnouncementMarqueeVO;
import com.backstage.system.domain.websocket.WsNotifyMessage;
import com.backstage.system.mapper.homepage.OshHomePageAnnouncementMapper;
import com.backstage.system.service.homepage.IOshHomePageAnnouncementPushService;
import com.backstage.system.service.websocket.WebSocketNotifyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 首页公告服务实现。
 * HTTP 提供 /notice 和 /dynamic 查询。
 * WebSocket 仅负责推送首页模块刷新事件，由前端收到事件后主动重新请求接口。
 */
@Service
public class OshHomePageAnnouncementPushServiceImpl implements IOshHomePageAnnouncementPushService {

    private static final Logger log = LoggerFactory.getLogger(OshHomePageAnnouncementPushServiceImpl.class);

    private static final String WS_TYPE_HOMEPAGE_ANNOUNCEMENT_REFRESH = "HOMEPAGE_ANNOUNCEMENT_REFRESH";
    private static final String HOMEPAGE_MODULE = "homepage";
    private static final int CHANNEL_SYSTEM = 1;
    private static final int CHANNEL_BUSINESS = 2;
    private static final int DEFAULT_LIMIT_PER_RESOURCE = 5;
    private static final int MAX_LIMIT_PER_RESOURCE = 20;
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private OshHomePageAnnouncementMapper homepageAnnouncementMapper;

    @Autowired
    private WebSocketNotifyService webSocketNotifyService;

    @Override
    public Map<String, List<AnnouncementMarqueeVO>> getSystemNotice() {
        return getGroupedAnnouncements(CHANNEL_SYSTEM, "首页系统通知");
    }

    @Override
    public Map<String, List<AnnouncementMarqueeVO>> getBusinessDynamic() {
        return getGroupedAnnouncements(CHANNEL_BUSINESS, "首页业务动态");
    }

    @Override
    public void pushAllModulesAnnouncementsData() {
        String taskId = generateTaskId();
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMATTER);
        try {
            log.info("[任务ID:{}] [时间:{}] 开始广播首页公告刷新事件", taskId, timestamp);
            webSocketNotifyService.broadcast(buildRefreshMessage());
            log.info("[任务ID:{}] 首页公告刷新事件广播完成", taskId);
        } catch (Exception e) {
            log.error("[任务ID:{}] [时间:{}] 首页公告刷新事件广播失败", taskId, timestamp, e);
        }
    }

    @Override
    public void pushAnnouncementsToUser(Long userId) {
        if (userId == null) {
            return;
        }

        try {
            webSocketNotifyService.send(userId, buildRefreshMessage());
            log.info("[首页公告] 首次连接刷新事件推送完成, userId={}", userId);
        } catch (Exception e) {
            log.warn("[首页公告] 首次连接刷新事件推送失败, userId={}, error={}", userId, e.getMessage());
        }
    }

    private Map<String, List<AnnouncementMarqueeVO>> getGroupedAnnouncements(int channel, String logLabel) {
        try {
            List<AnnouncementMarqueeVO> results =homepageAnnouncementMapper.selectByChannelGrouped(channel, normalizeLimitPerResource(DEFAULT_LIMIT_PER_RESOURCE));
            Map<String, List<AnnouncementMarqueeVO>> grouped = groupByResourceType(results);
            log.info("[{}HTTP查询] resourceGroups={}, total={}",
                    logLabel, grouped.size(), results == null ? 0 : results.size());
            return grouped;
        } catch (Exception e) {
            log.warn("[{}HTTP查询] 查询失败: {}", logLabel, e.getMessage());
            return new LinkedHashMap<>();
        }
    }

    private Map<String, List<AnnouncementMarqueeVO>> groupByResourceType(List<AnnouncementMarqueeVO> announcements) {
        Map<String, List<AnnouncementMarqueeVO>> grouped = new LinkedHashMap<>();
        if (announcements == null || announcements.isEmpty()) {
            return grouped;
        }

        for (AnnouncementMarqueeVO announcement : announcements) {
            if (announcement == null || announcement.getResourceType() == null || announcement.getResourceType().trim().isEmpty()) {
                continue;
            }
            grouped.computeIfAbsent(announcement.getResourceType(), key -> new ArrayList<>()).add(announcement);
        }
        return grouped;
    }

    private WsNotifyMessage buildRefreshMessage() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("module", HOMEPAGE_MODULE);
        payload.put("event", "refresh");
        payload.put("trigger", "websocket");

        WsNotifyMessage message = new WsNotifyMessage();
        message.setType(WS_TYPE_HOMEPAGE_ANNOUNCEMENT_REFRESH);
        message.setTitle("首页公告刷新");
        message.setBizId(HOMEPAGE_MODULE);
        message.setContent(JSON.toJSONString(payload));
        message.setRequireAuth(true);
        return message;
    }

    private String generateTaskId() {
        return "HPAB-" + System.currentTimeMillis() + "-" + Thread.currentThread().getId();
    }

    private int normalizeLimitPerResource(int limit) {
        if (limit <= 0) {
            return DEFAULT_LIMIT_PER_RESOURCE;
        }
        return Math.min(limit, MAX_LIMIT_PER_RESOURCE);
    }
}
