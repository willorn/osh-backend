package com.backstage.web.controller.tool;

import com.backstage.common.constant.OshUserConstants;
import com.backstage.common.threadlocal.ThreadLocalUtil;
import com.backstage.system.controller.tool.ToolPurchaseController;
import com.backstage.system.domain.user.OshUser;
import com.backstage.system.domain.vo.pay.OrderCheckoutRespVO;
import com.backstage.system.domain.vo.tool.ToolPurchaseDetailVO;
import com.backstage.system.domain.vo.tool.ToolQuotaPackageVO;
import com.backstage.system.request.tool.ToolQuotaPackageDeleteRequest;
import com.backstage.system.request.tool.ToolQuotaPackageSaveRequest;
import com.backstage.system.request.tool.ToolPurchaseCreateRequest;
import com.backstage.system.service.order.OrderService;
import com.backstage.system.service.tool.ToolPurchaseService;
import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class ToolPurchaseControllerTest {

    private MockMvc mockMvc;
    private ToolPurchaseService toolPurchaseService;
    private OrderService orderService;

    @Before
    public void setUp() {
        ToolPurchaseController controller = new ToolPurchaseController();
        toolPurchaseService = mock(ToolPurchaseService.class);
        ReflectionTestUtils.setField(controller, "toolPurchaseService", toolPurchaseService);
        orderService = mock(OrderService.class);
        ReflectionTestUtils.setField(controller, "orderService", orderService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @After
    public void tearDown() {
        ThreadLocalUtil.remove();
    }

    @Test
    public void shouldReturnPurchaseDetail() throws Exception {
        ToolPurchaseDetailVO detailVO = new ToolPurchaseDetailVO();
        detailVO.setRemainingCount(12);

        when(toolPurchaseService.getPurchaseDetail(eq(null))).thenReturn(detailVO);

        mockMvc.perform(get("/pc/tool/purchase/detail"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.remainingCount").value(12));

        verify(toolPurchaseService).getPurchaseDetail(eq(null));
    }

    @Test
    public void shouldReturnQuotaPackages() throws Exception {
        ToolQuotaPackageVO packageVO = new ToolQuotaPackageVO();
        packageVO.setPackageId(1001L);
        packageVO.setPackageName("基础包");

        when(toolPurchaseService.listQuotaPackages()).thenReturn(java.util.Collections.singletonList(packageVO));

        mockMvc.perform(get("/pc/tool/purchase/packages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].packageId").value(1001))
                .andExpect(jsonPath("$.data[0].packageName").value("基础包"));
    }

    @Test
    public void shouldCreatePurchaseOrder() throws Exception {
        OshUser user = new OshUser();
        user.setId(9L);
        user.setUsername("normal");
        ThreadLocalUtil.set(OshUserConstants.USER_INFO, user);

        OrderCheckoutRespVO respVO = new OrderCheckoutRespVO();
        respVO.setOrderNo("O20260517003");
        respVO.setPaymentNo("P20260517003");
        respVO.setExpireTime("2026-05-24 16:30:00");
        respVO.setCloseExpireMinutes(30);

        when(toolPurchaseService.createPurchaseOrder(eq(9L), eq("normal"), any(ToolPurchaseCreateRequest.class))).thenReturn(respVO);

        mockMvc.perform(post("/pc/tool/purchase/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"packageId\":2001,\"payType\":1,\"paymentMethod\":\"wxpay\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.orderNo").value("O20260517003"))
                .andExpect(jsonPath("$.data.paymentNo").value("P20260517003"))
                .andExpect(jsonPath("$.data.expireTime").value("2026-05-24 16:30:00"))
                .andExpect(jsonPath("$.data.closeExpireMinutes").value(30));
    }

    @Test
    public void shouldListPurchaseRecords() throws Exception {
        OshUser user = new OshUser();
        user.setId(9L);
        user.setUsername("normal");
        ThreadLocalUtil.set(OshUserConstants.USER_INFO, user);

        when(toolPurchaseService.listPurchaseRecords(eq(9L), any())).thenReturn(com.backstage.common.response.PageResponse.of(java.util.Collections.emptyList(), 0L, 1, 10));

        mockMvc.perform(post("/pc/tool/purchase/list")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pageNum\":1,\"pageSize\":10}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(0));
    }

    @Test
    public void shouldCancelPurchaseOrder() throws Exception {
        OshUser user = new OshUser();
        user.setId(9L);
        user.setUsername("normal");
        ThreadLocalUtil.set(OshUserConstants.USER_INFO, user);

                mockMvc.perform(post("/pc/tool/purchase/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderNo\":\"O20260517003\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.msg").value("操作成功"));

        verify(orderService).cancelPaymentByOrderNo("O20260517003");
    }

    @Test
    public void shouldSaveQuotaPackageWhenLevelIsEnough() throws Exception {
        OshUser user = new OshUser();
        user.setId(9L);
        user.setUsername("normal");
        ThreadLocalUtil.set(OshUserConstants.USER_INFO, user);
        ThreadLocalUtil.set(OshUserConstants.LEVEL, "5");

        when(toolPurchaseService.saveQuotaPackage(eq("normal"), any(ToolQuotaPackageSaveRequest.class))).thenReturn(1001L);

        mockMvc.perform(post("/pc/tool/purchase/package/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"packageName\":\"基础包\",\"useCount\":100,\"price\":9.90,\"payType\":1,\"status\":1,\"sortOrder\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(1001));
    }

    @Test
    public void shouldDeleteQuotaPackageWhenLevelIsEnough() throws Exception {
        OshUser user = new OshUser();
        user.setId(9L);
        user.setUsername("normal");
        ThreadLocalUtil.set(OshUserConstants.USER_INFO, user);
        ThreadLocalUtil.set(OshUserConstants.LEVEL, "5");

        mockMvc.perform(post("/pc/tool/purchase/package/delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"packageId\":1001}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(toolPurchaseService).deleteQuotaPackage(eq("normal"), any(ToolQuotaPackageDeleteRequest.class));
    }
}
