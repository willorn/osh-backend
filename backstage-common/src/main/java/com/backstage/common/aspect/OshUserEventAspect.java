package com.backstage.common.aspect;

import com.alibaba.fastjson2.JSON;
import com.backstage.common.constant.OshResourceConstants;
import com.backstage.common.constant.OshUserConstants;
import com.backstage.common.core.domain.OshUserEvent;
import com.backstage.common.core.redis.RedisCache;
import com.backstage.common.enums.ResultCode;
import com.backstage.common.threadlocal.ThreadLocalUtil;
import com.backstage.common.utils.ServletUtils;
import com.backstage.common.utils.generate.GenerateUtil;
import com.backstage.common.utils.ip.IpUtils;
import com.backstage.common.utils.jwt.JwtUtil;
import com.backstage.common.utils.kafka.KafkaMessageUtil;
import com.backstage.common.utils.list.ListUtil;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Aspect
@Component
@Order(2)
public class OshUserEventAspect {
    private static final Logger logger = LoggerFactory.getLogger(OshUserEventAspect.class);
    private static final ExpressionParser EXPRESSION_PARSER = new SpelExpressionParser();

    @Autowired
    private RedisCache redisCache;

    @Around("@annotation(oshUserEvent)")
    public Object userAction(ProceedingJoinPoint joinPoint,
                             com.backstage.common.annotation.OshUserEvent oshUserEvent) throws Throwable {
        long startMillis = System.currentTimeMillis();
        Long userId = ThreadLocalUtil.get(OshUserConstants.USER_ID, Long.class);
        String username = ThreadLocalUtil.get(OshUserConstants.USERNAME, String.class);
        String roleCode = ThreadLocalUtil.get(OshUserConstants.ROLE_CODE, String.class);
        Integer roleLevel = parseRoleLevel(ThreadLocalUtil.get(OshUserConstants.LEVEL, String.class));
        List<Long> resourceIds = ThreadLocalUtil.get(OshResourceConstants.RESOURCE_ID, List.class);

        Map<String, Object> userMap = userId == null ? null : redisCache.getCacheObject(OshUserConstants.LOGIN_USER + userId);
        if (userMap == null) {
            Long softUserId = readUserIdFromToken();
            if (softUserId != null) {
                Map<String, Object> softUserMap = redisCache.getCacheObject(OshUserConstants.LOGIN_USER + softUserId);
                if (softUserMap != null) {
                    userId = softUserId;
                    userMap = softUserMap;
                    username = username == null ? readUsernameFromToken() : username;
                    roleCode = roleCode == null ? readRoleCode(userMap) : roleCode;
                    roleLevel = roleLevel == null ? readRoleLevel(userMap) : roleLevel;
                }
            }
        }
        if ((userId == null || userMap == null) && !oshUserEvent.recordAnonymous()) {
            return joinPoint.proceed();
        }

        OshUserEvent event = new OshUserEvent(
                GenerateUtil.generateSnowflakeId(),
                userId,
                username,
                roleCode,
                roleLevel,
                oshUserEvent.module(),
                joinPoint.getSignature().getName(),
                oshUserEvent.actionType(),
                ListUtil.listToString(resourceIds),
                oshUserEvent.resourceType(),
                null,
                oshUserEvent.description(),
                null,
                null,
                readCurrentPoints(userMap),
                LocalDateTime.now()
        );
        event.setContribution(oshUserEvent.contribution() ? 1 : 0);
        fillRequestInfo(event);

        Object result = null;
        try {
            result = joinPoint.proceed();
            event.setResultCode(readResultCode(result));
            event.setStatus(isSuccessfulResult(result) ? ResultCode.SUCCESS.getMsg() : ResultCode.FAILED.getMsg());
            refillUserInfo(event, result);
            refillResourceIds(event);
            applyExpressionFields(event, joinPoint, result, oshUserEvent);
            return result;
        } catch (Throwable e) {
            event.setStatus(ResultCode.FAILED.getMsg());
            event.setException(trim(e.getMessage(), 1024));
            throw e;
        } finally {
            event.setDurationMs(System.currentTimeMillis() - startMillis);
            if (shouldSend(oshUserEvent, joinPoint, result, event)) {
                logger.info("user behavior event: {}", event);
                final OshUserEvent finalEvent = event;
                final String topic = oshUserEvent.topic();
                CompletableFuture.runAsync(() ->
                        KafkaMessageUtil.sendMessage(topic, JSON.toJSONString(finalEvent))
                ).exceptionally(ex -> {
                    logger.error("async send user behavior event failed, topic: {}, reason: {}", topic, ex.getMessage(), ex);
                    return null;
                });
            }
        }
    }

