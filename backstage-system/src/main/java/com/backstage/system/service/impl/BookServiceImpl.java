package com.backstage.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.backstage.common.exception.ServiceException;
import com.backstage.common.async.AsyncExecutorNames;
import com.backstage.common.async.AsyncTaskSupport;
import com.backstage.common.threadlocal.ThreadLocalUtil;
import com.backstage.common.utils.StringUtils;
import com.backstage.system.controller.book.BookListReqVO;
import com.backstage.system.domain.book.BookDO;
import com.backstage.system.domain.BookChapter;
import com.backstage.system.domain.UserBookRelation;
import com.backstage.system.domain.book.BookTagDO;
import com.backstage.system.domain.vo.book.*;
import com.backstage.system.mapper.book.BookChapterMapper;
import com.backstage.system.mapper.book.BookMapper;
import com.backstage.system.mapper.book.UserBookRelationMapper;
import com.backstage.system.mapper.book.BookTagDOMapper;
import com.backstage.system.domain.vo.pay.OrderCheckoutReqVO;
import com.backstage.system.domain.vo.pay.OrderCheckoutRespVO;
import com.backstage.system.domain.order.OshOrder;
import com.backstage.system.domain.order.enums.ProductTypeEnum;
import com.backstage.system.mapper.order.OshOrderMapper;
import com.backstage.system.service.book.BookChapterService;
import com.backstage.system.service.book.IBookService;
import com.backstage.system.service.order.OrderCheckoutService;
import com.backstage.system.utils.UserContextUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

/**
 * 电子书 服务层实现
 *
 * @author backstage
 */
@Service
public class BookServiceImpl extends ServiceImpl<BookMapper, BookDO> implements IBookService
{
    @Resource
    private BookMapper bookMapper;

    @Resource
    private BookChapterMapper bookChapterMapper;

    @Resource
    private BookChapterService bookChapterService;

    @Resource
    private UserBookRelationMapper userBookRelationMapper;

    @Resource
    private BookTagDOMapper bookTagDOMapper;

    @Autowired
    private com.backstage.system.service.common.OssService ossService;

    @Autowired
    private com.backstage.system.service.book.IBookEsService bookEsService;

    @Autowired
    private AsyncTaskSupport asyncTaskSupport;

    @Resource(name = AsyncExecutorNames.AGGREGATION)
    private Executor aggregationTaskExecutor;

    @Autowired
    private OrderCheckoutService orderCheckoutService;

    @Resource
    private OshOrderMapper oshOrderMapper;

    /**
     * 查询电子书列表
     *
     * @param book 电子书
     * @return 电子书集合
     */
    /**
     * 查询电子书列表
     *
     * @param reqVO 电子书
     * @return 电子书集合
     */

    @Override
    public Page<BookListVO> getBookPageList(BookListReqVO reqVO) {
        Page<BookDO> pageParam = new Page<>(reqVO.getPageNum(), reqVO.getPageSize());
        List<BookListVO> bookListVOS = bookMapper.getBookPageList(pageParam, reqVO);

        // tagNames 逗号字符串 -> tagNameList 数组
        for (BookListVO vo : bookListVOS) {
            if (StringUtils.isNotEmpty(vo.getTagNames())) {
                vo.setTagNameList(Arrays.asList(vo.getTagNames().split(",")));
            }
            // 处理封面URL - 生成临时访问链接（30分钟有效期）
            if (StringUtils.isNotEmpty(vo.getCover())) {
                vo.setCover(ossService.getLimitedUrl(vo.getCover(), 30));
            }
        }

        Page<BookListVO> result = new Page<>(pageParam.getCurrent(), pageParam.getSize(), pageParam.getTotal());
        result.setRecords(bookListVOS);
        return result;
    }

