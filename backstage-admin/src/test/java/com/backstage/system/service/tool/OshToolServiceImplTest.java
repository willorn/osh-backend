package com.backstage.system.service.tool;

import com.backstage.system.domain.tool.OshTool;
import com.backstage.system.domain.tool.OshToolPackage;
import com.backstage.system.domain.tool.OshToolTag;
import com.backstage.system.domain.user.OshUser;
import com.backstage.common.enums.ResourceCodePrefixEnum;
import com.backstage.common.exception.ServiceException;
import com.backstage.system.mapper.tool.OshToolCollectionMapper;
import com.backstage.system.mapper.tool.OshToolMapper;
import com.backstage.system.mapper.tool.OshToolPackageMapper;
import com.backstage.system.mapper.tool.OshToolTagMapper;
import com.backstage.system.mapper.tool.OshToolVoteMapper;
import com.backstage.system.domain.tool.ToolUsagePermission;
import com.backstage.system.service.OutboxEventService;
import com.backstage.system.request.tool.ToolPackageSaveRequest;
import com.backstage.system.request.tool.ToolSaveRequest;
import com.backstage.system.service.impl.tool.OshToolServiceImpl;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class OshToolServiceImplTest {

    @InjectMocks
    private OshToolServiceImpl toolService;

    @Mock
    private OshToolMapper oshToolMapper;

    @Mock
    private OshToolTagMapper oshToolTagMapper;

    @Mock
    private OshToolPackageMapper oshToolPackageMapper;

    @Mock
    private OshToolCollectionMapper oshToolCollectionMapper;

    @Mock
    private OshToolVoteMapper oshToolVoteMapper;

    @Mock
    private IOshToolEsService oshToolEsService;

    @Mock
    private OutboxEventService outboxEventService;

    @Mock
    private ResourceNoGenerator resourceNoGenerator;

    @Test
    public void shouldCreateMissingTagsWhenCreatingToolWithNewTagNames() {
        ToolSaveRequest request = new ToolSaveRequest();
        request.setToolName("图片转PDF");
        request.setRoutePath("/tool/image-to-pdf");
        request.setTags(Arrays.asList("PDF工具", "图片工具"));

        OshUser operator = new OshUser();
        operator.setUsername("admin");

        when(oshToolMapper.insertTool(any(OshTool.class))).thenAnswer(invocation -> {
            OshTool tool = invocation.getArgument(0);
            tool.setId(10001L);
            return 1;
        });
        when(resourceNoGenerator.generateUniqueNo(eq(ResourceCodePrefixEnum.TOOL), any())).thenReturn("tlAb12Cd");
        when(oshToolTagMapper.selectByName("PDF工具")).thenReturn(null);
        when(oshToolTagMapper.selectByName("图片工具")).thenReturn(null);
        when(oshToolTagMapper.insertToolTag(any(OshToolTag.class))).thenAnswer(invocation -> {
            OshToolTag tag = invocation.getArgument(0);
            tag.setId("PDF工具".equals(tag.getName()) ? 1L : 2L);
            return 1;
        });

        Long toolId = toolService.createTool(request, operator);

        assertEquals(Long.valueOf(10001L), toolId);
        verify(oshToolMapper).insertTool(argThat(tool -> Integer.valueOf(2).equals(tool.getStatus()) && "tlAb12Cd".equals(tool.getNo())));
        verify(oshToolTagMapper, times(2)).insertToolTag(any(OshToolTag.class));
        verify(oshToolTagMapper).insertToolTagRel(eq(10001L), eq(1L), eq("admin"));
        verify(oshToolTagMapper).insertToolTagRel(eq(10001L), eq(2L), eq("admin"));
        verify(oshToolEsService).buildIndexMessage(eq(10001L), eq(ToolIndexEventType.TOOL_INDEX_CREATE));
    }

    @Test
    public void shouldCreateToolPackagesWhenCreatingToolWithPackages() {
        ToolPackageSaveRequest packageRequest = new ToolPackageSaveRequest();
        packageRequest.setPackageName("10次体验包");
        packageRequest.setUseCount(10);
        packageRequest.setPrice(new BigDecimal("9.90"));
        packageRequest.setSortOrder(10);

        ToolSaveRequest request = new ToolSaveRequest();
        request.setToolName("测试工具");
        request.setRoutePath("/test/test");
        request.setResourceType("CASH_ONLY");
        request.setPackages(Collections.singletonList(packageRequest));

        OshUser operator = new OshUser();
        operator.setUsername("admin");

        when(oshToolMapper.insertTool(any(OshTool.class))).thenAnswer(invocation -> {
            OshTool tool = invocation.getArgument(0);
            tool.setId(10002L);
            return 1;
        });
        when(resourceNoGenerator.generateUniqueNo(eq(ResourceCodePrefixEnum.TOOL), any())).thenReturn("tlCd34Ef");
        when(oshToolPackageMapper.insertToolPackage(any(OshToolPackage.class))).thenReturn(1);

        Long toolId = toolService.createTool(request, operator);

        assertEquals(Long.valueOf(10002L), toolId);
        verify(oshToolPackageMapper).insertToolPackage(argThat(toolPackage ->
                Integer.valueOf(0).equals(toolPackage.getPointCost())
                        && Integer.valueOf(1).equals(toolPackage.getPayType())
        ));
        verify(oshToolMapper).insertTool(argThat(tool -> Integer.valueOf(2).equals(tool.getStatus())));
        verify(oshToolEsService).buildIndexMessage(eq(10002L), eq(ToolIndexEventType.TOOL_INDEX_CREATE));
    }

    @Test
    public void shouldAlwaysCreateInternalToolAndClearPointCostWhenSavingTool() {
        ToolSaveRequest request = new ToolSaveRequest();
        request.setToolName("测试工具");
        request.setRoutePath("/test/test");

        OshUser operator = new OshUser();
        operator.setUsername("admin");

        when(oshToolMapper.insertTool(any(OshTool.class))).thenAnswer(invocation -> {
            OshTool tool = invocation.getArgument(0);
            tool.setId(10003L);
            return 1;
        });
        when(resourceNoGenerator.generateUniqueNo(eq(ResourceCodePrefixEnum.TOOL), any())).thenReturn("tlGh56Ij");

        Long toolId = toolService.createTool(request, operator);

        assertEquals(Long.valueOf(10003L), toolId);
        verify(oshToolMapper).insertTool(argThat(tool ->
                Integer.valueOf(1).equals(tool.getAccessType())
                        && "/test/test".equals(tool.getRoutePath())
                        && tool.getIframeUrl() == null
                        && "tlGh56Ij".equals(tool.getNo())
                        && Integer.valueOf(0).equals(tool.getPointCost())
                        && Integer.valueOf(1).equals(tool.getLevel())
                        && Integer.valueOf(2).equals(tool.getStatus())
        ));
        verify(oshToolEsService).buildIndexMessage(eq(10003L), eq(ToolIndexEventType.TOOL_INDEX_CREATE));
    }

    @Test
    public void shouldMapLevelFromResourceTypeWhenCreatingTool() {
        assertResourceTypeLevel("FREE", 1, 11001L, "tlFree01");
        assertResourceTypeLevel("CASH_ONLY", 2, 11002L, "tlCash02");
        assertResourceTypeLevel("CASH_POINT", 3, 11003L, "tlPoint03");
        assertResourceTypeLevel("VIP", 4, 11004L, "tlVip004");
        assertResourceTypeLevel("SMALL_CLASS", 5, 11005L, "tlSmall5");
        assertResourceTypeLevel("INTERNAL", 6, 11006L, "tlInner6");
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectCreateToolWhenTagCountExceedsLimit() {
        ToolSaveRequest request = new ToolSaveRequest();
        request.setToolName("测试工具");
        request.setRoutePath("/test/test");
        request.setTags(Arrays.asList("标签一", "标签二", "标签三", "标签四"));

        OshUser operator = new OshUser();
        operator.setUsername("admin");

        toolService.createTool(request, operator);
    }

    @Test
    public void shouldReturnToolDetailWithoutConvertingLogoUrl() {
        OshTool tool = new OshTool();
        tool.setId(10001L);
        tool.setLogoUrl("common/image/tool/logo.png");

        when(oshToolMapper.selectToolById(10001L)).thenReturn(tool);
        when(oshToolTagMapper.selectTagNamesByToolId(10001L)).thenReturn(Collections.singletonList("PDF工具"));
        when(oshToolCollectionMapper.selectActiveToolIdsByUserIdAndToolIds(9L, Collections.singletonList(10001L)))
                .thenReturn(Collections.singletonList(10001L));
        OshToolPackage toolPackage = new OshToolPackage();
        toolPackage.setId(1L);
        toolPackage.setToolId(10001L);
        toolPackage.setPackageName("10次体验包");
        when(oshToolPackageMapper.selectPackagesByToolId(10001L)).thenReturn(Collections.singletonList(toolPackage));
        when(oshToolMapper.selectUserRemainingCount(10001L, 9L)).thenReturn(8);
        OshTool result = toolService.getToolDetail(10001L, 9L);

        assertEquals("common/image/tool/logo.png", result.getLogoUrl());
        assertEquals(Integer.valueOf(1), result.getCollectionFlag());
        assertEquals(Integer.valueOf(8), result.getRemainingCount());
        assertEquals(Integer.valueOf(1), result.getPurchasedFlag());
        assertEquals("10次体验包", result.getPackages().get(0).getPackageName());
        assertEquals(Collections.singletonList("PDF工具"), result.getTags());
    }

    @Test
    public void shouldConsumeUsageWhenToolIsPaidResourceType() {
        OshTool tool = new OshTool();
        tool.setId(10001L);
        tool.setResourceType("CASH_ONLY");
        tool.setLevel(1);

        when(oshToolMapper.selectToolById(10001L)).thenReturn(tool);
        when(oshToolMapper.consumeUserToolQuota(10001L, 9L, "normal")).thenReturn(1);
        when(oshToolMapper.selectUserRemainingCount(10001L, 9L)).thenReturn(8, 7);

        Integer remainingCount = toolService.consumeToolUsage(9L, 1, "normal", 10001L);

        assertEquals(Integer.valueOf(7), remainingCount);
        verify(oshToolMapper).consumeUserToolQuota(10001L, 9L, "normal");
        verify(oshToolMapper).increaseTotalUsage(10001L);
        verify(oshToolEsService).buildIndexMessage(eq(10001L), eq(ToolIndexEventType.TOOL_INDEX_COUNTER));
    }

    @Test
    public void shouldSkipConsumeUsageWhenToolIsFreeResourceType() {
        OshTool tool = new OshTool();
        tool.setId(10001L);
        tool.setResourceType("FREE");
        tool.setLevel(1);

        when(oshToolMapper.selectToolById(10001L)).thenReturn(tool);

        Integer remainingCount = toolService.consumeToolUsage(9L, 1, "normal", 10001L);

        assertEquals(Integer.valueOf(0), remainingCount);
        verify(oshToolMapper, times(0)).consumeUserToolQuota(any(Long.class), any(Long.class), any(String.class));
    }

    @Test(expected = ServiceException.class)
    public void shouldRejectConsumeUsageBeforeDeductingWhenPaidToolHasNoRemainingCount() {
        OshTool tool = new OshTool();
        tool.setId(10001L);
        tool.setResourceType("CASH_ONLY");
        tool.setLevel(1);

        when(oshToolMapper.selectToolById(10001L)).thenReturn(tool);
        when(oshToolMapper.selectUserRemainingCount(10001L, 9L)).thenReturn(0);

        try {
            toolService.consumeToolUsage(9L, 1, "normal", 10001L);
        } finally {
            verify(oshToolMapper, never()).consumeUserToolQuota(any(Long.class), any(Long.class), any(String.class));
            verify(oshToolMapper, never()).increaseTotalUsage(any(Long.class));
        }
    }

    @Test
    public void shouldReturnToolUsagePermissionWithQuotaStateForPaidTool() {
        OshTool tool = new OshTool();
        tool.setId(10001L);
        tool.setResourceType("CASH_ONLY");
        tool.setLevel(1);

        when(oshToolMapper.selectToolById(10001L)).thenReturn(tool);
        when(oshToolMapper.selectUserRemainingCount(10001L, 9L)).thenReturn(0);

        ToolUsagePermission permission = toolService.checkToolUsagePermission(9L, 1, 10001L);

        assertEquals(Boolean.TRUE, permission.getUseAllowed());
        assertEquals(Boolean.FALSE, permission.getDeductAllowed());
        assertEquals(Integer.valueOf(0), permission.getRemainingCount());
        assertEquals("工具使用次数不足", permission.getMessage());
    }

    @Test
    public void shouldReturnForbiddenPermissionWhenUserLevelDoesNotMeetRequiredLevel() {
        OshTool tool = new OshTool();
        tool.setId(10001L);
        tool.setResourceType("FREE");
        tool.setLevel(4);

        when(oshToolMapper.selectToolById(10001L)).thenReturn(tool);

        ToolUsagePermission permission = toolService.checkToolUsagePermission(9L, 3, 10001L);

        assertEquals(Boolean.FALSE, permission.getUseAllowed());
        assertEquals(Boolean.FALSE, permission.getDeductAllowed());
        assertEquals(Integer.valueOf(0), permission.getRemainingCount());
        assertEquals("用户权限不足", permission.getMessage());
    }

    @Test
    public void shouldIncreaseViewCountWhenRecordingToolView() {
        when(oshToolMapper.increaseViewCount(10001L)).thenReturn(1);

        toolService.recordToolView(10001L);

        verify(oshToolMapper).increaseViewCount(10001L);
        verify(oshToolEsService).buildIndexMessage(eq(10001L), eq(ToolIndexEventType.TOOL_INDEX_COUNTER));
    }

    @Test
    public void shouldFillMissingToolNoAndReturnUpdatedCount() {
        OshTool first = new OshTool();
        first.setId(10001L);
        OshTool second = new OshTool();
        second.setId(10002L);
        when(oshToolMapper.selectToolsWithMissingNo()).thenReturn(Arrays.asList(first, second));
        when(resourceNoGenerator.generateUniqueNo(eq(ResourceCodePrefixEnum.TOOL), any()))
                .thenReturn("tlAa11Bb", "tlCc22Dd");
        when(oshToolMapper.updateToolNoById(eq(10001L), any(String.class))).thenReturn(1);
        when(oshToolMapper.updateToolNoById(eq(10002L), any(String.class))).thenReturn(1);

        int updatedCount = toolService.fillMissingToolNo();

        assertEquals(2, updatedCount);
        verify(oshToolMapper).selectToolsWithMissingNo();
        verify(oshToolMapper).updateToolNoById(eq(10001L), eq("tlAa11Bb"));
        verify(oshToolMapper).updateToolNoById(eq(10002L), eq("tlCc22Dd"));
    }

    private void assertResourceTypeLevel(String resourceType, Integer expectedLevel, Long toolId, String no) {
        clearInvocations(oshToolMapper, resourceNoGenerator, oshToolEsService);
        final OshTool[] capturedTool = new OshTool[1];

        ToolSaveRequest request = new ToolSaveRequest();
        request.setToolName("测试工具-" + resourceType);
        request.setRoutePath("/test/" + resourceType.toLowerCase());
        request.setResourceType(resourceType);

        OshUser operator = new OshUser();
        operator.setUsername("admin");

        when(oshToolMapper.insertTool(any(OshTool.class))).thenAnswer(invocation -> {
            OshTool tool = invocation.getArgument(0);
            capturedTool[0] = tool;
            tool.setId(toolId);
            return 1;
        });
        when(resourceNoGenerator.generateUniqueNo(eq(ResourceCodePrefixEnum.TOOL), any())).thenReturn(no);

        Long createdId = toolService.createTool(request, operator);

        assertEquals(toolId, createdId);
        assertEquals(resourceType, capturedTool[0].getResourceType());
        assertEquals(expectedLevel, capturedTool[0].getLevel());
    }
}
