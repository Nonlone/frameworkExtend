package com.feitai.base.annotion;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * apollo 设置不自动刷新标签(只能再元素属性上使用,配合ApolloAutoChange使用)
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ApolloNotChange {
}
