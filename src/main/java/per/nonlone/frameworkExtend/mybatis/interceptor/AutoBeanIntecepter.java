package per.nonlone.frameworkExtend.mybatis.interceptor;

import per.nonlone.frameworkExtend.mybatis.annotation.AutoBeanHandler;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;

import java.sql.Connection;
import java.util.Properties;


@Slf4j
@Intercepts({
        @Signature(type = StatementHandler.class, method = "prepare", args = {Connection.class, Integer.class})
})
public class AutoBeanIntecepter extends BaseInterceptor  {

    private AutoBeanHandler autoBeanHandler;

    /**
     * 构造注入
     *
     * @param autoBeanHandler
     */
    public AutoBeanIntecepter(@NonNull AutoBeanHandler autoBeanHandler) {
        this.autoBeanHandler = autoBeanHandler;
    }

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        if (log.isDebugEnabled()) {
            log.debug("AutoBeanIntecepter intercept before Invocation.proceed()");
        }
        StatementHandler handler = getStatementHandlerFromStatementHandler(invocation);
        BoundSql boundSql = handler.getBoundSql();
        Object object = boundSql.getParameterObject();
        Class<?> constrainClass = autoBeanHandler.getAutoBeanConstraintClass();
        if (constrainClass != null && constrainClass.isInstance(object)) {
            autoBeanHandler.handleBoundSqlAndParameterObject(boundSql, constrainClass.cast(object));
        }
        Object result = invocation.proceed();
        log.debug("op<intercept> after Invocation.proceed()");
        return result;
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {

    }
}