    // 获取筛选的电子书列表
    @Override
    public Page<BookListVO> getFilterBookList(String filter) {
        // 分页：第1页，每页12条
        Page<BookDO> pageParam = new Page<>(1, 12);

        Page<BookListVO> voPage = bookMapper.selectFreeBookPage(pageParam);

        // SQL 查出的逗号分隔 tagNames → 转成 List<String>
        for (BookListVO vo : voPage.getRecords()) {
            if (StrUtil.isNotBlank(vo.getTagNames())) {
                List<String> tagList = Arrays.asList(vo.getTagNames().split(","));
                vo.setTagNameList(tagList);
            }
            // 处理封面URL - 生成临时访问链接（30分钟有效期）
            if (StringUtils.isNotEmpty(vo.getCover())) {
                vo.setCover(ossService.getLimitedUrl(vo.getCover(), 30));
            }
        }
        return voPage;
    }


    @Override
    public List<String> getTagList() {
        return bookTagDOMapper.getTagList();
    }

    /**
     * 查询电子书详情
     *
     * @param id 电子书ID
     * @param forEdit 是否用于编辑（true时返回原始相对路径，false时返回临时访问URL）
     * @return 电子书详情
     */
    @Override
    public BookDetailVO selectBookDetail(Long id, Boolean forEdit) {
        BookDetailAggregate aggregate = loadBookDetailAggregate(id, UserContextUtil.getCurrentUserIdSafely());
        return buildBookDetailVO(aggregate, forEdit);
    }

    /**
     * 查询电子书章节内容
     *
     * @param bookId 电子书ID
     * @param id 章节ID
     * @return 章节内容
     */
    @Override
    public BookChapterContentVO selectBookChapterContent(Long bookId, Long id) {
        BookChapter chapter = bookChapterMapper.selectBookChapterByBookIdAndId(bookId, id);
        checkEntityNotNull(chapter, "该记录不存在");

//        // 如果不是免费章节，检查用户是否购买
//        if (chapter.getIsFree() == 0)
//        {
//            checkUserHasBoughtOrThrow(userId, bookId);
//        }

        BookChapterContentVO vo = new BookChapterContentVO();
        BeanUtils.copyProperties(chapter, vo);
        vo.setIsFree(chapter.getIsFree());

        return vo;
    }

    /**
     * 查询电子书章节菜单
     *
     * @param id 电子书ID
     * @return 章节菜单
     */
    @Override
    public BookMenuVO selectBookMenu(Long id) {
        CompletableFuture<BookDO> bookFuture = asyncTaskSupport.supplyAsync(() -> {
            return requireBook(id);
        }, aggregationTaskExecutor);
        CompletableFuture<List<BookChapterVO>> chapterFuture = asyncTaskSupport.supplyAsync(
                () -> bookChapterMapper.selectBookChapterListByBookId(id), aggregationTaskExecutor);

        asyncTaskSupport.awaitAll(bookFuture, chapterFuture);

        BookDO bookDO = asyncTaskSupport.join(bookFuture);
        List<BookChapterVO> chapters = asyncTaskSupport.join(chapterFuture);

        BookMenuVO vo = new BookMenuVO();

        // 设置电子书基本信息
        BookSimpleVO simpleVO = new BookSimpleVO();
        BeanUtils.copyProperties(bookDO, simpleVO);
        if (StringUtils.isNotEmpty(simpleVO.getCover())) {
            simpleVO.setCover(ossService.getLimitedUrl(simpleVO.getCover(), 30));
        }
        vo.setDetail(simpleVO);

        vo.setMenus(chapters);

        return vo;
    }

    /**
     * 查询用户购买的电子书列表
     *
     * @param userId 用户ID
     * @return 电子书集合
     */
    @Override
    public List<BookDO> selectUserBookList(Long userId)
    {
        return userBookRelationMapper.selectUserBookList(userId);
    }

    /**
     * 分页查询用户购买的电子书列表
     *
     * @param userId 用户ID
     * @param page 分页参数
     * @return 电子书分页集合
     */
    @Override
    public Page<BookDO> selectUserBookListPage(Long userId, Page<BookDO> page) {
        return userBookRelationMapper.selectUserBookListPage(userId, page);
    }

