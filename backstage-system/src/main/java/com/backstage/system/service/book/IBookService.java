package com.backstage.system.service.book;

import com.backstage.system.controller.book.BookListReqVO;
import com.backstage.system.domain.book.BookDO;
import com.backstage.system.domain.vo.pay.OrderCheckoutRespVO;
import com.backstage.system.domain.vo.book.*;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import java.util.List;

/**
 * 电子书 服务层
 *
 * @author backstage
 */
public interface IBookService extends IService<BookDO> {


    /**
     * 查询电子书详情
     *
     * @param id 电子书ID
     * @param forEdit 是否用于编辑（true时返回原始相对路径，false时返回临时访问URL）
     * @return 电子书详情
     */
    BookDetailVO selectBookDetail(Long id, Boolean forEdit);

    /**
     * 查询电子书章节内容
     *
     * @param bookId 电子书ID
     * @param id 章节ID
     * @return 章节内容
     */
    BookChapterContentVO selectBookChapterContent(Long bookId, Long id);

    /**
     * 查询电子书章节菜单
     *
     * @param id 电子书ID
     * @return 章节菜单
     */
    BookMenuVO selectBookMenu(Long id);

    /**
     * 查询用户购买的电子书列表
     *
     * @param userId 用户ID
     * @return 电子书集合
     */
    List<BookDO> selectUserBookList(Long userId);

    /**
     * 分页查询用户购买的电子书列表
     *
     * @param userId 用户ID
     * @param page 分页参数
     * @return 电子书分页集合
     */
    Page<BookDO> selectUserBookListPage(Long userId, Page<BookDO> page);

    /**
     * 新增电子书
     *
     * @param reqVO 电子书请求VO
     * @return 电子书响应VO
     */
    Long createBook(BookSaveReqVO reqVO);

    /**
     * 修改电子书
     *
     * @param reqVO 电子书请求VO
     * @return 电子书响应VO
     */
    void updateBook(BookSaveReqVO reqVO);

    /**
     * 删除电子书（逻辑删除）
     *
     * @param id 电子书ID
     */
    void deleteBook(Long id);


    Page<BookListVO> getBookPageList(BookListReqVO reqVO);


    Page<BookListVO> getFilterBookList(String filter);



    /**
     * 新增电子书章节
     *
     * @param reqVO 章节请求VO
     */
    void createBookChapter(BookChapterSaveUpdateVO reqVO);

    /**
     * 修改电子书章节
     *
     * @param reqVO 章节请求VO
     */
    void updateBookChapter(BookChapterSaveUpdateVO reqVO);

    /**
     * 查询所有电子书标签名称（去重）
     *
     * @return 标签名称列表
     */
    List<String> getTagList();

    /**
     * 收藏/取消收藏电子书
     *
     * @param bookId 电子书ID
     * @param status 0-取消收藏，1-收藏
     */
    void favoriteBook(Long bookId, Integer status);

    /**
     * 关注/取消关注电子书
     *
     * @param bookId 电子书ID
     * @param status 0-取消关注，1-关注
     */
    void followBook(Long bookId, Integer status);

    /**
     * 购买电子书（发起支付，返回支付信息）
     *
     * @param bookId 电子书ID
     * @param userId 用户ID
     * @param channel 支付渠道
     * @return 支付结算结果
     */
    OrderCheckoutRespVO purchaseBook(Long bookId, Long userId, String channel);

    /**
     * 发放电子书访问权限（支付成功后由回调触发）
     *
     * @param orderNo 订单号
     */
    void grantBookAccess(String orderNo);

    /**
     * 查询用户与某本电子书的关联状态
     *
     * @param bookId 电子书ID
     * @return 关联状态
     */
    BookRelationStatusVO getBookRelationStatus(Long bookId);
}
