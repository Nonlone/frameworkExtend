package per.nonlone.framework.log.annotation;

import java.lang.annotation.*;

@Target({ElementType.METHOD})
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
public @interface LogStopWatch {
}
