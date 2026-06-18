package com.backstage.system.service.tool;

import com.backstage.common.constant.OshUserConstants;
import com.backstage.common.core.redis.RedisCache;
import com.backstage.common.exception.ServiceException;
import com.backstage.system.domain.tool.OshTool;
import com.backstage.system.domain.tool.OshToolPackage;
import com.backstage.system.domain.tool.OshToolPurchaseRecord;
import com.backstage.system.domain.user.OshUserAsset;
import com.backstage.system.mapper.tool.OshToolMapper;
import com.backstage.system.mapper.tool.OshToolPackageMapper;
import com.backstage.system.mapper.tool.OshToolPurchaseRecordMapper;
import com.backstage.system.request.tool.ToolQuotaPackageSaveRequest;
import com.backstage.system.mapper.user.OshUserAssetMapper;
import com.backstage.system.mapper.user.OshUserAssetRecordMapper;
import com.backstage.system.request.tool.ToolPurchaseCreateRequest;
import com.backstage.system.service.impl.tool.ToolPurchaseServiceImpl;
import com.backstage.system.service.order.OrderCheckoutService;
import com.backstage.system.domain.vo.pay.OrderCheckoutRespVO;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ToolPurchaseServiceImplTest {

    @InjectMocks
    private ToolPurchaseServiceImpl toolPurchaseService;

    @Mock
    private OshToolMapper oshToolMapper;

    @Mock
    private OshToolPackageMapper oshToolPackageMapper;

    @Mock
    private OshToolPurchaseRecordMapper oshToolPurchaseRecordMapper;

    @Mock
    private OshUserAssetMapper oshUserAssetMapper;

    @Mock
    private OshUserAssetRecordMapper oshUserAssetRecordMapper;

    @Mock
    private RedisCache redisCache;

    @Mock
    private OrderCheckoutService orderCheckoutService;

    @Test
    public void shouldCreateCashOnlyToolPurchaseOrderWithSnapshotRecord() {
        ToolPurchaseCreateRequest request = new ToolPurchaseCreateRequest();
        request.setPackageId(2001L);
        request.setPayType(1);
        request.setPaymentMethod("wxpay");

        OshToolPackage quotaPackage = new OshToolPackage();
        quotaPackage.setId(2001L);
        quotaPackage.setPackageName("体验包");
        quotaPackage.setUseCount(10);
        quotaPackage.setPrice(new BigDecimal("9.90"));
        quotaPackage.setPointCost(0);
        quotaPackage.setPayType(1);
        quotaPackage.setStatus(1);

        OrderCheckoutRespVO checkoutRespVO = new OrderCheckoutRespVO();
        checkoutRespVO.setOrderNo("O20260517001");
        checkoutRespVO.setPaymentNo("P20260517001");
        checkoutRespVO.setPrice(new BigDecimal("9.90"));
        checkoutRespVO.setNeedPay(true);

        when(oshToolPackageMapper.selectPackageById(2001L)).thenReturn(quotaPackage);
        when(orderCheckoutService.checkout(any())).thenReturn(checkoutRespVO);
        when(oshToolPurchaseRecordMapper.insertToolPurchaseRecord(any(OshToolPurchaseRecord.class))).thenReturn(1);

        OrderCheckoutRespVO result = toolPurchaseService.createPurchaseOrder(9L, "normal", request);

        assertEquals("O20260517001", result.getOrderNo());
        verify(oshToolPurchaseRecordMapper).insertToolPurchaseRecord(argThat(record ->
                "O20260517001".equals(record.getOrderNo())
                        && Integer.valueOf(1).equals(record.getPackagePayTypeSnapshot())
                        && Integer.valueOf(10).equals(record.getPackageUseCountSnapshot())
                        && new BigDecimal("9.90").compareTo(record.getPackageCashAmountSnapshot()) == 0
        ));
        verify(oshUserAssetMapper, never()).updateById(any(OshUserAsset.class));
    }
    

    @Test(expected = ServiceException.class)
    public void shouldRejectCreateOrderWhenPointsAreInsufficientForCashPointPackage() {
        ToolPurchaseCreateRequest request = new ToolPurchaseCreateRequest();
        request.setPackageId(2002L);
        request.setPayType(3);
        request.setPaymentMethod("points");

        OshToolPackage quotaPackage = new OshToolPackage();
        quotaPackage.setId(2002L);
        quotaPackage.setPackageName("推荐包");
        quotaPackage.setUseCount(50);
        quotaPackage.setPrice(new BigDecimal("29.90"));
        quotaPackage.setPointCost(100);
        quotaPackage.setPayType(3);
        quotaPackage.setStatus(1);

        OshUserAsset userAsset = new OshUserAsset();
        userAsset.setUserId(9L);
        userAsset.setPoints(99L);

        when(oshToolPackageMapper.selectPackageById(2002L)).thenReturn(quotaPackage);
        when(oshUserAssetMapper.selectOne(any())).thenReturn(userAsset);

        try {
            toolPurchaseService.createPurchaseOrder(9L, "normal", request);
        } finally {
            verify(orderCheckoutService, never()).checkout(any());
        }
    }

    @Test
    public void shouldAutoConvertPointCostWhenSavingCashPointQuotaPackage() {
        ToolQuotaPackageSaveRequest request = new ToolQuotaPackageSaveRequest();
        request.setPackageName("积分包");
        request.setUseCount(200);
        request.setPrice(new BigDecimal("9.90"));
        request.setPointCost(1);
        request.setPayType(3);
        request.setStatus(1);
        request.setSortOrder(5);

        when(oshToolPackageMapper.insertPackage(any(OshToolPackage.class))).thenAnswer(invocation -> {
            OshToolPackage quotaPackage = invocation.getArgument(0);
            quotaPackage.setId(3001L);
            return 1;
        });

        Long packageId = toolPurchaseService.saveQuotaPackage("admin", request);

        assertEquals(Long.valueOf(3001L), packageId);
        verify(oshToolPackageMapper).insertPackage(argThat(quotaPackage ->
                Integer.valueOf(99).equals(quotaPackage.getPointCost())
                        && Integer.valueOf(3).equals(quotaPackage.getPayType())
        ));
    }
}
