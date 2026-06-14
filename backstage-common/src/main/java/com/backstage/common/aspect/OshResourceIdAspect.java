package com.backstage.common.aspect;

import com.backstage.common.annotation.OshResourceId;
import com.backstage.common.constant.OshResourceConstants;
import com.backstage.common.threadlocal.ThreadLocalUtil;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Aspect
@Component
@Order(1)
public class OshResourceIdAspect {
    private static final Logger logger = LoggerFactory.getLogger(OshResourceIdAspect.class);

    @Before("execution(* *(..)) && @within(org.springframework.web.bind.annotation.RestController)")
    public void getResourceNo(JoinPoint joinPoint) {
        List<Long> resourceIds = resolveResourceIds(joinPoint);
        if (!resourceIds.isEmpty()) {
            ThreadLocalUtil.set(OshResourceConstants.RESOURCE_ID, resourceIds);
            return;
        }
        ThreadLocalUtil.remove(OshResourceConstants.RESOURCE_ID);
    }

    private List<Long> resolveResourceIds(JoinPoint joinPoint) {
        List<Long> result = new ArrayList<>();
        Object[] args = joinPoint.getArgs();
        if (args == null || args.length == 0) {
            return result;
        }

        if (joinPoint.getSignature() instanceof MethodSignature) {
            Annotation[][] parameterAnnotations = ((MethodSignature) joinPoint.getSignature()).getMethod().getParameterAnnotations();
            for (int i = 0; i < parameterAnnotations.length && i < args.length; i++) {
                if (hasResourceId(parameterAnnotations[i])) {
                    addValues(result, args[i]);
                }
            }
            if (!result.isEmpty()) {
                return result;
            }
        }

        for (Object arg : args) {
            collectFromFields(result, arg);
            if (!result.isEmpty()) {
                return result;
            }
        }
        return result;
    }

    private boolean hasResourceId(Annotation[] annotations) {
        if (annotations == null) {
            return false;
        }
        for (Annotation annotation : annotations) {
            if (annotation instanceof OshResourceId) {
                return true;
            }
        }
        return false;
    }

    private void collectFromFields(List<Long> result, Object arg) {
        if (arg == null || isSimpleValue(arg)) {
            return;
        }
        Class<?> type = arg.getClass();
        while (type != null && !Object.class.equals(type)) {
            for (Field field : type.getDeclaredFields()) {
                if (!field.isAnnotationPresent(OshResourceId.class)) {
                    continue;
                }
                field.setAccessible(true);
                try {
                    addValues(result, field.get(arg));
                } catch (IllegalAccessException e) {
                    logger.error("Failed to get resource id field value", e);
                }
            }
            type = type.getSuperclass();
        }
    }

    private boolean isSimpleValue(Object value) {
        return value instanceof Number || value instanceof CharSequence || value instanceof Boolean || value instanceof Character;
    }

    private void addValues(List<Long> result, Object value) {
        if (value == null) {
            return;
        }
        if (value instanceof Collection) {
            for (Object item : (Collection<?>) value) {
                addOne(result, item);
            }
            return;
        }
        if (value.getClass().isArray()) {
            int length = Array.getLength(value);
            for (int i = 0; i < length; i++) {
                addOne(result, Array.get(value, i));
            }
            return;
        }
        addOne(result, value);
    }

    private void addOne(List<Long> result, Object value) {
        if (value == null) {
            return;
        }
        try {
            result.add(Long.valueOf(value.toString()));
        } catch (NumberFormatException e) {
            logger.warn("Resource id value is not numeric: {}", value);
        }
    }
}