    @Override
    @Transactional
    public void createBookChapter(BookChapterSaveUpdateVO reqVO) {
        BookDO bookDO = getById(reqVO.getBookId());
        checkEntityNotNull(bookDO, "电子书不存在");

        BookChapter bookChapter = new BookChapter();
        BeanUtils.copyProperties(reqVO, bookChapter);

        Integer chapterNo = reqVO.getChapterNo();
        if (chapterNo == null) {
            Integer maxChapterNo = bookChapterMapper.selectMaxChapterNoByBookId(reqVO.getBookId());
            chapterNo = maxChapterNo == null ? 1 : maxChapterNo + 1;
        }
        if (chapterNo < 1) {
            throw new ServiceException("章节号必须大于等于1");
        }
        bookChapter.setChapterNo(chapterNo);
        bookChapter.setSortOrder(reqVO.getSortOrder() == null ? chapterNo : reqVO.getSortOrder());
        bookChapter.setIsFree(reqVO.getIsFree());
        if (bookChapter.getSortOrder() < 1) {
            throw new ServiceException("排序值必须大于等于1");
        }

        bookChapterMapper.insert(bookChapter);
    }

    @Override
    @Transactional
    public void updateBookChapter(BookChapterSaveUpdateVO reqVO) {
        BookDO bookDO = getById(reqVO.getBookId());
        checkEntityNotNull(bookDO, "电子书不存在");

        BookChapter bookChapter = bookChapterMapper.selectBookChapterByBookIdAndId(reqVO.getBookId(), reqVO.getId());
        checkEntityNotNull(bookChapter, "章节不存在");

        if (reqVO.getTitle() != null) {
            bookChapter.setTitle(reqVO.getTitle());
        }
        if (reqVO.getContent() != null) {
            bookChapter.setContent(reqVO.getContent());
        }
        if (reqVO.getChapterNo() != null) {
            if (reqVO.getChapterNo() < 1) {
                throw new ServiceException("章节号必须大于等于1");
            }
            bookChapter.setChapterNo(reqVO.getChapterNo());
        }
        if (reqVO.getSortOrder() != null) {
            if (reqVO.getSortOrder() < 1) {
                throw new ServiceException("排序值必须大于等于1");
            }
            bookChapter.setSortOrder(reqVO.getSortOrder());
        }
        if (reqVO.getIsFree() != null) {
            bookChapter.setIsFree(reqVO.getIsFree());
        }

        if (bookChapter.getSortOrder() == null && bookChapter.getChapterNo() != null) {
            bookChapter.setSortOrder(bookChapter.getChapterNo());
        }

        bookChapterMapper.updateById(bookChapter);
    }

    /**
     * 新增电子书
     *
     * @param reqVO 电子书请求VO
     * @return 电子书响应VO
     */
    @Override
    @Transactional
    public Long createBook(BookSaveReqVO reqVO) {

        BookDO bookDO = new BookDO();
        BeanUtils.copyProperties(reqVO, bookDO);
        bookDO.setOriginalPrice(reqVO.getTPrice());
        bookDO.setStatus("0");
        if (bookDO.getLevel() == null) {
            bookDO.setLevel(1);
        }
        // 新增电子书
        bookMapper.insert(bookDO);
        // 新增电子书标签表记录
        List<BookTagDO> bookTagDOS = packageBookTagInsertDOList(bookDO.getId(), reqVO);
        if (!bookTagDOS.isEmpty()) {
            bookTagDOMapper.insert(bookTagDOS);
        }
        // 批量新增章节
        batchInsertChapters(bookDO.getId(), reqVO.getChapters());
        // 同步ES
        bookEsService.syncBookToEs(bookDO.getId());
        return bookDO.getId();
    }

