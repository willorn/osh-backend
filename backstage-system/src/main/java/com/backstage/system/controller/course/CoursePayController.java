package com.backstage.system.controller.course;

import java.util.LinkedHashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;

import com.backstage.common.annotation.Anonymous;
import com.backstage.common.annotation.OshUserEvent;
import com.backstage.common.constant.ResourceType;
import com.backstage.common.core.domain.R;
import com.backstage.common.exception.ServiceException;
import com.backstage.system.domain.vo.order.PayResponse;
import com.backstage.system.mapper.course.CourseBuyMapper;
import com.backstage.system.service.course.ICoursePayService;
import com.backstage.system.utils.UserContextUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Course payment HTTP entry.
 *
 * Endpoints are intentionally namespaced under /pc/course/pay so they live next to other course
 * endpoints (e.g. OshCourseController @ /pc/course/*) without colliding with any existing
 * method-level path. The shared payment controllers (PayController / WxPayController /
 * IsWxPayController) are left untouched and continue to serve any other module that relies on them.
 */
@RestController
@RequestMapping("/pc/course/pay")
public class CoursePayController {

    @Autowired
    private ICoursePayService coursePayService;
    @Autowired
    private CourseBuyMapper courseBuyMapper;

    /**
     * Create a course payment order and return a QR code string (wxpay or alipay).
     *
     * Request body example:
     *   { "course_id": 1000, "pay_type": "wxpay" }
     *
     * Response data example:
     *   { "qrcode": "weixin://wxpay/bizpayurl?pr=xxx",
     *     "payurl": "",
     *     "out_trade_no": "abc123",
     *     "pay_type": "wxpay" }
     */
    @Anonymous
    @PostMapping("/create")
    @OshUserEvent(module = "支付模块", actionType = "支付", resourceType = ResourceType.COURSE_TYPE,
            resourceIdExpression = "#p0['course_id']", description = "创建课程支付订单",
            recordAnonymous = true, successOnly = true)
    public R<Map<String, Object>> create(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Long userId = UserContextUtil.getCurrentUserIdSafely();
        if (userId == null) {
            return R.fail("请先登录后再支付");
        }
        Long courseId = parseCourseId(body.get("course_id"));
        String payType = body.get("pay_type") == null ? "wxpay" : body.get("pay_type").toString();
        String clientIp = resolveClientIp(request);

        try {
            PayResponse gatewayResp = coursePayService.createCoursePay(courseId, payType, clientIp, userId);

            // Gateway uses code=1 for success; surface the failure to the client verbatim.
            if (gatewayResp.getCode() != 1) {
                String msg = gatewayResp.getMsg() == null ? "支付网关返回失败" : gatewayResp.getMsg();
                return R.fail(msg);
            }

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("qrcode", gatewayResp.getQrcode());
            data.put("payurl", gatewayResp.getPayUrl());
            data.put("out_trade_no", gatewayResp.getOutTradeNo());
            data.put("pay_type", payType);
            data.put("course_id", courseId);
            return R.ok(data);
        } catch (ServiceException e) {
            return R.fail(e.getMessage());
        } catch (Exception e) {
            return R.fail("创建支付订单失败: " + e.getMessage());
        }
    }

    /**
     * Poll the gateway for the latest status of a previously created order.
     *
     * Query example: /pc/course/pay/status?out_trade_no=abc123
     * Response data: { "paid": true }
     */
    @Anonymous
    @GetMapping("/status")
    @OshUserEvent(module = "支付模块", actionType = "消费", resourceType = ResourceType.COURSE_TYPE,
            resourceIdExpression = "#result.data['course_id']",
            resourceNameExpression = "#result.data['order_no']",
            recordConditionExpression = "#result.data['consume_recorded'] == true",
            description = "课程支付成功消费", recordAnonymous = true, successOnly = true)
    public R<Map<String, Object>> status(@RequestParam("out_trade_no") String outTradeNo) {
        Long userId = UserContextUtil.getCurrentUserIdSafely();
        if (userId == null) {
            return R.fail("请先登录后再查询支付状态");
        }
        Map<String, Object> before = courseBuyMapper.selectByOrderNoAndUserId(outTradeNo, userId);
        boolean wasPaid = isPaid(before);
        boolean paid = coursePayService.isCoursePaid(outTradeNo, userId);
        Map<String, Object> latest = courseBuyMapper.selectByOrderNoAndUserId(outTradeNo, userId);
        boolean changedToPaid = paid && !wasPaid && isPaid(latest);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("paid", paid);
        data.put("consume_recorded", changedToPaid);
        data.put("order_no", outTradeNo);
        putIfNotNull(data, "course_id", latest == null ? null : latest.get("courseId"));
        putIfNotNull(data, "pay_price", latest == null ? null : latest.get("payPrice"));
        return R.ok(data);
    }

    private boolean isPaid(Map<String, Object> courseBuy) {
        return courseBuy != null && "paid".equals(String.valueOf(courseBuy.get("payStatus")));
    }

    private void putIfNotNull(Map<String, Object> data, String key, Object value) {
        if (value != null) {
            data.put(key, value);
        }
    }

    private Long parseCourseId(Object raw) {
        if (raw == null) {
            throw new ServiceException("course_id 不能为空");
        }
        try {
            return Long.parseLong(raw.toString());
        } catch (NumberFormatException e) {
            throw new ServiceException("course_id 必须是整数");
        }
    }

    private String resolveClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
