package com.backstage.system.service.website.impl;

import com.backstage.system.domain.announcement.vo.AnnouncementMarqueeVO;
import com.backstage.system.domain.user.OshUser;
import com.backstage.system.mapper.announcement.OshAnnouncementMapper;
import com.backstage.system.mapper.user.OshUserMapper;
import com.backstage.system.service.website.IWebsiteAnnouncementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 实用网站公告/动态 Service 实现
 *
 * 公告栏（channel=1）：审核通过时写入，标题格式：「GitHub」已上线，快去看看！
 * 动态栏（channel=2）：用户好评时写入，标题格式：张** 给「GitHub」点了好评
 */
@Service
public class WebsiteAnnouncementServiceImpl implements IWebsiteAnnouncementService {

    private static final Logger log = LoggerFactory.getLogger(WebsiteAnnouncementServiceImpl.class);

    /** channel=1：公告栏 */
    private static final int CHANNEL_NOTICE  = 1;
    /** channel=2：动态栏 */
    private static final int CHANNEL_DYNAMIC = 2;

    private static final String NOTICE_ICON   = "🌐";

    private static final String DYNAMIC_ICON  = "👍";

    private static final String RESOURCE_TYPE_WEBSITE = "website";

    @Autowired
    private OshAnnouncementMapper announcementMapper;

    @Autowired
    private OshUserMapper userMapper;

    // ==================== 公告栏 ====================

    @Override
    public void insertWebsiteNotice(Long websiteId, String websiteName) {
        try {
            String title = buildNoticeTitle(websiteName);
            // 幂等：同一网站已有公告则跳过
            int exists = announcementMapper.countWebsiteAnnouncementByResourceAndTitle(
                    websiteId, title, CHANNEL_NOTICE);
            if (exists > 0) {
                log.info("【网站公告】已存在，跳过，websiteId={}", websiteId);
                return;
            }
            String link = "/website/detail/" + websiteId;
            announcementMapper.insertWebsiteAnnouncement(
                    title, link, NOTICE_ICON,
                    RESOURCE_TYPE_WEBSITE, websiteId, CHANNEL_NOTICE);
            log.info("【网站公告】写入成功，websiteId={}, title={}", websiteId, title);
        } catch (Exception e) {
            log.error("【网站公告】写入失败，websiteId={}, error={}", websiteId, e.getMessage());
        }
    }

    // ==================== 动态栏 ====================

    @Override
    public void insertWebsiteDynamic(Long userId, Long websiteId, String websiteName) {
        try {
            String maskedUsername = buildMaskedUsername(userId);
            String title = buildDynamicTitle(maskedUsername, websiteName);
            // 幂等：同一用户对同一网站的好评动态只写一条
            int exists = announcementMapper.countWebsiteAnnouncementByResourceAndTitle(
                    websiteId, title, CHANNEL_DYNAMIC);
            if (exists > 0) {
                log.info("【网站动态】已存在，跳过，websiteId={}, userId={}", websiteId, userId);
                return;
            }
            announcementMapper.insertWebsiteAnnouncement(
                    title, "", DYNAMIC_ICON,
                    RESOURCE_TYPE_WEBSITE, websiteId, CHANNEL_DYNAMIC);
            log.info("【网站动态】写入成功，websiteId={}, title={}", websiteId, title);
        } catch (Exception e) {
            // 动态写入失败不影响评价主流程
            log.error("【网站动态】写入失败，websiteId={}, userId={}, error={}",
                    websiteId, userId, e.getMessage());
        }
    }

    // ==================== 查询 ====================

    @Override
    public List<AnnouncementMarqueeVO> getWebsiteNotices(int limit) {
        int safeLimit = (limit <= 0 || limit > 50) ? 10 : limit;
        return announcementMapper.selectWebsiteNotices(safeLimit);
    }

    @Override
    public List<AnnouncementMarqueeVO> getWebsiteDynamics(int limit) {
        int safeLimit = (limit <= 0 || limit > 50) ? 10 : limit;
        return announcementMapper.selectWebsiteDynamics(safeLimit);
    }

    // ==================== 私有工具方法 ====================

    /**
     * 构建公告栏标题
     * 格式：「GitHub」已上线，快去看看！
     */
    private String buildNoticeTitle(String websiteName) {
        String safeName = websiteName != null ? websiteName : "新网站";
        return String.format("「%s」已上线，快去看看！", safeName);
    }

    /**
     * 构建动态栏标题
     * 格式：张** 给「GitHub」点了好评
     */
    private String buildDynamicTitle(String maskedUsername, String websiteName) {
        String safeName = websiteName != null ? websiteName : "某网站";
        return String.format("%s 给「%s」点了好评", maskedUsername, safeName);
    }

    /**
     * 根据 userId 查询用户，构建脱敏用户名（取前2位 + **）
     */
    private String buildMaskedUsername(Long userId) {
        if (userId == null) return "某用户";
        try {
            OshUser user = userMapper.selectUserById(userId);
            if (user == null) return "某用户";
            String name = user.getUsername();
            if (name == null || name.isEmpty()) return "某用户";
            return name.length() <= 2
                    ? name.charAt(0) + "**"
                    : name.substring(0, 2) + "**";
        } catch (Exception e) {
            log.warn("【网站动态】获取用户信息失败，userId={}", userId);
            return "某用户";
        }
    }
}