    /**
     * 封装标签数据
     *
     * @param reqVO 电子书DO
     */
    private List<BookTagDO> packageBookTagInsertDOList(Long id, BookSaveReqVO reqVO) {
        List<BookTagDO> insertList = new ArrayList<>();
        List<String> tags = reqVO.getTags() == null ? Collections.emptyList() : reqVO.getTags();
        for (int i = 0; i < tags.size(); i++) {
            BookTagDO bookTagInsertDO = new BookTagDO();
            bookTagInsertDO.setBookId(id);
            bookTagInsertDO.setTagName(tags.get(i));
            bookTagInsertDO.setSortOrder(i + 1);
            insertList.add(bookTagInsertDO);
        }
        return insertList;
    }

    /**
     * 批量插入章节
     *
     * @param bookId   电子书ID
     * @param chapters 章节列表
     */
    private void batchInsertChapters(Long bookId, List<BookChapterSaveUpdateVO> chapters) {
        if (chapters == null || chapters.isEmpty()) {
            return;
        }

        List<BookChapter> chapterList = new ArrayList<>();
        for (int i = 0; i < chapters.size(); i++) {
            BookChapterSaveUpdateVO chapterVO = chapters.get(i);
            BookChapter bookChapter = new BookChapter();
            BeanUtils.copyProperties(chapterVO, bookChapter);
            bookChapter.setBookId(bookId);

            // 处理章节号
            Integer chapterNo = chapterVO.getChapterNo();
            if (chapterNo == null) {
                chapterNo = i + 1;
            }
            if (chapterNo < 1) {
                throw new ServiceException("章节号必须大于等于1");
            }
            bookChapter.setChapterNo(chapterNo);

            // 处理排序
            Integer sortOrder = chapterVO.getSortOrder();
            if (sortOrder == null) {
                sortOrder = chapterNo;
            }
            if (sortOrder < 1) {
                throw new ServiceException("排序值必须大于等于1");
            }
            bookChapter.setSortOrder(sortOrder);

            chapterList.add(bookChapter);
        }

        // 批量插入
        bookChapterService.saveBatch(chapterList);
    }

    /**
     * 修改电子书
     *
     * @param reqVO 电子书请求VO
     * @return 电子书响应VO
     */
    @Override
    @Transactional
    public void updateBook(BookSaveReqVO reqVO)
    {
        BookDO book = getById(reqVO.getId());
        checkEntityNotNull(book, "电子书不存在");
        BookDO bookDO = new BookDO();
        BeanUtils.copyProperties(reqVO, bookDO);
        bookDO.setOriginalPrice(reqVO.getTPrice());
        if (bookDO.getLevel() == null) {
            bookDO.setLevel(1);
        }
        bookMapper.updateById(bookDO);

        // 更新电子书标签
        // 先删除该电子书原有的标签
        bookTagDOMapper.delete(
                new LambdaQueryWrapper<BookTagDO>()
                        .eq(BookTagDO::getBookId, reqVO.getId())
        );
        // 再新增新的标签
        List<BookTagDO> bookTagDOS = packageBookTagInsertDOList(reqVO.getId(), reqVO);
        if (!bookTagDOS.isEmpty()) {
            bookTagDOMapper.insert(bookTagDOS);
        }

        syncBookChapters(reqVO.getId(), reqVO.getChapters());

        // 同步ES
        bookEsService.syncBookToEs(reqVO.getId());
    }

