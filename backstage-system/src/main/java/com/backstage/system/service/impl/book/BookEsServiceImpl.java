package com.backstage.system.service.impl.book;

import com.backstage.common.utils.StringUtils;
import com.backstage.system.controller.book.BookListReqVO;
import com.backstage.system.domain.BookChapter;
import com.backstage.system.domain.UserBookRelation;
import com.backstage.system.domain.book.BookDO;
import com.backstage.system.domain.book.es.OshBookEsDocument;
import com.backstage.system.domain.vo.book.BookListVO;
import com.backstage.system.mapper.book.BookEsMapper;
import com.backstage.system.mapper.book.BookChapterMapper;
import com.backstage.system.mapper.book.BookMapper;
import com.backstage.system.mapper.book.BookTagDOMapper;
import com.backstage.system.mapper.book.UserBookRelationMapper;
import com.backstage.system.service.book.IBookEsService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class BookEsServiceImpl implements IBookEsService {

    private static final Logger log = LoggerFactory.getLogger(BookEsServiceImpl.class);

    @Autowired
    private BookEsMapper bookEsMapper;

    @Autowired
    private BookMapper bookMapper;

    @Autowired
    private BookChapterMapper bookChapterMapper;

    @Autowired
    private BookTagDOMapper bookTagDOMapper;

    @Autowired
    private UserBookRelationMapper userBookRelationMapper;

    @Autowired
    private com.backstage.system.service.common.OssService ossService;

    @Override
    public Page<BookListVO> searchBooks(BookListReqVO request) {
        BookEsMapper.BookEsSearchResult esResult;
        try {
            esResult = bookEsMapper.searchBooks(request);
        } catch (Exception ex) {
            log.error("search books from es failed, request={}", request, ex);
            throw new IllegalStateException("search books from es failed", ex);
        }

        List<BookListVO> rows = esResult.getRows();
        if (StringUtils.isEmpty(rows)) {
            Page<BookListVO> emptyPage = new Page<>(esResult.getPageNum(), esResult.getPageSize(), 0);
            emptyPage.setRecords(Collections.emptyList());
            return emptyPage;
        }

        for (BookListVO vo : rows) {
            if (StringUtils.isNotEmpty(vo.getCover())) {
                vo.setCover(ossService.getLimitedUrl(vo.getCover(), 30));
            }
        }
        fillUserRelationStates(rows, request == null ? null : request.getUserId());

        Page<BookListVO> page = new Page<>(esResult.getPageNum(), esResult.getPageSize(), esResult.getTotal());
        page.setRecords(rows);
        return page;
    }

    /**
     * 为 ES 搜索结果补充当前用户的收藏与购买状态。
     *
     * @param rows 电子书列表
     * @param userId 当前用户ID
     */
    private void fillUserRelationStates(List<BookListVO> rows, Long userId) {
        if (userId == null) {
            rows.forEach(row -> {
                row.setFavorited(0);
                row.setPurchased(0);
            });
            return;
        }

        List<Long> bookIds = rows.stream().map(BookListVO::getId).collect(Collectors.toList());
        List<UserBookRelation> relations = userBookRelationMapper.selectList(
                new LambdaQueryWrapper<UserBookRelation>()
                        .eq(UserBookRelation::getUserId, userId)
                        .in(UserBookRelation::getBookId, bookIds)
        );
        Map<Long, UserBookRelation> relationMap = new HashMap<>();
        for (UserBookRelation relation : relations) {
            relationMap.put(relation.getBookId(), relation);
        }
        for (BookListVO row : rows) {
            UserBookRelation relation = relationMap.get(row.getId());
            row.setFavorited(relation == null ? 0 : Optional.ofNullable(relation.getFavorited()).orElse(0));
            row.setPurchased(relation == null ? 0 : Optional.ofNullable(relation.getPurchased()).orElse(0));
        }
    }

    @Override
    public int syncAllBooksToEs() {
        int pageNum = 1;
        int pageSize = 200;
        int total = 0;

        try {
            bookEsMapper.deleteAllBooks();
        } catch (Exception ex) {
            throw new IllegalStateException("clear books in es failed", ex);
        }

        while (true) {
            Page<BookDO> pageParam = new Page<>(pageNum, pageSize);
            LambdaQueryWrapper<BookDO> wrapper = new LambdaQueryWrapper<BookDO>()
                    .eq(BookDO::getStatus, "0")
                    .orderByDesc(BookDO::getCreateTime);
            Page<BookDO> pageResult = bookMapper.selectPage(pageParam, wrapper);
            List<BookDO> rows = pageResult.getRecords();
            if (StringUtils.isEmpty(rows)) {
                break;
            }

            List<OshBookEsDocument> documents = new ArrayList<>(rows.size());
            for (BookDO row : rows) {
                documents.add(buildEsDocumentFromDO(row));
            }

            try {
                total += bookEsMapper.bulkUpsertBooks(documents);
            } catch (Exception ex) {
                throw new IllegalStateException("sync books to es failed", ex);
            }

            if (rows.size() < pageSize) {
                break;
            }
            pageNum++;
        }

        return total;
    }

    private OshBookEsDocument buildEsDocumentFromDO(BookDO bookDO) {
        OshBookEsDocument document = new OshBookEsDocument();
        document.setId(bookDO.getId());
        document.setTitle(bookDO.getTitle());
        document.setDescription(bookDO.getDescription());
        document.setCover(bookDO.getCover());
        document.setPrice(bookDO.getPrice() != null ? bookDO.getPrice() : java.math.BigDecimal.ZERO);
        document.setOriginalPrice(bookDO.getOriginalPrice() != null ? bookDO.getOriginalPrice() : java.math.BigDecimal.ZERO);
        document.setSubCount(bookDO.getSubCount());
        document.setChapterCount(Math.toIntExact(bookChapterMapper.selectCount(
                new LambdaQueryWrapper<BookChapter>().eq(BookChapter::getBookId, bookDO.getId())
        )));
        document.setLevel(bookDO.getLevel());
        document.setStatus(bookDO.getStatus());
        document.setDeleteFlag(0);
        document.setRemark(bookDO.getRemark());
        document.setCreateBy(bookDO.getCreateBy());
        document.setUpdateBy(bookDO.getUpdateBy());
        document.setCreateTime(bookDO.getCreateTime());
        document.setUpdateTime(bookDO.getUpdateTime());

        List<String> tagNames = extractTagNames(bookDO.getId());
        document.setTagNames(tagNames);
        String tagText = String.join(" ", tagNames);
        document.setTagNamesText(tagText);
        document.setSearchText(buildSearchText(bookDO.getTitle(), bookDO.getDescription(), tagText));
        return document;
    }

    @Override
    public void syncBookToEs(Long bookId) {
        try {
            BookDO bookDO = bookMapper.selectById(bookId);
            if (bookDO == null) {
                return;
            }
            bookEsMapper.upsertBook(buildEsDocumentFromDO(bookDO));
        } catch (Exception ex) {
            log.warn("sync book to es failed, bookId={}", bookId, ex);
        }
    }

    @Override
    public void deleteBookFromEs(Long bookId) {
        try {
            bookEsMapper.deleteBook(bookId);
        } catch (Exception ex) {
            log.warn("delete book from es failed, bookId={}", bookId, ex);
        }
    }

    private List<String> extractTagNames(Long bookId) {
        List<String> tags = bookTagDOMapper.selectBookTagListByBookId(bookId);
        if (StringUtils.isEmpty(tags)) {
            return Collections.emptyList();
        }
        return tags;
    }

    private String buildSearchText(String title, String description, String tagText) {
        StringBuilder builder = new StringBuilder();
        appendSearchField(builder, title);
        appendSearchField(builder, description);
        appendSearchField(builder, tagText);
        return builder.toString().trim();
    }

    private void appendSearchField(StringBuilder builder, String value) {
        if (StringUtils.isEmpty(value)) {
            return;
        }
        if (builder.length() > 0) {
            builder.append(' ');
        }
        builder.append(value.trim());
    }
}
