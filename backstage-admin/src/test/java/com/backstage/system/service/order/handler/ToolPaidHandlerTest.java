package com.backstage.system.service.order.handler;

import com.backstage.system.domain.order.enums.ProductTypeEnum;
import com.backstage.system.domain.tool.OshToolPurchaseRecord;
import com.backstage.system.mapper.tool.OshToolQuotaMapper;
import com.backstage.system.mapper.tool.OshToolPurchaseRecordMapper;
import com.backstage.system.service.tool.ToolPurchaseAnnouncementPublisher;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.time.LocalDateTime;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ToolPaidHandlerTest {

    @InjectMocks
    private ToolPaidHandler toolPaidHandler;

    @Mock
    private OshToolPurchaseRecordMapper oshToolPurchaseRecordMapper;

    @Mock
    private OshToolQuotaMapper oshToolQuotaMapper;

    @Mock
    private ToolPurchaseAnnouncementPublisher toolPurchaseAnnouncementPublisher;

    @Test
    public void shouldExposeToolBizTypeForPaidHandlerRegistry() {
        assertEquals(ProductTypeEnum.TOOL.getName(), toolPaidHandler.bizType());
    }

    @Test
    public void shouldGrantToolQuotaAndMarkRecordWhenPaymentSucceeded() {
        OshToolPurchaseRecord record = new OshToolPurchaseRecord();
        record.setId(1L);
        record.setOrderNo("O20260517002");
        record.setUserId(9L);
        record.setPackageUseCountSnapshot(50);
        record.setGrantStatus(0);

        when(oshToolPurchaseRecordMapper.selectByOrderNo("O20260517002")).thenReturn(record);
        when(oshToolQuotaMapper.increaseUserQuota(9L, 50, "system")).thenReturn(0);
        when(oshToolQuotaMapper.insertUserQuota(9L, 50, "system")).thenReturn(1);
        when(oshToolPurchaseRecordMapper.updateGrantSuccess(org.mockito.ArgumentMatchers.eq(1L), any(LocalDateTime.class), org.mockito.ArgumentMatchers.eq("system"))).thenReturn(1);

        toolPaidHandler.handle("O20260517002");

        verify(oshToolQuotaMapper).insertUserQuota(9L, 50, "system");
        verify(oshToolPurchaseRecordMapper).updateGrantSuccess(org.mockito.ArgumentMatchers.eq(1L), any(LocalDateTime.class), org.mockito.ArgumentMatchers.eq("system"));
    }

    @Test
    public void shouldSkipGrantWhenRecordHasAlreadyBeenGranted() {
        OshToolPurchaseRecord record = new OshToolPurchaseRecord();
        record.setId(1L);
        record.setOrderNo("O20260517002");
        record.setGrantStatus(1);

        when(oshToolPurchaseRecordMapper.selectByOrderNo("O20260517002")).thenReturn(record);

        toolPaidHandler.handle("O20260517002");

        verify(oshToolQuotaMapper, never()).insertUserQuota(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyString());
    }
}
