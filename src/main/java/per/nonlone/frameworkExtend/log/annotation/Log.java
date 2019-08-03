package per.nonlone.frameworkExtend.log.annotation;

import org.slf4j.event.Level;
import per.nonlone.frameworkExtend.log.LogPoint;

import java.lang.annotation.*;

/**
 * 日志输出注解
 */
@Target({ElementType.METHOD})
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
public @interface Log {

    /**
     * 记录返回，如果为false，只记录返回类名
     *
     * @return
     */
    boolean isLogReturn() default true;

    /**
     * 日志级别
     *
     * @return
     */
    Level level() default Level.INFO;

    /**
     * 日志输出的通知位置
     *
     * @return
     */
    LogPoint[] logPoint() default {LogPoint.AROUND, LogPoint.THROWING};
}
