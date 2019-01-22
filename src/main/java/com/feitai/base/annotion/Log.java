package com.feitai.base.annotion;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.slf4j.event.Level;

/**
 * 日志输出注解
 *  @author linguocheng
 *  @date 2019年1月18日
 */
@Target({ElementType.METHOD})
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
public @interface Log {

	
	
	Level level() default Level.INFO;
	
	/**
	 * 日志输出的通知位置
	 * @return
	 */
	LogLocation[] logLocation() default {LogLocation.AROUND};
}
