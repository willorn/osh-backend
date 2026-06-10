package com.backstage.system.service.book;

import com.backstage.common.constant.OshUserConstants;
import com.backstage.common.threadlocal.ThreadLocalUtil;
import com.backstage.system.domain.UserBookRelation;
import com.backstage.system.domain.book.BookDO;
import com.backstage.system.domain.vo.book.BookRelationStatusVO;
import com.backstage.system.mapper.book.UserBookRelationMapper;
import com.backstage.system.service.impl.BookServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BookRelationStorageTest {

    @AfterEach
    void tearDown() {
        ThreadLocalUtil.remove();
    }

    @Test
    void shouldStoreBookFavoriteInUserBookRelation() {
        UserBookRelationMapper relationMapper = Mockito.mock(UserBookRelationMapper.class);
        BookServiceImpl service = Mockito.spy(new BookServiceImpl());
        ReflectionTestUtils.setField(service, "userBookRelationMapper", relationMapper);
        Mockito.doReturn(new BookDO()).when(service).getById(7L);

        UserBookRelation relation = new UserBookRelation();
        relation.setId(11L);
        relation.setUserId(3L);
        relation.setBookId(7L);
        relation.setFavorited(0);
        when(relationMapper.selectByUserIdAndBookId(3L, 7L)).thenReturn(relation);
        ThreadLocalUtil.set(OshUserConstants.USER_ID, 3L);

        service.favoriteBook(7L, 1);

        assertEquals(Integer.valueOf(1), relation.getFavorited());
        assertNotNull(relation.getFavoriteTime());
        verify(relationMapper).updateById(relation);
    }

    @Test
    void shouldReadFavoriteAndPurchaseStatusFromUserBookRelation() {
        UserBookRelationMapper relationMapper = Mockito.mock(UserBookRelationMapper.class);
        BookServiceImpl service = new BookServiceImpl();
        ReflectionTestUtils.setField(service, "userBookRelationMapper", relationMapper);

        UserBookRelation relation = new UserBookRelation();
        relation.setFavorited(1);
        relation.setFollowed(0);
        relation.setPurchased(1);
        relation.setFavoriteTime(LocalDateTime.now());
        when(relationMapper.selectByUserIdAndBookId(3L, 7L)).thenReturn(relation);
        ThreadLocalUtil.set(OshUserConstants.USER_ID, 3L);

        BookRelationStatusVO status = service.getBookRelationStatus(7L);

        assertEquals(Integer.valueOf(1), status.getFavorited());
        assertEquals(Integer.valueOf(1), status.getPurchased());
    }
}
