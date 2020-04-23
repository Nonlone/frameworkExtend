package per.nonlone.framework.mybatis.interceptor;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;

import java.sql.Connection;
import java.util.Objects;
import java.util.Properties;
import java.util.function.Function;


@Slf4j
@Intercepts({
        @Signature(type = StatementHandler.class, method = "prepare", args = {Connection.class, Integer.class})
})
public class StatementHandlerInterceptor extends BaseInterceptor {

    private Function<Invocation, HandlerResult> handlerFunction;

    private boolean isAccpetNull;

    /**
     * 构造注入
     */

    public StatementHandlerInterceptor(@NonNull Function<Invocation, HandlerResult> handlerFunction) {
        this(handlerFunction, false);
    }

    public StatementHandlerInterceptor(@NonNull Function<Invocation, HandlerResult> handlerFunction, boolean isAccpetNull) {
        this.handlerFunction = handlerFunction;
        this.isAccpetNull = isAccpetNull;
    }

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        HandlerResult handlerResult = handlerFunction.apply(invocation);
        if (Objects.isNull(handlerResult) && isAccpetNull) {
            return null;
        } else if (Objects.nonNull(handlerResult) && handlerResult.isReplaced()) {
            return handlerResult.getResult();
        }
        return invocation.proceed();
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {

    }

    @Data
    @AllArgsConstructor
    protected static class HandlerResult {
        /**
         * 是否
         */
        private boolean replaced;

        private Object result;

    }
}
