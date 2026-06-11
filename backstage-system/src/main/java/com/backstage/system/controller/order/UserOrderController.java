package com.backstage.system.controller.order;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

import com.backstage.common.core.domain.R;
import com.backstage.system.domain.order.OshOrder;
import com.backstage.system.domain.order.enums.OrderStatusEnum;
import com.backstage.system.domain.order.enums.ProductTypeEnum;
import com.backstage.system.mapper.order.OshOrderMapper;
import com.backstage.system.utils.UserContextUtil;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 用户中心 - 我的订单记录（从统一订单表 osh_order 查询）
 */
@RestController
@RequestMapping("/pc/user/order")
public class UserOrderController {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Resource
    private OshOrderMapper oshOrderMapper;

    /**
     * 查询当前用户的订单列表
     *
     * @param page   页码
     * @param limit  每页条数
     * @param status 订单状态筛选：1-已支付，2-已取消/已关闭，不传则查全部
     */
    @GetMapping("/list")
    public R list(@RequestParam(defaultValue = "1") Integer page,
                  @RequestParam(defaultValue = "20") Integer limit,
                  @RequestParam(required = false) Integer status) {
        Long userId = UserContextUtil.getCurrentUserId();
        if (userId == null) {
            return R.fail("请先登录");
        }

        PageHelper.startPage(page, limit);
        List<OshOrder> orders = oshOrderMapper.selectByUserIdAndStatus(userId, status);
        PageInfo<OshOrder> pageInfo = new PageInfo<>(orders);

        // 转换为前端期望的格式
        List<LinkedHashMap<String, Object>> rows = pageInfo.getList().stream()
                .map(this::toFrontendVO)
                .collect(Collectors.toList());

        LinkedHashMap<String, Object> data = new LinkedHashMap<>();
        data.put("count", pageInfo.getTotal());
        data.put("rows", rows);

        return R.ok(data);
    }

    /**
     * 将 OshOrder 转换为前端 BuyList 组件期望的字段格式
     */
    private LinkedHashMap<String, Object> toFrontendVO(OshOrder order) {
        LinkedHashMap<String, Object> vo = new LinkedHashMap<>();
        vo.put("no", order.getOrderNo());
        vo.put("created_time", formatTime(order.getCreatedTime()));
        vo.put("goods", order.getProductName());
        vo.put("price", order.getPayableAmount());
        vo.put("total_price", order.getOriginalAmount());
        vo.put("status", mapStatus(order.getStatus()));
        vo.put("type", mapProductType(order.getProductType()));
        return vo;
    }

    private String mapStatus(Integer status) {
        if (status == null) {
            return "closed";
        }
        if (status == OrderStatusEnum.PENDING.getCode()) {
            return "pendding";
        }
        if (status == OrderStatusEnum.PAID.getCode()) {
            return "success";
        }
        return "closed";
    }

    private String mapProductType(Integer productType) {
        if (productType == null) {
            return "default";
        }
        ProductTypeEnum typeEnum = ProductTypeEnum.fromCode(productType);
        if (typeEnum == null) {
            return "default";
        }
        switch (typeEnum) {
            case GROUP:
                return "group";
            case SECKILL:
                return "flashsale";
            case MEMBER:
                return "member";
            default:
                return "default";
        }
    }

    private String formatTime(LocalDateTime time) {
        if (time == null) {
            return "";
        }
        return time.format(TIME_FORMATTER);
    }
}
