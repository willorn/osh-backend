package com.backstage.system.controller.book;

import com.backstage.common.annotation.OshUserEvent;
import com.backstage.common.annotation.Anonymous;
import com.backstage.common.core.domain.R;
import com.backstage.common.response.PageResponse;
import com.backstage.system.config.properties.SearchEsProperties;
import com.backstage.system.domain.vo.book.*;
import com.backstage.system.service.book.IBookEsService;
import com.backstage.system.service.book.IBookService;
import com.backstage.system.utils.UserContextUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

/**
 * 电子书Controller
 *
 * @author backstage
 */
@ApiOperation(value = "电子书接口")
@RestController
@RequestMapping("/pc/book")
public class BookController {

    private static final Logger log = LoggerFactory.getLogger(BookController.class);

    @Resource
    private IBookService bookService;

    @Autowired
    private IBookEsService bookEsService;

    @Autowired
    private SearchEsProperties searchEsProperties;

    /**
     * 电子书列表
     */
    @ApiOperation(value = "电子书列表")
    @OshUserEvent(module = "电子书模块", actionType = "查询", description = "查询电子书列表")
    @PreAuthorize("hasAuthority('book:list')")
    @PostMapping("/page")
    public R<Page<BookListVO>> list(@RequestBody BookListReqVO reqVO) {
        reqVO.setUserLevel(UserContextUtil.getCurrentLevel());
        reqVO.setUserId(UserContextUtil.getCurrentUserIdSafely());
        if (searchEsProperties.isEnabled()) {
            try {
                log.info("使用es查询电子书");
                return R.ok(bookEsService.searchBooks(reqVO));
            } catch (Exception ex) {
                log.warn("book search fallback to mysql after es failure, reqVO={}", reqVO, ex);
            }
        }
        Page<BookListVO> pageResult = bookService.getBookPageList(reqVO);
        return R.ok(pageResult);
    }

    @ApiOperation(value = "电子书搜索")
    @PostMapping("/search")
    @Anonymous
    public R<PageResponse<BookListVO>> search(@RequestBody BookListReqVO reqVO) {
        reqVO.setUserId(UserContextUtil.getCurrentUserIdSafely());
        Page<BookListVO> pageResult = bookService.getBookPageList(reqVO);
        int pageNum = reqVO.getPageNum() == null ? 1 : reqVO.getPageNum().intValue();
        int pageSize = reqVO.getPageSize() == null ? 12 : reqVO.getPageSize().intValue();
        return R.ok(PageResponse.of(pageResult.getRecords(), pageResult.getTotal(), pageNum, pageSize), "ok");
    }

    /**
     * 查看电子书详情
     */
    @ApiOperation(value = "电子书详情")
//    @OshUserEvent(module = "电子书模块", actionType = "查询", description = "查询电子书详情")
    @Anonymous
    @GetMapping("/getById")
    public R<BookDetailVO> getById(@RequestParam Long id, @RequestParam(required = false, defaultValue = "false") Boolean forEdit) {
        BookDetailVO detail = bookService.selectBookDetail(id, forEdit);
        return R.ok(detail);
    }

    /**
     * 查看电子书章节内容
     */
    @ApiOperation(value = "章节内容详情")
    @OshUserEvent(module = "电子书模块", actionType = "查询", description = "查询章节内容")
    @PreAuthorize("hasAuthority('book:chapter:detail')")
    @GetMapping("/detail")
    public R<BookChapterContentVO> detail(@RequestParam Long book_id, @RequestParam Long id) {
        BookChapterContentVO content = bookService.selectBookChapterContent(book_id, id);
        return R.ok(content);
    }

    /**
     * 查看电子书章节菜单
     */
    @ApiOperation(value = "章节菜单")
    @OshUserEvent(module = "电子书模块", actionType = "查询", description = "查询章节菜单")
    @Anonymous
    @GetMapping("/menus")
    public R<BookMenuVO> menus(@RequestParam Long id) {
        BookMenuVO menu = bookService.selectBookMenu(id);
        return R.ok(menu);
    }


