package com.feitai.base.mybatis;

import tk.mybatis.mapper.common.Mapper;

import java.lang.annotation.*;

/**
 * 外键应用多个引用
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface Many {

    Class<? extends Mapper<?>> classOfMapper();

    Class<?> classOfEntity();

    String sourceField() default "id";

    String targetField() default "id";


}