    private void syncBookChapters(Long bookId, List<BookChapterSaveUpdateVO> chapters) {
        List<BookChapterVO> existingChapters = bookChapterMapper.selectBookChapterListByBookId(bookId);
        List<Long> incomingIds = new ArrayList<>();

        if (chapters != null) {
            for (int i = 0; i < chapters.size(); i++) {
                BookChapterSaveUpdateVO chapterVO = chapters.get(i);
                chapterVO.setBookId(bookId);
                if (chapterVO.getChapterNo() == null || chapterVO.getChapterNo() < 1) {
                    chapterVO.setChapterNo(i + 1);
                }
                if (chapterVO.getSortOrder() == null || chapterVO.getSortOrder() < 1) {
                    chapterVO.setSortOrder(i + 1);
                }
                if (chapterVO.getIsFree() == null) {
                    chapterVO.setIsFree(0);
                }

                if (chapterVO.getId() == null) {
                    createBookChapter(chapterVO);
                    continue;
                }

                incomingIds.add(chapterVO.getId());
                updateBookChapter(chapterVO);
            }
        }

        List<Long> toDeleteIds = existingChapters.stream()
                .map(BookChapterVO::getId)
                .filter(id -> !incomingIds.contains(id))
                .collect(Collectors.toList());

        if (!toDeleteIds.isEmpty()) {
            bookChapterMapper.delete(new LambdaQueryWrapper<BookChapter>()
                    .eq(BookChapter::getBookId, bookId)
                    .in(BookChapter::getId, toDeleteIds));
        }
    }

    /**
     * 删除电子书（逻辑删除）
     *
     * @param id 电子书ID
     */
    @Override
    @Transactional
    public void deleteBook(Long id)
    {
        BookDO bookDO = getById(id);
        checkEntityNotNull(bookDO, "电子书不存在");
        bookMapper.deleteById(id);

        // 删除关联标签
        bookTagDOMapper.delete(new LambdaQueryWrapper<BookTagDO>()
                .eq(BookTagDO::getBookId, id)
                .eq(BookTagDO::getDelFlag, 0));

        // 删除关联章节
        bookChapterMapper.delete(new LambdaQueryWrapper<BookChapter>()
                .eq(BookChapter::getBookId, id)
                .eq(BookChapter::getDelFlag, 0));

        // 从ES删除
        bookEsService.deleteBookFromEs(id);
    }

    /**
     * 检查实体是否为空，为空则抛出异常
     *
     * @param entity 实体对象
     * @param message 异常信息
     */
    private void checkEntityNotNull(Object entity, String message)
    {
        if (StringUtils.isNull(entity)) {
            throw new ServiceException(message);
        }
    }

    /**
     * 检查用户是否购买电子书
     *
     * @param userId 用户ID
     * @param bookId 电子书ID
     * @return 是否购买
     */
    private boolean checkUserHasBought(Long userId, Long bookId)
    {
        if (StringUtils.isNull(userId)) {
            return false;
        }
        UserBookRelation relation = userBookRelationMapper.selectByUserIdAndBookId(userId, bookId);
        return relation != null && Integer.valueOf(1).equals(relation.getPurchased());
    }

    /**
     * 检查用户是否购买电子书，未购买则抛出异常
     *
     * @param userId 用户ID
     * @param bookId 电子书ID
     */
    private void checkUserHasBoughtOrThrow(Long userId, Long bookId)
    {
        if (!checkUserHasBought(userId, bookId)) {
            throw new ServiceException("请先购买该电子书");
        }
    }

    /**
     * 获取或创建用户电子书关联记录
     */
    private UserBookRelation getOrCreateRelation(Long userId, Long bookId) {
        UserBookRelation relation = userBookRelationMapper.selectByUserIdAndBookId(userId, bookId);
        if (relation == null) {
            relation = new UserBookRelation();
            relation.setUserId(userId);
            relation.setBookId(bookId);
            relation.setFavorited(0);
            relation.setFollowed(0);
            relation.setPurchased(0);
            userBookRelationMapper.insert(relation);
        }
        return relation;
    }

    @Override
    @Transactional
    public void favoriteBook(Long bookId, Integer status) {
        Long userId = UserContextUtil.getCurrentUserId();
        BookDO bookDO = getById(bookId);
        checkEntityNotNull(bookDO, "电子书不存在");

        UserBookRelation relation = getOrCreateRelation(userId, bookId);
        relation.setFavorited(status);
        relation.setFavoriteTime(status == 1 ? LocalDateTime.now() : null);
        userBookRelationMapper.updateById(relation);
    }

