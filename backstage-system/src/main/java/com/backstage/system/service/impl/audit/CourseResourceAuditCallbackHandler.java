package com.backstage.system.service.impl.audit;

import com.backstage.common.enums.AnnouncementChannelEnum;
import com.backstage.common.enums.ResourceStatusEnum;
import com.backstage.common.enums.ResourceTypeEnum;
import com.backstage.common.utils.StringUtils;
import com.backstage.system.domain.audit.ResourceAuditItemVO;
import com.backstage.system.domain.user.OshUser;
import com.backstage.system.domain.websocket.WsNotifyMessage;
import com.backstage.system.mapper.audit.ResourceAuditMapper;
import com.backstage.system.mapper.course.OshCourseAnnouncementMapper;
import com.backstage.system.mapper.user.OshUserMapper;
import com.backstage.system.service.audit.ResourceAuditCallbackContext;
import com.backstage.system.service.audit.ResourceAuditCallbackHandler;
import com.backstage.system.service.websocket.WebSocketNotifyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;

/**
 * 课程资源审核回调处理器
 */
@Component
public class CourseResourceAuditCallbackHandler implements ResourceAuditCallbackHandler {

    private static final Logger log = LoggerFactory.getLogger(CourseResourceAuditCallbackHandler.class);
    private static final String COURSE_AUDIT_RESULT = "COURSE_AUDIT_RESULT";
    private static final String COURSE_USER_NOTICE_REFRESH = "COURSE_USER_NOTICE_REFRESH";
    private static final String SYSTEM_OPERATOR = "system";

    @Resource
    private ResourceAuditMapper resourceAuditMapper;

    @Resource
    private OshUserMapper oshUserMapper;

    @Resource
    private OshCourseAnnouncementMapper oshCourseAnnouncementMapper;

    @Resource
    private WebSocketNotifyService webSocketNotifyService;

    @Override
    public List<ResourceTypeEnum> resourceTypes() {
        return Collections.singletonList(ResourceTypeEnum.COURSE);
    }

    @Override
    public void handle(ResourceAuditCallbackContext context) {
        ResourceAuditItemVO resource = resourceAuditMapper.selectAuditNotifyItem(
                ResourceTypeEnum.COURSE.getMysqlTableName(),
                context.getResourceId()
        );
        if (resource == null) {
            return;
        }
        String courseName = StringUtils.isEmpty(resource.getTitle()) ? String.valueOf(context.getResourceId()) : resource.getTitle();
        String jumpUrl = buildCourseJumpUrl(context.getResourceId());
        notifyCreator(resource, context.getResourceId(), context.getResourceStatus(), courseName, jumpUrl);
        if (ResourceStatusEnum.isPublished(context.getResourceStatus())) {
            publishCourseOnline(courseName, context.getResourceId(), jumpUrl);
        }
    }

    private void notifyCreator(ResourceAuditItemVO resource,
                               Long resourceId,
                               Integer resourceStatus,
                               String courseName,
                               String jumpUrl) {
        if (StringUtils.isEmpty(resource.getCreateBy())) {
            return;
        }
        Long targetUserId = parseTargetUserId(resource.getCreateBy());
        if (targetUserId == null) {
            log.warn("课程审核通知跳过：无法解析创建人ID, resourceId={}, createBy={}", resourceId, resource.getCreateBy());
            return;
        }
        WsNotifyMessage message = new WsNotifyMessage();
        message.setType(COURSE_AUDIT_RESULT);
        message.setTitle(ResourceStatusEnum.isPublished(resourceStatus) ? "课程审核通过" : "课程审核未通过");
        message.setContent(webSocketNotifyService.truncate(buildCreatorContent(courseName, resourceStatus)));
        message.setJumpUrl(jumpUrl);
        message.setBizId(String.valueOf(resourceId));
        webSocketNotifyService.send(targetUserId, message);
    }

    private void publishCourseOnline(String courseName, Long courseId, String jumpUrl) {
        String title = "课程上新：「" + courseName + "」已上线";
        oshCourseAnnouncementMapper.insertCourseAnnouncement(
                title,
                jumpUrl,
                AnnouncementChannelEnum.SYSTEM_NOTICE.getCode(),
                SYSTEM_OPERATOR
        );

        WsNotifyMessage broadcast = new WsNotifyMessage();
        broadcast.setType(COURSE_USER_NOTICE_REFRESH);
        broadcast.setTitle(title);
        broadcast.setContent("新课程已上线，点击可前往查看。");
        broadcast.setJumpUrl(jumpUrl);
        broadcast.setBizId(String.valueOf(courseId));
        webSocketNotifyService.broadcast(broadcast);
    }

    private String buildCreatorContent(String courseName, Integer resourceStatus) {
        return "课程「" + courseName + "」审核" + (ResourceStatusEnum.isPublished(resourceStatus) ? "已通过" : "未通过");
    }

    private String buildCourseJumpUrl(Long courseId) {
        return "/course_detail/" + courseId;
    }

    private Long parseTargetUserId(String createBy) {
        try {
            return Long.valueOf(createBy);
        } catch (Exception ignored) {
            OshUser user = oshUserMapper.getUserByUsername(createBy);
            return user == null ? null : user.getId();
        }
    }
}
