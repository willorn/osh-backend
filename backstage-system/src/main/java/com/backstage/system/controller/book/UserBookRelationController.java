package com.backstage.system.controller.book;

import com.backstage.common.annotation.Anonymous;
import com.backstage.common.core.domain.R;
import com.backstage.common.threadlocal.ThreadLocalUtil;
import com.backstage.system.domain.vo.pay.OrderCheckoutRespVO;
import com.backstage.system.domain.vo.book.BookRelationActionReqVO;
import com.backstage.system.domain.vo.book.BookRelationReqVO;
import com.backstage.system.domain.vo.book.BookRelationStatusVO;
import com.backstage.system.service.book.IBookService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

/**
 * 用户电子书关联 Controller（收藏/关注/购买）
 *
 * @author backstage
 */
@Validated
@RestController
@RequestMapping("/pc/book/relation")
public class UserBookRelationController {

    @Resource
    private IBookService bookService;

    /**
     * 收藏/取消收藏电子书
     */
    @PostMapping("/favorite")
    @PreAuthorize("hasAuthority('book:favorite')")
    public R<String> favorite(@Valid @RequestBody BookRelationActionReqVO reqVO) {
        bookService.favoriteBook(reqVO.getBookId(), reqVO.getStatus());
        return R.ok(reqVO.getStatus() == 1 ? "收藏成功" : "取消收藏成功");
    }

    /**
     * 关注/取消关注电子书
     */
    @PostMapping("/follow")
    @PreAuthorize("hasAuthority('book:follow')")
    public R<String> follow(@Valid @RequestBody BookRelationActionReqVO reqVO) {
        bookService.followBook(reqVO.getBookId(), reqVO.getStatus());
        return R.ok(reqVO.getStatus() == 1 ? "关注成功" : "取消关注成功");
    }

    /**
     * 购买电子书，返回支付信息（二维码/支付链接）
     */
    @PostMapping("/purchase")
    @PreAuthorize("hasAuthority('book:purchase')")
    public R<OrderCheckoutRespVO> purchase(@Valid @RequestBody BookRelationReqVO reqVO) {
        Long userId = ThreadLocalUtil.getCurrentUserId();
        OrderCheckoutRespVO result = bookService.purchaseBook(
                reqVO.getBookId(), userId, reqVO.getChannel());
        return R.ok(result);
    }

    /**
     * 查询用户与某本电子书的关联状态
     */
    @GetMapping("/status")
    @PreAuthorize("hasAuthority('book:status')")
    public R<BookRelationStatusVO> status(@NotNull(message = "电子书ID不能为空") @RequestParam Long bookId) {
        return R.ok(bookService.getBookRelationStatus(bookId));
    }
}
