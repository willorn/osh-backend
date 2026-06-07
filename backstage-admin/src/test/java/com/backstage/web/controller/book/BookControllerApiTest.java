package com.backstage.web.controller.book;

import com.backstage.RuoYiApplication;
import com.backstage.common.constant.OshUserConstants;
import com.backstage.common.threadlocal.ThreadLocalUtil;
import com.backstage.system.domain.book.BookDO;
import com.backstage.system.domain.book.BookTagDO;
import com.backstage.system.domain.questionanswer.Question;
import com.backstage.system.domain.user.OshUser;
import com.backstage.system.domain.vo.book.BookChapterVO;
import com.backstage.system.mapper.book.BookChapterMapper;
import com.backstage.system.mapper.book.BookMapper;
import com.backstage.system.mapper.book.BookTagDOMapper;
import com.backstage.system.mapper.questionanswer.OshQAQuestionMapper;
import com.backstage.system.service.ISysConfigService;
import com.backstage.system.service.ISysDictTypeService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = RuoYiApplication.class)
@AutoConfigureMockMvc(addFilters = false)
public class BookControllerApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BookMapper bookMapper;

    @Autowired
    private BookChapterMapper bookChapterMapper;

    @Autowired
    private BookTagDOMapper bookTagDOMapper;

    @Autowired
    private OshQAQuestionMapper oshQaQuestionMapper;

    @MockBean
    private ISysConfigService sysConfigService;

    @MockBean
    private ISysDictTypeService sysDictTypeService;

    @MockBean
    private KafkaTemplate<String, String> kafkaTemplate;

    @BeforeEach
    void setUp() {
        Mockito.when(sysConfigService.selectCaptchaEnabled()).thenReturn(false);
    }

    @AfterEach
    void tearDown() {
        ThreadLocalUtil.remove();
        SecurityContextHolder.clearContext();
    }

    @Test
    public void shouldCreateUpdateAndDeleteBookWithLevelTagsAndChapters() throws Exception {
        String suffix = String.valueOf(System.currentTimeMillis());
        String createPayload = "{"
                + "\"title\":\"自动化电子书-" + suffix + "\","
                + "\"cover\":\"common/image/book/test-cover-" + suffix + ".png\","
                + "\"desc\":\"创建电子书接口测试\","
                + "\"price\":29.90,"
                + "\"t_price\":59.90,"
                + "\"level\":4,"
                + "\"tags\":[\"自动化\",\"电子书\"],"
                + "\"chapters\":["
                + "{\"title\":\"第一章-" + suffix + "\",\"content\":\"<h2>第一章</h2><p>测试内容</p>\",\"chapterNo\":1,\"sortOrder\":1,\"isFree\":1},"
                + "{\"title\":\"第二章-" + suffix + "\",\"content\":\"<h2>第二章</h2><p>更多内容</p>\",\"chapterNo\":2,\"sortOrder\":2,\"isFree\":0}"
                + "]"
                + "}";

        MvcResult createResult = performAsUser(buildCurrentUser(), post("/pc/book/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();

        Long bookId = extractDataId(createResult);
        BookDO created = bookMapper.selectById(bookId);
        assertNotNull(created);
        assertEquals("自动化电子书-" + suffix, created.getTitle());
        assertEquals(Integer.valueOf(4), created.getLevel());
        assertEquals(new BigDecimal("59.90"), created.getOriginalPrice());

        List<BookChapterVO> createdChapters = bookChapterMapper.selectBookChapterListByBookId(bookId);
        assertEquals(2, createdChapters.size());
        assertEquals("第一章-" + suffix, createdChapters.get(0).getTitle());

        List<BookTagDO> createdTags = bookTagDOMapper.selectList(new LambdaQueryWrapper<BookTagDO>()
                .eq(BookTagDO::getBookId, bookId));
        assertEquals(2, createdTags.size());

        Long firstChapterId = createdChapters.get(0).getId();
        String updatePayload = "{"
                + "\"id\":" + bookId + ","
                + "\"title\":\"自动化电子书-更新-" + suffix + "\","
                + "\"cover\":\"common/image/book/test-cover-" + suffix + ".png\","
                + "\"desc\":\"更新电子书接口测试\","
                + "\"price\":19.90,"
                + "\"t_price\":39.90,"
                + "\"level\":5,"
                + "\"tags\":[\"更新后标签\"],"
                + "\"chapters\":["
                + "{\"id\":" + firstChapterId + ",\"title\":\"重写第一章-" + suffix + "\",\"content\":\"<h2>重写第一章</h2><p>更新内容</p>\",\"chapterNo\":1,\"sortOrder\":1,\"isFree\":1},"
                + "{\"title\":\"新增第三章-" + suffix + "\",\"content\":\"<h2>新增第三章</h2><p>全新内容</p>\",\"chapterNo\":2,\"sortOrder\":2,\"isFree\":0}"
                + "]"
                + "}";

        performAsUser(buildCurrentUser(), post("/pc/book/update")
                .contentType(MediaType.APPLICATION_JSON)
                .content(updatePayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        BookDO updated = bookMapper.selectById(bookId);
        assertNotNull(updated);
        assertEquals("自动化电子书-更新-" + suffix, updated.getTitle());
        assertEquals(Integer.valueOf(5), updated.getLevel());
        assertEquals(new BigDecimal("39.90"), updated.getOriginalPrice());

        List<BookChapterVO> updatedChapters = bookChapterMapper.selectBookChapterListByBookId(bookId);
        assertEquals(2, updatedChapters.size());
        assertTrue(updatedChapters.stream().anyMatch(item -> ("重写第一章-" + suffix).equals(item.getTitle())));
        assertTrue(updatedChapters.stream().anyMatch(item -> ("新增第三章-" + suffix).equals(item.getTitle())));

        List<BookTagDO> updatedTags = bookTagDOMapper.selectList(new LambdaQueryWrapper<BookTagDO>()
                .eq(BookTagDO::getBookId, bookId));
        assertEquals(1, updatedTags.size());
        assertEquals("更新后标签", updatedTags.get(0).getTagName());

        performAsUser(buildCurrentUser(), get("/pc/book/getById")
                .param("id", String.valueOf(bookId))
                .param("forEdit", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.level").value(5))
                .andExpect(jsonPath("$.data.book_details.length()").value(2));

        performAsUser(buildCurrentUser(), delete("/pc/book/delete")
                .param("id", String.valueOf(bookId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        assertNull(bookMapper.selectById(bookId));
    }

    @Test
    public void shouldCreateAndListQuestionsByBookResource() throws Exception {
        Long bookId = createStandaloneBook();
        String suffix = String.valueOf(System.currentTimeMillis());
        String questionContent = "这是电子书自动化提问内容-" + suffix;

        performAsUser(buildCurrentUser(), post("/pc/qna/question/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"resourceType\":\"电子书\",\"resourceNo\":" + bookId + ",\"content\":\"" + questionContent + "\",\"isPaidOnly\":0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        Question question = oshQaQuestionMapper.selectOne(new LambdaQueryWrapper<Question>()
                .eq(Question::getResourceType, "电子书")
                .eq(Question::getResourceNo, bookId)
                .eq(Question::getContent, questionContent)
                .orderByDesc(Question::getId)
                .last("limit 1"));
        assertNotNull(question);
        assertEquals(Long.valueOf(1L), question.getUserId());

        performAsUser(buildCurrentUser(), post("/pc/qna/question/list")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"resourceType\":\"电子书\",\"resourceNo\":" + bookId + ",\"type\":\"all\",\"pageNum\":1,\"pageSize\":10}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.rows[0].resourceType").value("电子书"))
                .andExpect(jsonPath("$.rows[0].resourceNo").value(bookId))
                .andExpect(jsonPath("$.rows[0].content").value(questionContent));
    }

    @Test
    public void shouldReturnAnonymousBookDetailWithIsbuyFlag() throws Exception {
        Long bookId = createStandaloneBook();

        ThreadLocalUtil.remove();
        SecurityContextHolder.clearContext();

        mockMvc.perform(get("/pc/book/getById")
                        .param("id", String.valueOf(bookId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(bookId))
                .andExpect(jsonPath("$.data.isbuy").value(false))
                .andExpect(jsonPath("$.data.level").value(3));
    }

    private Long createStandaloneBook() {
        String suffix = String.valueOf(System.currentTimeMillis());
        BookDO book = new BookDO();
        book.setTitle("问答测试电子书-" + suffix);
        book.setCover("common/image/book/question-" + suffix + ".png");
        book.setDescription("问答测试电子书简介");
        book.setPrice(new BigDecimal("9.90"));
        book.setOriginalPrice(new BigDecimal("19.90"));
        book.setStatus("0");
        book.setLevel(3);
        book.setCreateBy("integration_test_user");
        book.setUpdateBy("integration_test_user");
        bookMapper.insert(book);
        return book.getId();
    }

    private Long extractDataId(MvcResult mvcResult) throws Exception {
        JsonNode jsonNode = objectMapper.readTree(mvcResult.getResponse().getContentAsString());
        return jsonNode.get("data").asLong();
    }

    private OshUser buildCurrentUser() {
        OshUser oshUser = new OshUser();
        oshUser.setId(1L);
        oshUser.setUsername("integration_test_user");
        return oshUser;
    }

    private ResultActions performAsUser(OshUser oshUser, RequestBuilder requestBuilder) throws Exception {
        ThreadLocalUtil.set(OshUserConstants.USER_ID, oshUser.getId());
        ThreadLocalUtil.set(OshUserConstants.USER_INFO, oshUser);
        ThreadLocalUtil.set(OshUserConstants.LEVEL, "5");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        oshUser.getUsername(),
                        "N/A",
                        Arrays.asList(
                                new SimpleGrantedAuthority("book:create"),
                                new SimpleGrantedAuthority("book:update"),
                                new SimpleGrantedAuthority("book:detail"),
                                new SimpleGrantedAuthority("book:delete"),
                                new SimpleGrantedAuthority("qna:question:create"),
                                new SimpleGrantedAuthority("qna:question:list")
                        )
                )
        );
        return mockMvc.perform(requestBuilder);
    }
}
