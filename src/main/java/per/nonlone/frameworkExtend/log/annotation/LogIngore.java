package per.nonlone.frameworkExtend.log.annotation;

import java.lang.annotation.*;

/**
 * 日志输出注解，忽略参数
 */
@Target({ElementType.PARAMETER})
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
public @interface LogIngore {
}