    @Override
    @Transactional
    public void followBook(Long bookId, Integer status) {
        Long userId = ThreadLocalUtil.getCurrentUserId();
        BookDO bookDO = getById(bookId);
        checkEntityNotNull(bookDO, "电子书不存在");

        UserBookRelation relation = getOrCreateRelation(userId, bookId);
        relation.setFollowed(status);
        relation.setFollowTime(status == 1 ? LocalDateTime.now() : null);
        userBookRelationMapper.updateById(relation);
    }

    @Override
    public OrderCheckoutRespVO purchaseBook(Long bookId, Long userId, String channel) {
        BookDO bookDO = getById(bookId);
        checkEntityNotNull(bookDO, "电子书不存在");

        // 校验是否已购买
        UserBookRelation relation = userBookRelationMapper.selectByUserIdAndBookId(userId, bookId);
        if (relation != null && Integer.valueOf(1).equals(relation.getPurchased())) {
            throw new ServiceException("您已购买该电子书");
        }

        // 构建订单结算参数，交给订单模块处理
        return orderCheckoutService.checkout(buildBookCheckoutReqVO(bookDO, bookId, userId, channel));
    }

    /**
     * 构建电子书下单结算参数。
     *
     * @param bookDO 电子书信息
     * @param bookId 电子书ID
     * @param userId 用户ID
     * @param channel 支付渠道
     * @return 订单结算参数
     */
    private OrderCheckoutReqVO buildBookCheckoutReqVO(BookDO bookDO,
                                                      Long bookId,
                                                      Long userId,
                                                      String channel) {
        OrderCheckoutReqVO reqVO = new OrderCheckoutReqVO();
        reqVO.setUserId(userId);
        reqVO.setProductType(ProductTypeEnum.BOOK.getCode());
        reqVO.setProductId(bookId);
        reqVO.setProductName(bookDO.getTitle());
        reqVO.setOriginalAmount(bookDO.getPrice());
        reqVO.setPayableAmount(bookDO.getPrice());
        reqVO.setDiscountAmount(java.math.BigDecimal.ZERO);
        reqVO.setChannel(channel);
        return reqVO;
    }

    @Override
    @Transactional
    public void grantBookAccess(String orderNo) {
        // 查询订单获取 userId 和 productId
        OshOrder order = oshOrderMapper.selectByOrderNo(orderNo);
        if (order == null) {
            throw new ServiceException("订单不存在: " + orderNo);
        }

        Long userId = order.getUserId();
        Long bookId = order.getProductId();

        // 幂等校验
        UserBookRelation relation = getOrCreateRelation(userId, bookId);
        if (Integer.valueOf(1).equals(relation.getPurchased())) {
            return;
        }

        BookDO bookDO = getById(bookId);
        relation.setPurchased(1);
        relation.setPurchasePrice(bookDO != null ? bookDO.getPrice() : java.math.BigDecimal.ZERO);
        relation.setOrderNo(orderNo);
        relation.setPayTime(LocalDateTime.now());
        userBookRelationMapper.updateById(relation);
    }

    @Override
    public BookRelationStatusVO getBookRelationStatus(Long bookId) {
        Long userId = ThreadLocalUtil.getCurrentUserId();
        return buildBookRelationStatus(userId, bookId);
    }

    private BookRelationStatusVO buildBookRelationStatus(Long userId, Long bookId)
    {
        if (userId == null)
        {
            return buildAnonymousRelationStatus();
        }

        BookRelationStatusVO vo = new BookRelationStatusVO();
        UserBookRelation relation = userBookRelationMapper.selectByUserIdAndBookId(userId, bookId);
        if (relation == null)
        {
            vo.setFavorited(0);
            vo.setFollowed(0);
            vo.setPurchased(0);
            return vo;
        }

        vo.setFavorited(Optional.ofNullable(relation.getFavorited()).orElse(0));
        vo.setFollowed(Optional.ofNullable(relation.getFollowed()).orElse(0));
        vo.setPurchased(Optional.ofNullable(relation.getPurchased()).orElse(0));
        return vo;
    }