    private boolean shouldSend(com.backstage.common.annotation.OshUserEvent annotation,
                               ProceedingJoinPoint joinPoint, Object result, OshUserEvent event) {
        if (annotation.successOnly() && !ResultCode.SUCCESS.getMsg().equals(event.getStatus())) {
            return false;
        }
        if (annotation.recordConditionExpression() == null || annotation.recordConditionExpression().trim().isEmpty()) {
            return true;
        }
        Object condition = evaluate(annotation.recordConditionExpression(), joinPoint, result);
        return Boolean.TRUE.equals(condition) || "true".equalsIgnoreCase(String.valueOf(condition));
    }

    private Long readCurrentPoints(Map<String, Object> userMap) {
        if (userMap == null) {
            return null;
        }
        Object assetObj = userMap.get(OshUserConstants.ASSET);
        if (!(assetObj instanceof Map)) {
            return null;
        }
        Object pointsObj = ((Map<?, ?>) assetObj).get(OshUserConstants.POINTS);
        if (pointsObj == null) {
            return null;
        }
        try {
            return Long.valueOf(pointsObj.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer parseRoleLevel(String level) {
        try {
            return level == null ? null : Integer.valueOf(level);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Long readUserIdFromToken() {
        try {
            HttpServletRequest request = ServletUtils.getRequest();
            if (request == null) {
                return null;
            }
            String token = request.getHeader(OshUserConstants.TOKEN);
            return token == null || token.isEmpty() ? null : JwtUtil.getUserIdByToken(token);
        } catch (Exception ignored) {
            return null;
        }
    }

    private String readUsernameFromToken() {
        try {
            HttpServletRequest request = ServletUtils.getRequest();
            if (request == null) {
                return null;
            }
            String token = request.getHeader(OshUserConstants.TOKEN);
            if (token == null || token.isEmpty()) {
                return null;
            }
            Object username = JwtUtil.parseToken(token).get(OshUserConstants.USERNAME);
            return username == null ? null : username.toString();
        } catch (Exception ignored) {
            return null;
        }
    }

    private String readRoleCode(Map<String, Object> userMap) {
        Map<?, ?> role = readNestedMap(userMap, OshUserConstants.ROLE);
        Object roleCode = role == null ? null : role.get(OshUserConstants.ROLE_CODE);
        return roleCode == null ? null : roleCode.toString();
    }

    private Integer readRoleLevel(Map<String, Object> userMap) {
        Map<?, ?> role = readNestedMap(userMap, OshUserConstants.ROLE);
        Object level = role == null ? null : role.get(OshUserConstants.LEVEL);
        return level == null ? null : parseRoleLevel(level.toString());
    }

    private void fillRequestInfo(OshUserEvent event) {
        try {
            HttpServletRequest request = ServletUtils.getRequest();
            if (request == null) {
                return;
            }
            event.setRequestUri(request.getRequestURI());
            event.setRequestMethod(request.getMethod());
            event.setIp(IpUtils.getIpAddr(request));
            event.setUserAgent(trim(request.getHeader("User-Agent"), 512));
            String traceId = request.getHeader("X-Trace-Id");
            if (traceId == null || traceId.isEmpty()) {
                traceId = request.getHeader("X-Request-Id");
            }
            event.setTraceId(trim(traceId, 128));
        } catch (Exception ignored) {
            // Request context may not exist for non-web invocations.
        }
    }

    private String readResultCode(Object result) {
        if (result == null) {
            return null;
        }
        try {
            Object code = result.getClass().getMethod("getCode").invoke(result);
            return code == null ? null : code.toString();
        } catch (Exception e) {
            return null;
        }
    }

    private boolean isSuccessfulResult(Object result) {
        if (result == null) {
            return true;
        }
        String code = readResultCode(result);
        if (code != null) {
            return "200".equals(code) || "1000".equals(code) || "0".equals(code);
        }
        try {
            Object msg = result.getClass().getMethod("getMsg").invoke(result);
            return msg != null && ResultCode.SUCCESS.getMsg().equals(msg.toString());
        } catch (Exception e) {
            return true;
        }
    }

    private void refillUserInfo(OshUserEvent event, Object result) {
        if (event.getUserId() != null || result == null) {
            return;
        }
        Object data = readBeanProperty(result, "getData");
        Long userId = parseLong(readBeanProperty(data, "getUserId"));
        if (userId == null) {
            userId = parseLong(readMapValue(data, OshUserConstants.USER_ID));
        }
        if (userId == null) {
            return;
        }
        event.setUserId(userId);
        Object username = readBeanProperty(data, "getUsername");
        if (username == null) {
            username = readMapValue(data, OshUserConstants.USERNAME);
        }
        event.setUsername(username == null ? event.getUsername() : username.toString());
        Map<String, Object> userMap = redisCache.getCacheObject(OshUserConstants.LOGIN_USER + userId);
        event.setRole(event.getRole() == null ? readRoleCode(userMap) : event.getRole());
        event.setRoleLevel(event.getRoleLevel() == null ? readRoleLevel(userMap) : event.getRoleLevel());
        event.setCurrentPoint(event.getCurrentPoint() == null ? readCurrentPoints(userMap) : event.getCurrentPoint());
    }

    private Object readBeanProperty(Object bean, String getterName) {
        if (bean == null) {
            return null;
        }
        try {
            return bean.getClass().getMethod(getterName).invoke(bean);
        } catch (Exception e) {
            return null;
        }
    }

    private Object readMapValue(Object value, String key) {
        if (!(value instanceof Map)) {
            return null;
        }
        return ((Map<?, ?>) value).get(key);
    }

    private Map<?, ?> readNestedMap(Map<String, Object> source, String key) {
        if (source == null) {
            return null;
        }
        Object value = source.get(key);
        return value instanceof Map ? (Map<?, ?>) value : null;
    }

    private Long parseLong(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return Long.valueOf(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void refillResourceIds(OshUserEvent event) {
        if (event.getResourceId() != null && !event.getResourceId().isEmpty()) {
            return;
        }
        List<Long> resourceIds = ThreadLocalUtil.get(OshResourceConstants.RESOURCE_ID, List.class);
        event.setResourceId(ListUtil.listToString(resourceIds));
    }

    private void applyExpressionFields(OshUserEvent event, ProceedingJoinPoint joinPoint, Object result,
                                       com.backstage.common.annotation.OshUserEvent annotation) {
        Object resourceType = evaluate(annotation.resourceTypeExpression(), joinPoint, result);
        if (resourceType != null) {
            event.setResourceType(trim(resourceType.toString(), 64));
        }
        Object resourceId = evaluate(annotation.resourceIdExpression(), joinPoint, result);
        if (resourceId != null) {
            event.setResourceId(objectToCsv(resourceId));
        }
        Object resourceName = evaluate(annotation.resourceNameExpression(), joinPoint, result);
        if (resourceName != null) {
            event.setResourceName(trim(resourceName.toString(), 255));
        }
    }

    private Object evaluate(String expression, ProceedingJoinPoint joinPoint, Object result) {
        if (expression == null || expression.trim().isEmpty()) {
            return null;
        }
        try {
            EvaluationContext context = new StandardEvaluationContext();
            Object[] args = joinPoint.getArgs();
            context.setVariable("args", args);
            context.setVariable("result", result);
            for (int i = 0; i < args.length; i++) {
                context.setVariable("p" + i, args[i]);
                context.setVariable("a" + i, args[i]);
            }
            if (joinPoint.getSignature() instanceof MethodSignature) {
                String[] parameterNames = ((MethodSignature) joinPoint.getSignature()).getParameterNames();
                if (parameterNames != null) {
                    for (int i = 0; i < parameterNames.length && i < args.length; i++) {
                        context.setVariable(parameterNames[i], args[i]);
                    }
                }
            }
            return EXPRESSION_PARSER.parseExpression(expression).getValue(context);
        } catch (Exception e) {
            logger.warn("failed to evaluate OshUserEvent expression: {}", expression, e);
            return null;
        }
    }

    private String objectToCsv(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Collection) {
            StringBuilder builder = new StringBuilder();
            for (Object item : (Collection<?>) value) {
                if (item == null) {
                    continue;
                }
                if (builder.length() > 0) {
                    builder.append(',');
                }
                builder.append(item);
            }
            return builder.toString();
        }
        return value.toString();
    }

    private String trim(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
