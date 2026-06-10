package com.backstage.system.controller.book;

import com.backstage.system.domain.vo.book.BookRelationReqVO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.validation.annotation.Validated;

import javax.validation.ConstraintViolation;
import javax.validation.Valid;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.constraints.NotNull;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

public class UserBookRelationValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    public void shouldEnableBeanValidationOnControllerMethods() {
        Assertions.assertTrue(UserBookRelationController.class.isAnnotationPresent(Validated.class),
                "Controller 应启用 @Validated");

        assertRequestBodyValidated(getMethod("favorite"));
        assertRequestBodyValidated(getMethod("follow"));
        assertRequestBodyValidated(getMethod("purchase"));

        NotNull notNull = getMethod("status").getParameters()[0].getAnnotation(NotNull.class);
        Assertions.assertNotNull(notNull, "status 接口的 bookId 参数应使用 @NotNull");
        Assertions.assertEquals("电子书ID不能为空", notNull.message());
    }

    @Test
    public void shouldRequireBookIdForPurchaseRequest() {
        BookRelationReqVO reqVO = new BookRelationReqVO();

        Set<String> messages = validateMessages(reqVO);

        Assertions.assertTrue(messages.contains("电子书ID不能为空"));
    }

    @Test
    public void shouldRequireValidStatusForRelationActionRequest() throws Exception {
        Object reqVO = newRelationActionRequest();
        invokeSetter(reqVO, "setBookId", Long.class, 1L);

        Assertions.assertTrue(validateMessages(reqVO).contains("status 参数无效，请传 0 或 1"));

        invokeSetter(reqVO, "setStatus", Integer.class, 2);
        Assertions.assertTrue(validateMessages(reqVO).contains("status 参数无效，请传 0 或 1"));
    }

    @Test
    public void shouldAllowPurchaseRequestWithoutStatus() {
        BookRelationReqVO reqVO = new BookRelationReqVO();
        reqVO.setBookId(1L);

        Assertions.assertTrue(validateMessages(reqVO).isEmpty());
    }

    private void assertRequestBodyValidated(Method method) {
        Annotation[] annotations = method.getParameterAnnotations()[0];
        boolean hasValidationAnnotation = false;
        for (Annotation annotation : annotations) {
            if (annotation.annotationType() == Valid.class || annotation.annotationType() == Validated.class) {
                hasValidationAnnotation = true;
                break;
            }
        }
        Assertions.assertTrue(hasValidationAnnotation, method.getName() + " 入参应使用注解校验");
    }

    private Method getMethod(String methodName) {
        for (Method method : UserBookRelationController.class.getDeclaredMethods()) {
            if (method.getName().equals(methodName)) {
                return method;
            }
        }
        Assertions.fail("未找到方法: " + methodName);
        return null;
    }

    private Object newRelationActionRequest() throws Exception {
        try {
            Class<?> clazz = Class.forName("com.backstage.system.domain.vo.book.BookRelationActionReqVO");
            return clazz.getDeclaredConstructor().newInstance();
        } catch (ClassNotFoundException e) {
            Assertions.fail("应提供收藏/关注专用请求对象，以便通过注解约束 status");
            return null;
        }
    }

    private void invokeSetter(Object target, String methodName, Class<?> parameterType, Object value) throws Exception {
        target.getClass().getMethod(methodName, parameterType).invoke(target, value);
    }

    private Set<String> validateMessages(Object target) {
        Set<String> messages = new HashSet<>();
        Set<ConstraintViolation<Object>> violations = validator.validate(target);
        for (ConstraintViolation<Object> violation : violations) {
            messages.add(violation.getMessage());
        }
        return messages;
    }
}
