package com.feitai.base.annotion;

import org.slf4j.event.Level;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

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

	/**
	 * 记录时间，仅当 LogPoint.AROUND 时候生效
	 * @return
	 */
	boolean isStopWatch() default false;

	/**
	 * 记录时间单位
	 * @return
	 */
	TimeUnit stopWatchUnit() default TimeUnit.SECONDS;

	/**
	 * 日志级别
	 * @return
	 */
	Level level() default Level.INFO;
	
	/**
	 * 日志输出的通知位置
	 * @return
	 */
	LogPoint[] logPoint() default {LogPoint.AROUND, LogPoint.THROWING};
}
