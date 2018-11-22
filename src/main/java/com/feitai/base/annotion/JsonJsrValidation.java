package com.feitai.base.annotion;

import com.feitai.base.configuration.JsonJsrValidationInit;
import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.*;

/**
 * JSON JSR自动注解校验
 * 采用JSON Path节点校验
 * 需要fastjson支持
 * 适用于元素属性
 */
@Constraint(validatedBy = JsonJsrValidationInit.class) //具体的实现
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(value = {ElementType.FIELD})
public @interface JsonJsrValidation {
    /**
     * {"root","root.detail","root.detail.id"}
     * @return
     */
    String[] value() default {};

    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};

    String message() default "节点不能为空";
}
