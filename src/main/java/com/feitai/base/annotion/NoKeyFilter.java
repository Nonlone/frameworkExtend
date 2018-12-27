package com.feitai.base.annotion;

import java.lang.annotation.*;

/**
 * 该类，成员变量 忽略KeyFilter
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(value = {ElementType.TYPE,ElementType.FIELD})
public @interface NoKeyFilter {
}
