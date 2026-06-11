package com.backstage.web.controller.tool;

import com.backstage.common.constant.OshUserConstants;
import com.backstage.common.threadlocal.ThreadLocalUtil;
import com.backstage.system.controller.tool.OshToolController;
import com.backstage.system.domain.user.OshUser;
import com.backstage.system.domain.tool.OshToolTag;
import com.backstage.system.domain.vo.tool.ToolQuotaCurrentVO;
import com.backstage.system.service.tool.IOshToolCollectionService;
import com.backstage.system.service.tool.IOshToolEsService;
import com.backstage.system.service.tool.IOshToolService;
import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.util.Collections;

public class OshToolControllerEsSyncTest {

    private MockMvc mockMvc;
    private IOshToolEsService oshToolEsService;

    @Before
    public void setUp() {
        OshToolController controller = new OshToolController();
        ReflectionTestUtils.setField(controller, "oshToolService", mock(IOshToolService.class));
        ReflectionTestUtils.setField(controller, "oshToolCollectionService", mock(IOshToolCollectionService.class));
        oshToolEsService = mock(IOshToolEsService.class);
        ReflectionTestUtils.setField(controller, "oshToolEsService", oshToolEsService);

        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @After
    public void tearDown() {
        ThreadLocalUtil.remove();
    }

    @Test
    public void shouldSyncAllToolsToEs() throws Exception {
        when(oshToolEsService.syncAllToolsToEs()).thenReturn(15);

        mockMvc.perform(post("/pc/tool/esSync/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(15));

        verify(oshToolEsService).syncAllToolsToEs();
    }

    @Test
    public void shouldFillMissingToolNo() throws Exception {
        IOshToolService oshToolService = mock(IOshToolService.class);
        OshToolController controller = new OshToolController();
        ReflectionTestUtils.setField(controller, "oshToolService", oshToolService);
        ReflectionTestUtils.setField(controller, "oshToolCollectionService", mock(IOshToolCollectionService.class));
        ReflectionTestUtils.setField(controller, "oshToolEsService", mock(IOshToolEsService.class));
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        when(oshToolService.fillMissingToolNo()).thenReturn(7);

        mockMvc.perform(post("/pc/tool/fill/no"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(7));

        verify(oshToolService).fillMissingToolNo();
    }

    @Test
    public void shouldInitMissingUserToolQuota() throws Exception {
        IOshToolService oshToolService = mock(IOshToolService.class);
        OshToolController controller = new OshToolController();
        ReflectionTestUtils.setField(controller, "oshToolService", oshToolService);
        ReflectionTestUtils.setField(controller, "oshToolCollectionService", mock(IOshToolCollectionService.class));
        ReflectionTestUtils.setField(controller, "oshToolEsService", mock(IOshToolEsService.class));
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        OshUser user = new OshUser();
        user.setId(1L);
        user.setUsername("admin");
        ThreadLocalUtil.set(OshUserConstants.USER_INFO, user);

        when(oshToolService.initMissingUserToolQuota("admin")).thenReturn(3);

        mockMvc.perform(post("/pc/tool/quota/init"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(3));

        verify(oshToolService).initMissingUserToolQuota("admin");
    }

    @Test
    public void shouldGetCurrentUserToolQuota() throws Exception {
        IOshToolService oshToolService = mock(IOshToolService.class);
        OshToolController controller = new OshToolController();
        ReflectionTestUtils.setField(controller, "oshToolService", oshToolService);
        ReflectionTestUtils.setField(controller, "oshToolCollectionService", mock(IOshToolCollectionService.class));
        ReflectionTestUtils.setField(controller, "oshToolEsService", mock(IOshToolEsService.class));
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        OshUser user = new OshUser();
        user.setId(1L);
        user.setUsername("admin");
        ThreadLocalUtil.set(OshUserConstants.USER_INFO, user);

        ToolQuotaCurrentVO quota = new ToolQuotaCurrentVO();
        quota.setRemainingCount(10);
        quota.setTotalBuyCount(20);
        quota.setUsedCount(10);

        when(oshToolService.getCurrentUserToolQuota(1L)).thenReturn(quota);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/pc/tool/quota/current"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.remainingCount").value(10))
                .andExpect(jsonPath("$.data.totalBuyCount").value(20))
                .andExpect(jsonPath("$.data.usedCount").value(10));

        verify(oshToolService).getCurrentUserToolQuota(1L);
    }

    @Test
    public void shouldListTagsByKeyword() throws Exception {
        IOshToolService oshToolService = mock(IOshToolService.class);
        OshToolController controller = new OshToolController();
        ReflectionTestUtils.setField(controller, "oshToolService", oshToolService);
        ReflectionTestUtils.setField(controller, "oshToolCollectionService", mock(IOshToolCollectionService.class));
        ReflectionTestUtils.setField(controller, "oshToolEsService", mock(IOshToolEsService.class));
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        OshToolTag tag = new OshToolTag();
        tag.setId(1L);
        tag.setName("PDF工具");
        when(oshToolService.listAvailableTags("PDF")).thenReturn(Collections.singletonList(tag));

        mockMvc.perform(get("/pc/tool/tags").param("keyword", "PDF"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].name").value("PDF工具"));

        verify(oshToolService).listAvailableTags("PDF");
    }
}
