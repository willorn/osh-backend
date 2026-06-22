package com.backstage.system.service.impl.audit;

import com.backstage.common.enums.ResourceStatusEnum;
import com.backstage.common.enums.ResourceTypeEnum;
import com.backstage.common.utils.StringUtils;
import com.backstage.system.domain.audit.ResourceAuditItemVO;
import com.backstage.system.domain.user.OshUser;
import com.backstage.system.domain.websocket.WsNotifyMessage;
import com.backstage.system.mapper.audit.ResourceAuditMapper;
import com.backstage.system.mapper.user.OshUserMapper;
import com.backstage.system.service.audit.ResourceAuditCallbackContext;
import com.backstage.system.service.audit.ResourceAuditCallbackHandler;
import com.backstage.system.service.websocket.WebSocketNotifyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;

/**
 * 默认资源审核回调处理器：异步通知资源创建人
 */
@Component
public class DefaultResourceAuditCallbackHandler implements ResourceAuditCallbackHandler {

    private static final Logger log = LoggerFactory.getLogger(DefaultResourceAuditCallbackHandler.class);

    @Resource
    private ResourceAuditMapper resourceAuditMapper;

    @Resource
    private OshUserMapper oshUserMapper;

    @Resource
    private WebSocketNotifyService webSocketNotifyService;

    @Override
    public List<ResourceTypeEnum> resourceTypes() {
        return Arrays.asList(
                ResourceTypeEnum.QA_QUESTION,
                ResourceTypeEnum.QA_ANSWER,
                ResourceTypeEnum.BOOK,
                ResourceTypeEnum.OPEN_PROJECT,
                ResourceTypeEnum.INFO_GAP
        );
    }

    @Override
    public void handle(ResourceAuditCallbackContext context) {
        notifyResourceCreator(context.getResourceType(), context.getResourceId(), context.getResourceStatus());
    }

    private void notifyResourceCreator(ResourceTypeEnum resourceType, Long resourceId, Integer resourceStatus) {
        try {
            ResourceAuditItemVO resource = resourceAuditMapper.selectAuditNotifyItem(resourceType.getMysqlTableName(), resourceId);
            if (resource == null || StringUtils.isEmpty(resource.getCreateBy())) {
                return;
            }
            Long targetUserId = parseTargetUserId(resource.getCreateBy());
            if (targetUserId == null) {
                log.warn("审核通知跳过：无法解析资源创建人ID, resourceType={}, resourceId={}, createBy={}",
                        resourceType.getType(), resourceId, resource.getCreateBy());
                return;
            }
            WsNotifyMessage message = new WsNotifyMessage();
            message.setType("RESOURCE_AUDIT_RESULT");
            message.setTitle(ResourceStatusEnum.isPublished(resourceStatus) ? "资源审核通过" : "资源审核未通过");
            message.setContent(webSocketNotifyService.truncate(buildAuditNotifyContent(resourceType, resource, resourceStatus)));
            message.setJumpUrl("/audit");
            message.setBizId(String.valueOf(resourceId));
            webSocketNotifyService.send(targetUserId, message);
        } catch (Exception ex) {
            log.error("审核结果WebSocket通知失败, resourceType={}, resourceId={}, error={}",
                    resourceType.getType(), resourceId, ex.getMessage(), ex);
        }
    }

    private Long parseTargetUserId(String createBy) {
        try {
            return Long.valueOf(createBy);
        } catch (Exception ignored) {
            OshUser user = oshUserMapper.getUserByUsername(createBy);
            return user == null ? null : user.getId();
        }
    }

    private String buildAuditNotifyContent(ResourceTypeEnum resourceType, ResourceAuditItemVO resource, Integer resourceStatus) {
        String resourceName = StringUtils.isEmpty(resource.getTitle()) ? String.valueOf(resource.getId()) : resource.getTitle();
        String statusText = ResourceStatusEnum.isPublished(resourceStatus) ? "已通过" : "已拒绝";
        return getResourceTypeLabel(resourceType) + "「" + resourceName + "」审核" + statusText;
    }

    private String getResourceTypeLabel(ResourceTypeEnum resourceType) {
        switch (resourceType) {
            case COURSE:
                return "课程";
            case QA_QUESTION:
                return "答疑问题";
            case QA_ANSWER:
                return "答疑回答";
            case BOOK:
                return "电子书";
            case TOOL:
                return "工具";
            case WEBSITE:
                return "实用网站";
            case OPEN_PROJECT:
                return "开源项目";
            case INFO_GAP:
                return "信息差";
            default:
                return resourceType.getType();
        }
    }
}
