package com.backstage.system.service.impl.course;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import com.backstage.common.config.PayConfig;
import com.backstage.common.exception.ServiceException;
import com.backstage.system.domain.course.OshCourse;
import com.backstage.system.domain.order.enums.ProductTypeEnum;
import com.backstage.system.domain.vo.order.PayResponse;
import com.backstage.system.mapper.course.CourseBuyMapper;
import com.backstage.system.mapper.course.OshCourseMapper;
import com.backstage.system.service.behavior.ContributionService;
import com.backstage.system.service.course.ICoursePayService;
import com.backstage.system.utils.SignUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

/**
 * Course-scoped payment service.
 *
 * Mirrors the upstream call style of {@code PayServiceImpl} on purpose so that the existing
 * shared payment flow remains untouched. Differences vs. the shared {@code PayServiceImpl}:
 *   - {@code payType} is parameterised (wxpay / alipay) instead of hard-coded to wxpay.
 *   - Price is resolved server-side from {@code osh_course} so the client cannot tamper with it.
 *   - Status query is folded into the same service for cohesion within the course module.
 */
@Service
public class CoursePayServiceImpl implements ICoursePayService {
    private static final Logger log = LoggerFactory.getLogger(CoursePayServiceImpl.class);

    private static final String PAY_TYPE_WECHAT = "wxpay";
    private static final String PAY_TYPE_ALIPAY = "alipay";

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private OshCourseMapper oshCourseMapper;

    @Autowired
    private CourseBuyMapper courseBuyMapper;

    @Autowired
    private PayConfig payConfig;

    @Autowired
    private ContributionService contributionService;

    @Override
    public PayResponse createCoursePay(Long courseId, String payType, String clientIp, Long userId) {
        if (courseId == null || courseId <= 0) {
            throw new ServiceException("course_id 不能为空");
        }
        if (userId == null || userId <= 0) {
            throw new ServiceException("请先登录后再支付");
        }
        String channel = normalizePayType(payType);

        if (courseBuyMapper.countPaidByUserAndCourse(userId, courseId) > 0) {
            throw new ServiceException("该课程已购买，请勿重复支付");
        }

        OshCourse course = oshCourseMapper.selectCourseById(courseId);
        if (course == null) {
            throw new ServiceException("课程不存在: " + courseId);
        }
        BigDecimal price = course.getPrice();
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ServiceException("课程价格无效");
        }

        // Strip dashes to stay below the gateway's typical out_trade_no length limit.
        String outTradeNo = UUID.randomUUID().toString().replace("-", "");
        String moneyStr = price.setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString();
        String title = course.getTitle() == null ? ("课程#" + courseId) : course.getTitle();

        Map<String, String> params = new HashMap<>();
        params.put("pid", payConfig.PID);
        params.put("type", channel);
        params.put("out_trade_no", outTradeNo);
        params.put("notify_url", payConfig.NOTIFY_URL);
        params.put("return_url", PayConfig.RETURN_URL);
        params.put("name", title);
        params.put("money", moneyStr);
        params.put("clientip", clientIp == null ? "127.0.0.1" : clientIp);
        params.put("device", "pc");
        params.put("sign_type", "MD5");

        String sign = SignUtil.createSign(params, payConfig.KEY);
        params.put("sign", sign);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.setAll(params);

