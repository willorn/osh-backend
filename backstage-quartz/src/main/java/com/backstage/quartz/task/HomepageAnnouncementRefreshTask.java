package com.backstage.quartz.task;

import com.backstage.system.service.homepage.IOshHomePageAnnouncementPushService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 首页公告刷新事件 XXL-Job 任务。
 *
 * handler 名称：homepage-announcement-refresh
 * 调度建议：0 0 * * * ?
 */
@Component
public class HomepageAnnouncementRefreshTask {

    private static final Logger log = LoggerFactory.getLogger(HomepageAnnouncementRefreshTask.class);

    @Resource
    private IOshHomePageAnnouncementPushService homePageAnnouncementPushService;

    @XxlJob("homepage-announcement-refresh")
    public void pushHomepageAnnouncementRefreshEvent() {
        XxlJobHelper.log("首页公告刷新事件任务开始");
        log.info("[homepage-announcement] xxl-job refresh event start");
        try {
            homePageAnnouncementPushService.pushAllModulesAnnouncementsData();
            XxlJobHelper.log("首页公告刷新事件任务完成");
            log.info("[homepage-announcement] xxl-job refresh event finished");
        } catch (Exception ex) {
            XxlJobHelper.log("首页公告刷新事件任务失败: {0}", ex.getMessage());
            log.error("[homepage-announcement] xxl-job refresh event failed, error={}", ex.getMessage(), ex);
            throw ex;
        }
    }
}
