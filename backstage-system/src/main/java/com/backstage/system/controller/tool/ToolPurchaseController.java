package com.backstage.system.controller.tool;

import com.backstage.common.annotation.Anonymous;
import com.backstage.common.annotation.OshUserLevel;
import com.backstage.common.core.domain.R;
import com.backstage.common.response.PageResponse;
import com.backstage.system.domain.user.OshUser;
import com.backstage.system.domain.vo.pay.OrderCheckoutRespVO;
import com.backstage.system.domain.vo.tool.ToolPurchaseDetailVO;
import com.backstage.system.domain.vo.tool.ToolPurchaseListVO;
import com.backstage.system.domain.vo.tool.ToolQuotaPackageVO;
import com.backstage.system.request.tool.ToolQuotaPackageDeleteRequest;
import com.backstage.system.request.tool.ToolQuotaPackageSaveRequest;
import com.backstage.system.request.tool.ToolPurchaseCancelRequest;
import com.backstage.system.request.tool.ToolPurchaseCreateRequest;
import com.backstage.system.request.tool.ToolPurchaseListRequest;
import com.backstage.system.service.order.OrderService;
import com.backstage.system.service.tool.ToolPurchaseService;
import com.backstage.system.utils.UserContextUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Api(tags = "工具购买")
@Validated
@RestController
@RequestMapping("/pc/tool/purchase")
public class ToolPurchaseController {

    @Autowired
    private ToolPurchaseService toolPurchaseService;

    @Autowired
    private OrderService orderService;

    @ApiOperation("查询工具购买详情")
    @GetMapping("/detail")
    @Anonymous
    public R<ToolPurchaseDetailVO> detail() {
        OshUser currentUser = UserContextUtil.getCurrentUser();
        Long userId = currentUser == null ? null : currentUser.getId();
        return R.ok(toolPurchaseService.getPurchaseDetail(userId));
    }

    @ApiOperation("查询工具点数套餐列表")
    @GetMapping("/packages")
    @Anonymous
    public R<List<ToolQuotaPackageVO>> packages() {
        return R.ok(toolPurchaseService.listQuotaPackages());
    }

    @ApiOperation("创建工具购买订单")
    @PostMapping("/create")
    @OshUserLevel(value = 1)
    public R<OrderCheckoutRespVO> create(@Validated @RequestBody ToolPurchaseCreateRequest request) {
        OshUser currentUser = UserContextUtil.getCurrentUser();
        if (currentUser == null) {
            return R.fail("请先登录");
        }
        return R.ok(toolPurchaseService.createPurchaseOrder(currentUser.getId(), currentUser.getUsername(), request));
    }

    @ApiOperation("查询我的工具购买记录")
    @PostMapping("/list")
    @OshUserLevel(value = 1)
    public R<PageResponse<ToolPurchaseListVO>> list(@RequestBody(required = false) ToolPurchaseListRequest request) {
        OshUser currentUser = UserContextUtil.getCurrentUser();
        if (currentUser == null) {
            return R.fail("请先登录");
        }
        return R.ok(toolPurchaseService.listPurchaseRecords(currentUser.getId(), request));
    }

    @ApiOperation("手动关闭工具购买订单")
    @PostMapping("/cancel")
    @OshUserLevel(value = 1)
    public R<String> cancel(@Validated @RequestBody ToolPurchaseCancelRequest request) {
        OshUser currentUser = UserContextUtil.getCurrentUser();
        if (currentUser == null) {
            return R.fail("请先登录");
        }
        orderService.cancelPaymentByOrderNo(request.getOrderNo());
        return R.ok("关单成功");
    }

    @ApiOperation("新增/修改工具点数套餐")
    @PostMapping("/package/save")
    @OshUserLevel(value = 5)
    public R<Long> savePackage(@Validated @RequestBody ToolQuotaPackageSaveRequest request) {
        OshUser currentUser = UserContextUtil.getCurrentUser();
        if (currentUser == null) {
            return R.fail("请先登录");
        }
        return R.ok(toolPurchaseService.saveQuotaPackage(currentUser.getUsername(), request));
    }

    @ApiOperation("删除工具点数套餐")
    @PostMapping("/package/delete")
    @OshUserLevel(value = 5)
    public R<String> deletePackage(@Validated @RequestBody ToolQuotaPackageDeleteRequest request) {
        OshUser currentUser = UserContextUtil.getCurrentUser();
        if (currentUser == null) {
            return R.fail("请先登录");
        }
        toolPurchaseService.deleteQuotaPackage(currentUser.getUsername(), request);
        return R.ok("删除成功");
    }
}
