package com.backstage.system.controller.audit;

import com.backstage.common.annotation.OshUserEvent;
import com.backstage.common.annotation.OshUserLevel;
import com.backstage.common.enums.ResourceStatusEnum;
import com.backstage.common.core.domain.R;
import com.backstage.common.exception.ServiceException;
import com.backstage.system.domain.audit.ResourceAuditApproveRequest;
import com.backstage.system.domain.audit.ResourceAuditPageVO;
import com.backstage.system.domain.audit.ResourceAuditRequest;
import com.backstage.system.domain.user.OshUser;
import com.backstage.system.service.audit.IResourceAuditService;
import com.backstage.system.utils.UserContextUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Api(tags = "统一资源审核")
@RestController
@RequestMapping("/pc/audit")
public class ResourceAuditController {

    @Autowired
    private IResourceAuditService resourceAuditService;

    @ApiOperation("分页查询待审核资源")
    @PostMapping("/pending")
    @OshUserLevel(value = 5)
    public R<ResourceAuditPageVO> pending(@Validated @RequestBody ResourceAuditRequest request) {
        try {
            return R.ok(resourceAuditService.pagePending(request));
        } catch (IllegalArgumentException | ServiceException ex) {
            return R.fail(ex.getMessage());
        }
    }

    @ApiOperation("审核资源")
    @PostMapping("/auditResource")
    @OshUserEvent(module = "资源审核", actionType = "审核", resourceTypeExpression = "#p0.resourceType", resourceIdExpression = "#p0.resourceId", description = "审核资源")
    @OshUserLevel(value = 5)
    public R<String> audit(@Validated @RequestBody ResourceAuditApproveRequest request) {
        OshUser currentUser = UserContextUtil.getCurrentUser();
        if (currentUser == null) {
            return R.fail("请先登录");
        }
        try {
            resourceAuditService.audit(request.getResourceType(), request.getResourceId(), request.getStatus(), currentUser.getUsername(), currentUser.getId());
            return R.ok(ResourceStatusEnum.isPublished(request.getStatus()) ? "审核通过" : "已拒绝");
        } catch (IllegalArgumentException | ServiceException ex) {
            return R.fail(ex.getMessage());
        }
    }
}