    /**
     * 新增电子书
     */
    @ApiOperation(value = "新增电子书")
    @OshUserEvent(module = "电子书模块", actionType = "新增", description = "创建电子书")
    @PreAuthorize("hasAuthority('book:create')")
    @PostMapping("/create")
    public R<Long> create(@Valid @RequestBody BookSaveReqVO reqVO) {
        return R.ok(bookService.createBook(reqVO), "创建成功");
    }

    /**
     * 修改电子书
     */
    @ApiOperation(value = "修改电子书")
    @OshUserEvent(module = "电子书模块", actionType = "修改", description = "更新电子书")
    @PreAuthorize("hasAuthority('book:update')")
    @PostMapping("/update")
    public R<String> update(@Valid @RequestBody BookSaveReqVO reqVO) {
        if (reqVO.getId() == null)
        {
            return R.fail("电子书ID不能为空");
        }
        bookService.updateBook(reqVO);
        return R.ok( "修改成功");
    }

    /**
     * 删除电子书
     */
    @ApiOperation(value = "删除电子书")
    @OshUserEvent(module = "电子书模块", actionType = "删除", description = "删除电子书")
    @PreAuthorize("hasAuthority('book:delete')")
    @DeleteMapping("/delete")
    public R<String> delete(@RequestParam("id") Long id)
    {
        bookService.deleteBook(id);
        return R.ok("删除成功");
    }

    /**
     * 查询所有电子书标签列表
     */
    @ApiOperation(value = "标签列表")
    @OshUserEvent(module = "电子书模块", actionType = "查询", description = "查询标签列表")
    @Anonymous
    @GetMapping("/getTagList")
    public R<List<String>> getTagList() {
        return R.ok(bookService.getTagList());
    }

    /**
     * 新增电子书章节
     */
    @ApiOperation(value = "新增章节")
    @OshUserEvent(module = "电子书模块", actionType = "新增", description = "创建章节")
    @PreAuthorize("hasAuthority('book:chapter:create')")
    @PostMapping("/chapter/create")
    public R<String> createBookChapter(@RequestBody BookChapterSaveUpdateVO reqVO) {
        if (reqVO.getBookId() == null) {
            return R.fail("电子书ID不能为空");
        }
        bookService.createBookChapter(reqVO);
        return R.ok("创建成功");
    }

    /**
     * 修改电子书章节
     */
    @ApiOperation(value = "修改章节")
    @OshUserEvent(module = "电子书模块", actionType = "修改", description = "更新章节")
    @PreAuthorize("hasAuthority('book:chapter:update')")
    @PostMapping("/chapter/update")
    public R<String> updateBookChapter(@RequestBody BookChapterSaveUpdateVO reqVO) {
        if (reqVO.getId() == null) {
            return R.fail("章节ID不能为空");
        }
        if (reqVO.getBookId() == null) {
            return R.fail("电子书ID不能为空");
        }
        bookService.updateBookChapter(reqVO);
        return R.ok("修改成功");
    }

    /**
     * 筛选电子书列表
     */
    @ApiOperation(value = "筛选电子书")
    @OshUserEvent(module = "电子书模块", actionType = "查询", description = "筛选电子书")
    @PreAuthorize("hasAuthority('book:filter')")
    @GetMapping("/getFilterBookList")
    public R<Page<BookListVO>> getFilterBookList(@RequestParam String filter) {
        Page<BookListVO> bookList = bookService.getFilterBookList(filter);
        return R.ok(bookList);
    }

    @ApiOperation(value = "全量同步电子书到ES")
    @PostMapping("/esSync/all")
    public R<Integer> syncAllBooksToEs() {
        return R.ok(bookEsService.syncAllBooksToEs(), "ok");
    }
}