    private BookDetailAggregate loadBookDetailAggregate(Long bookId, Long userId)
    {
        CompletableFuture<BookDO> bookFuture = asyncTaskSupport.supplyAsync(() -> requireBook(bookId), aggregationTaskExecutor);
        CompletableFuture<List<BookChapterVO>> chapterFuture = asyncTaskSupport.supplyAsync(
                () -> bookChapterMapper.selectBookChapterListByBookId(bookId), aggregationTaskExecutor);
        CompletableFuture<List<String>> tagFuture = asyncTaskSupport.supplyAsync(
                () -> bookTagDOMapper.selectBookTagListByBookId(bookId), aggregationTaskExecutor);
        CompletableFuture<BookRelationStatusVO> relationFuture = userId == null
                ? asyncTaskSupport.completedFuture(buildAnonymousRelationStatus())
                : asyncTaskSupport.supplyAsync(() -> buildBookRelationStatus(userId, bookId), aggregationTaskExecutor);

        asyncTaskSupport.awaitAll(bookFuture, chapterFuture, tagFuture, relationFuture);
        return new BookDetailAggregate(
                asyncTaskSupport.join(bookFuture),
                asyncTaskSupport.join(chapterFuture),
                asyncTaskSupport.join(tagFuture),
                asyncTaskSupport.join(relationFuture)
        );
    }

    private BookDetailVO buildBookDetailVO(BookDetailAggregate aggregate, Boolean forEdit)
    {
        BookDO bookDO = aggregate.getBook();
        BookDetailVO vo = new BookDetailVO();
        BeanUtils.copyProperties(bookDO, vo);
        vo.setDesc(bookDO.getDescription());
        vo.setTryContent(bookDO.getTryContent());
        vo.setPrice(Optional.ofNullable(bookDO.getPrice()).map(Object::toString).orElse("0"));
        vo.setTPrice(Optional.ofNullable(bookDO.getOriginalPrice()).map(Object::toString).orElse("0"));
        vo.setSubCount(Optional.ofNullable(bookDO.getSubCount()).orElse(0));
        vo.setLevel(bookDO.getLevel());
        vo.setCover(resolveBookCover(bookDO.getCover(), forEdit));
        vo.setBookDetails(aggregate.getChapters());
        vo.setTags(aggregate.getTags());
        vo.setIsbuy(aggregate.getRelationStatus().getPurchased() == 1);
        vo.setIsfava(aggregate.getRelationStatus().getFavorited() == 1);
        return vo;
    }

    private BookDO requireBook(Long bookId)
    {
        BookDO bookDO = getById(bookId);
        checkEntityNotNull(bookDO, "该记录不存在");
        return bookDO;
    }

    private String resolveBookCover(String cover, Boolean forEdit)
    {
        if (StringUtils.isEmpty(cover))
        {
            return cover;
        }
        if (Boolean.TRUE.equals(forEdit))
        {
            return cover;
        }
        return ossService.getLimitedUrl(cover, 30);
    }

    private BookRelationStatusVO buildAnonymousRelationStatus()
    {
        BookRelationStatusVO vo = new BookRelationStatusVO();
        vo.setFavorited(0);
        vo.setFollowed(0);
        vo.setPurchased(0);
        return vo;
    }

    private static class BookDetailAggregate
    {
        private final BookDO book;

        private final List<BookChapterVO> chapters;

        private final List<String> tags;

        private final BookRelationStatusVO relationStatus;

        private BookDetailAggregate(BookDO book, List<BookChapterVO> chapters, List<String> tags, BookRelationStatusVO relationStatus)
        {
            this.book = book;
            this.chapters = chapters;
            this.tags = tags;
            this.relationStatus = relationStatus;
        }

        public BookDO getBook()
        {
            return book;
        }

        public List<BookChapterVO> getChapters()
        {
            return chapters;
        }

        public List<String> getTags()
        {
            return tags;
        }

        public BookRelationStatusVO getRelationStatus()
        {
            return relationStatus;
        }
    }
}
