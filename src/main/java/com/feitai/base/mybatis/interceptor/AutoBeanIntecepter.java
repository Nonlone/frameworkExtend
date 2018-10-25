package com.feitai.base.mybatis.interceptor;

import com.feitai.base.mybatis.annotation.AutoBeanHandler;
import com.feitai.utils.ObjectUtils;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.statement.RoutingStatementHandler;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.plugin.*;

import java.sql.Connection;
import java.util.Properties;


@Slf4j
@Intercepts({
        @Signature(type = StatementHandler.class, method = "prepare", args = {Connection.class, Integer.class})
})
public class AutoBeanIntecepter implements Interceptor {

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
        RoutingStatementHandler handler = (RoutingStatementHandler) invocation.getTarget();
        StatementHandler delegate = (StatementHandler) ObjectUtils.getFieldValue(handler, "delegate");
        BoundSql boundSql = delegate.getBoundSql();
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
