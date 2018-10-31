package com.feitai.base.mybatis.interceptor;

import com.feitai.base.mybatis.MultipleDataSource;
import com.feitai.base.mybatis.annotation.DataSource;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;

import java.sql.Connection;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MyBatis 自动分配数据库连接插件，需要注入多数据池数据源
 */
@Slf4j
@Data
@Intercepts({
        @Signature(type = StatementHandler.class, method = "prepare", args = {Connection.class, Integer.class})
})
public class AnnotationMultiDataSourceInterceptor extends AbstractMultiDataSourceInterceptor {

    public AnnotationMultiDataSourceInterceptor(MultipleDataSource multipleDataSource, ConcurrentHashMap<String, ConnectionSignature> connectionSignatureMap) {
        super(multipleDataSource, connectionSignatureMap);
    }

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        if (multipleDataSource == null) {
            log.error("op<intercept> multipleDataSource is null");
            Object result = invocation.proceed();
            return result;
        }
        Class<?> mapperClass = getMapperClass(invocation);
        if (mapperClass == null) {
            log.warn("op<intercept> mapperClass is null");
            Object result = invocation.proceed();
            return result;
        }
        if (mapperClass.isAnnotationPresent(DataSource.class)) {
            if (log.isDebugEnabled()) {
                log.warn("op<intercept>  Mapper Class does not has DataSource Annotation ");
            }
            Object result = invocation.proceed();
            return result;
        }
        if (log.isDebugEnabled()) {
            log.debug("op<intercept> before Invocation.proceed()");
        }
        DataSource dataSource = mapperClass.getAnnotation(DataSource.class);
        if (null != dataSource) {
            String dataSourceKey = dataSource.value().toLowerCase();
            if (!dataSourceKey.equals(multipleDataSource.getDataSourceKey())) {
                setConnection(invocation, dataSourceKey);
            }
        }
        Object result = invocation.proceed();
        if (log.isDebugEnabled()) {
            log.debug("op<intercept> after Invocation.proceed()");
        }
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
