package com.backstage.system.service.tool;

import com.backstage.system.domain.tool.OshTool;
import com.backstage.system.mapper.tool.OshToolCollectionMapper;
import com.backstage.system.mapper.tool.OshToolEsMapper;
import com.backstage.system.mapper.tool.OshToolMapper;
import com.backstage.system.mapper.tool.OshToolTagMapper;
import com.backstage.system.service.impl.tool.OshToolEsServiceImpl;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class OshToolEsServiceImplTest {

    @InjectMocks
    private OshToolEsServiceImpl oshToolEsService;

    @Mock
    private OshToolEsMapper oshToolEsMapper;

    @Mock
    private OshToolMapper oshToolMapper;

    @Mock
    private OshToolTagMapper oshToolTagMapper;

    @Mock
    private OshToolCollectionMapper oshToolCollectionMapper;

    @Test
    public void shouldSyncToolNoWhenSyncingAllToolsToEs() throws Exception {
        OshTool tool = new OshTool();
        tool.setId(10001L);
        tool.setToolName("图片转PDF");
        tool.setNo("tlAb12Cd");
        tool.setStatus(4);
        tool.setDeleteFlag(0);

        when(oshToolMapper.selectAllToolsForEsSync()).thenReturn(Collections.singletonList(tool), Collections.emptyList());
        when(oshToolTagMapper.selectTagNamesByToolId(10001L)).thenReturn(Collections.singletonList("PDF工具"));
        when(oshToolTagMapper.selectTagIdsByToolId(10001L)).thenReturn(Collections.singletonList(1L));
        when(oshToolEsMapper.bulkUpsertTools(anyList())).thenReturn(1);

        int count = oshToolEsService.syncAllToolsToEs();

        ArgumentCaptor<java.util.List> captor = ArgumentCaptor.forClass(java.util.List.class);
        verify(oshToolEsMapper).bulkUpsertTools(captor.capture());
        ToolIndexMessage message = (ToolIndexMessage) captor.getValue().get(0);
        assertEquals(1, count);
        assertEquals("tlAb12Cd", message.getNo());
        assertEquals("图片转PDF", message.getToolName());
        assertEquals(ToolIndexEventType.TOOL_INDEX_UPDATE, message.getEventType());
    }
}
