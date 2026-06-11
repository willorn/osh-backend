package com.backstage.system.controller.book;

import com.backstage.common.constant.OshUserConstants;
import com.backstage.common.threadlocal.ThreadLocalUtil;
import com.backstage.system.service.book.IBookEsService;
import com.backstage.system.service.book.IBookService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BookSearchUserStateTest {

    @AfterEach
    void tearDown() {
        ThreadLocalUtil.remove();
    }

    @Test
    void shouldPassCurrentUserIdToAnonymousBookSearch() throws Exception {
        IBookService bookService = Mockito.mock(IBookService.class);
        BookController controller = new BookController();
        ReflectionTestUtils.setField(controller, "bookService", bookService);
        ReflectionTestUtils.setField(controller, "bookEsService", Mockito.mock(IBookEsService.class));

        BookListReqVO request = new BookListReqVO();
        when(bookService.getBookPageList(same(request))).thenReturn(new Page<>());
        ThreadLocalUtil.set(OshUserConstants.USER_ID, 9L);

        controller.search(request);

        Field userIdField = BookListReqVO.class.getDeclaredField("userId");
        userIdField.setAccessible(true);
        assertEquals(9L, userIdField.get(request));
        verify(bookService).getBookPageList(request);
    }

    @Test
    void shouldKeepAnonymousBookSearchAvailableWithoutUserId() throws Exception {
        IBookService bookService = Mockito.mock(IBookService.class);
        BookController controller = new BookController();
        ReflectionTestUtils.setField(controller, "bookService", bookService);
        ReflectionTestUtils.setField(controller, "bookEsService", Mockito.mock(IBookEsService.class));

        BookListReqVO request = new BookListReqVO();
        when(bookService.getBookPageList(same(request))).thenReturn(new Page<>());

        controller.search(request);

        Field userIdField = BookListReqVO.class.getDeclaredField("userId");
        userIdField.setAccessible(true);
        assertNull(userIdField.get(request));
        verify(bookService).getBookPageList(request);
    }
}