        try {
            String result = restTemplate.postForObject(payConfig.API_URL, form, String.class);

            PayResponse response = objectMapper.readValue(result, PayResponse.class);
            response.setOutTradeNo(outTradeNo);
            if (response.getCode() == 1) {
                courseBuyMapper.upsertPendingOrder(userId, courseId, outTradeNo, channel, price, price);
            }
            return response;
        } catch (Exception e) {
            // Surface a typed error so the controller can return a clean R.fail() to the client.
            throw new ServiceException("调用支付网关失败: " + e.getMessage());
        }
    }

    @Override
    public boolean isCoursePaid(String outTradeNo, Long userId) {
        if (outTradeNo == null || outTradeNo.isEmpty() || userId == null || userId <= 0) {
            return false;
        }

        if (courseBuyMapper.countPaidByOrderNoAndUserId(outTradeNo, userId) > 0) {
            return true;
        }

        // The gateway documents `act=order` as the per-order lookup endpoint.
        String queryUrl = payConfig.STATUS_URL
                + "?act=order"
                + "&pid=" + payConfig.PID
                + "&key=" + payConfig.KEY
                + "&out_trade_no=" + outTradeNo;

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> res = restTemplate.getForObject(queryUrl, Map.class);
            if (res == null) {
                return false;
            }
            boolean paid = isGatewayPaid(res);
            if (paid) {
                Map<String, Object> courseBuy = courseBuyMapper.selectByOrderNoAndUserId(outTradeNo, userId);
                int updated = courseBuyMapper.markPaidByOrderNoAndUserId(outTradeNo, userId);
                if (updated > 0) {
                    recordCourseRevenue(courseBuy, outTradeNo, userId);
                }
            }
            return paid;
        } catch (Exception e) {
            // Gateway hiccup should not throw; let the client keep polling.
            return false;
        }
    }

    private String normalizePayType(String payType) {
        if (payType == null) {
            return PAY_TYPE_WECHAT;
        }
        String lower = payType.trim().toLowerCase();
        if (PAY_TYPE_WECHAT.equals(lower) || PAY_TYPE_ALIPAY.equals(lower)) {
            return lower;
        }
        throw new ServiceException("不支持的支付方式: " + payType);
    }

    private boolean isGatewayPaid(Map<String, Object> res) {
        Object topStatus = res.get("status");
        Object topTradeStatus = res.get("trade_status");
        Object topCode = res.get("code");

        Object dataObj = res.get("data");
        Object dataStatus = null;
        Object dataTradeStatus = null;
        if (dataObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) dataObj;
            dataStatus = data.get("status");
            dataTradeStatus = data.get("trade_status");
        }

        // Different gateways/versions return different shapes. Treat any explicit "paid"
        // status as success to avoid false negatives that keep users stuck on QR page.
        return isPaidValue(topStatus)
                || isPaidValue(topTradeStatus)
                || isPaidValue(dataStatus)
                || isPaidValue(dataTradeStatus)
                || ("1".equals(String.valueOf(topCode)) && (isPaidValue(topStatus) || isPaidValue(dataStatus)));
    }

    private void recordCourseRevenue(Map<String, Object> courseBuy, String outTradeNo, Long userId) {
        if (courseBuy == null) {
            return;
        }
        try {
            contributionService.recordRevenue(
                    ProductTypeEnum.COURSE.getName(),
                    toLong(courseBuy.get("courseId")),
                    toLong(courseBuy.get("id")),
                    outTradeNo,
                    userId,
                    toBigDecimal(courseBuy.get("payPrice")),
                    0L,
                    ProductTypeEnum.COURSE.getName()
            );
        } catch (Exception e) {
            log.warn("record course contribution revenue failed, orderNo={}, userId={}", outTradeNo, userId, e);
        }
    }

    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.valueOf(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        if (value instanceof Number) {
            return BigDecimal.valueOf(((Number) value).doubleValue());
        }
        try {
            return new BigDecimal(value.toString());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    private boolean isPaidValue(Object raw) {
        if (raw == null) {
            return false;
        }
        String v = String.valueOf(raw).trim();
        if (v.isEmpty()) {
            return false;
        }
        return Objects.equals(v, "1")
                || "TRADE_SUCCESS".equalsIgnoreCase(v)
                || "SUCCESS".equalsIgnoreCase(v)
                || "PAID".equalsIgnoreCase(v)
                || "已支付".equals(v)
                || "支付成功".equals(v);
    }
}
