package com.backstage.system.service.tool;

import com.backstage.common.response.PageResponse;
import com.backstage.system.domain.vo.pay.OrderCheckoutRespVO;
import com.backstage.system.domain.vo.tool.ToolPurchaseDetailVO;
import com.backstage.system.domain.vo.tool.ToolPurchaseListVO;
import com.backstage.system.domain.vo.tool.ToolQuotaPackageVO;
import com.backstage.system.request.tool.ToolQuotaPackageDeleteRequest;
import com.backstage.system.request.tool.ToolQuotaPackageSaveRequest;
import com.backstage.system.request.tool.ToolPurchaseCreateRequest;
import com.backstage.system.request.tool.ToolPurchaseListRequest;

import java.util.List;

public interface ToolPurchaseService {

    ToolPurchaseDetailVO getPurchaseDetail(Long userId);

    List<ToolQuotaPackageVO> listQuotaPackages();

    OrderCheckoutRespVO createPurchaseOrder(Long userId, String operator, ToolPurchaseCreateRequest request);

    PageResponse<ToolPurchaseListVO> listPurchaseRecords(Long userId, ToolPurchaseListRequest request);

    void cancelPendingPurchase(String paymentNo);

    Long saveQuotaPackage(String operator, ToolQuotaPackageSaveRequest request);

    void deleteQuotaPackage(String operator, ToolQuotaPackageDeleteRequest request);
}
