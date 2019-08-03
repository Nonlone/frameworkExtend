package per.nonlone.frameworkExtend.apollo.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * apollo更新回调(类上的namespace更新时回调,属性上的元素变动时回调)
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE,ElementType.FIELD})
public @interface ApolloAutoCallBack {
    Class<?> callBack();
}
