package per.nonlone.framework.mybatis;

import tk.mybatis.mapper.common.Mapper;

import java.lang.annotation.*;

/**
 * 外键应用单个实体
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface One {

    Class<? extends Mapper<?>> classOfMapper();

    String sourceField() default "id";

    String targetField() default "id";


}
