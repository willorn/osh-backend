package com.backstage.system.service.impl.course;

import com.backstage.common.async.AsyncExecutorNames;
import com.backstage.common.async.AsyncTaskSupport;
import com.backstage.common.enums.AnnouncementChannelEnum;
import com.backstage.system.domain.course.OshCourse;
import com.backstage.system.domain.user.OshUser;
import com.backstage.system.domain.websocket.WsNotifyMessage;
import com.backstage.system.mapper.course.OshCourseAnnouncementMapper;
import com.backstage.system.mapper.course.OshCourseMapper;
import com.backstage.system.mapper.user.OshUserMapper;
import com.backstage.system.service.course.CoursePurchaseAnnouncementPublisher;
import com.backstage.system.service.websocket.WebSocketNotifyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.concurrent.Executor;

@Service
public class CoursePurchaseAnnouncementPublisherImpl implements CoursePurchaseAnnouncementPublisher {

    private static final Logger log = LoggerFactory.getLogger(CoursePurchaseAnnouncementPublisherImpl.class);
    private static final String SYSTEM_OPERATOR = "system";
    private static final String COURSE_PURCHASE_SUCCESS = "COURSE_PURCHASE_SUCCESS";
    private static final String COURSE_USER_NOTICE_REFRESH = "COURSE_USER_NOTICE_REFRESH";

    @Resource
    private OshCourseAnnouncementMapper oshCourseAnnouncementMapper;

    @Resource
    private OshCourseMapper oshCourseMapper;

    @Resource
    private OshUserMapper oshUserMapper;

    @Resource
    private WebSocketNotifyService webSocketNotifyService;

    @Resource
    private AsyncTaskSupport asyncTaskSupport;

    @Resource
    @Qualifier(AsyncExecutorNames.NOTIFICATION)
    private Executor notificationTaskExecutor;

    @Override
    public void publishPurchaseSuccess(Long userId, Long courseId, String orderNo) {
        if (userId == null || courseId == null) {
            return;
        }
        log.info("提交课程购买成功异步副作用任务, orderNo={}, userId={}, courseId={}", orderNo, userId, courseId);
        asyncTaskSupport.runAsync(() -> doPublish(userId, courseId, orderNo), notificationTaskExecutor)
                .exceptionally(ex -> {
                    log.warn("课程购买公告异步发布失败, orderNo={}, error={}", orderNo, ex.getMessage(), ex);
                    return null;
                });
    }

    private void doPublish(Long userId, Long courseId, String orderNo) {
        OshCourse course = oshCourseMapper.selectCourseById(courseId);
        String courseTitle = resolveCourseTitle(course, courseId);
        String username = resolveUsername(userId);
        String jumpUrl = "/course_detail/" + courseId;
        String title = username + "购买了「" + courseTitle + "」";

        sendPersonalSuccessNotification(userId, courseTitle, jumpUrl, courseId);
        log.info("课程购买成功个人通知发送完成, orderNo={}, userId={}, courseId={}", orderNo, userId, courseId);

        oshCourseAnnouncementMapper.insertCourseAnnouncement(
                title,
                jumpUrl,
                AnnouncementChannelEnum.USER_NOTICE.getCode(),
                SYSTEM_OPERATOR
        );
        log.info("课程购买成功业务公告写入完成, orderNo={}, courseId={}, title={}", orderNo, courseId, title);

        WsNotifyMessage broadcast = new WsNotifyMessage();
        broadcast.setType(COURSE_USER_NOTICE_REFRESH);
        broadcast.setTitle(title);
        broadcast.setContent(null);
        broadcast.setJumpUrl(jumpUrl);
        broadcast.setBizId(String.valueOf(courseId));
        webSocketNotifyService.broadcast(broadcast);
        log.info("课程购买成功公告广播完成, orderNo={}, courseId={}, type={}", orderNo, courseId, COURSE_USER_NOTICE_REFRESH);
    }

    private void sendPersonalSuccessNotification(Long userId, String courseTitle, String jumpUrl, Long courseId) {
        WsNotifyMessage personal = new WsNotifyMessage();
        personal.setType(COURSE_PURCHASE_SUCCESS);
        personal.setTitle("您购买的「" + courseTitle + "」已到账");
        personal.setContent("课程已解锁，点击可前往课程详情页开始学习。");
        personal.setJumpUrl(jumpUrl);
        personal.setBizId(String.valueOf(courseId));
        webSocketNotifyService.send(userId, personal);
    }

    private String resolveCourseTitle(OshCourse course, Long courseId) {
        if (course == null || course.getTitle() == null || course.getTitle().trim().isEmpty()) {
            return "课程#" + courseId;
        }
        return course.getTitle().trim();
    }

    private String resolveUsername(Long userId) {
        if (userId == null) {
            return "有用户";
        }
        OshUser user = oshUserMapper.selectUserById(userId);
        if (user == null || user.getUsername() == null || user.getUsername().trim().isEmpty()) {
            return "有用户";
        }
        return user.getUsername();
    }
}
