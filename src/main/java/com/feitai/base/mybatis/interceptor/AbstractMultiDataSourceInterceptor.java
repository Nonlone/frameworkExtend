package com.feitai.base.mybatis.interceptor;

import com.feitai.base.mybatis.MultipleDataSource;
import com.feitai.utils.ObjectUtils;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.statement.RoutingStatementHandler;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Invocation;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Mybatis 多数据源抽象拦截器
 */
@Slf4j
public abstract class AbstractMultiDataSourceInterceptor implements Interceptor {


    @Getter
    protected MultipleDataSource multipleDataSource;


    protected AbstractMultiDataSourceInterceptor(MultipleDataSource multipleDataSource) {
        this.multipleDataSource = multipleDataSource;
    }

    /**
     * 获取Mapper Class
     *
     * @param invocation
     * @return
     * @throws ClassNotFoundException
     */
    protected Class<?> getMapperClass(Invocation invocation)  {
        Object target = invocation.getTarget();
        if(target instanceof RoutingStatementHandler ){
            RoutingStatementHandler handler = (RoutingStatementHandler) invocation.getTarget();
            StatementHandler delegate = (StatementHandler) ObjectUtils.getFieldValue(handler, "delegate");
            MappedStatement mappedStatement = (MappedStatement) ObjectUtils.getFieldValue(delegate, "mappedStatement");
            String mapperClassPath = mappedStatement.getId().substring(0, mappedStatement.getId().lastIndexOf("."));
            try {
                Class<?> mapperClass = Class.forName(mapperClassPath);
                return mapperClass;
            }catch (ClassNotFoundException cnfe){
                log.error(String.format("mappedStatement.Id<%s> mapperClassPath<%s>",mappedStatement.getId(),mapperClassPath),cnfe);
            }
        }
        return null;
    }

    /**
     * 设置连接
     *
     * @param invocation
     * @param key
     * @throws SQLException
     */
    protected void setConnection(Invocation invocation, String key) throws SQLException {
        if (multipleDataSource.hasDataSourceKey(key)) {
            multipleDataSource.setDataSourceKey(key);
            Connection conn = multipleDataSource.getConnection();
            Object[] args = invocation.getArgs();
            args[0] = conn;
            ObjectUtils.setFieldValue(invocation, "args", args);
            if (log.isDebugEnabled()) {
                log.debug("op<intercept> change to datasource key: " + key);
            }
        }
    }
}
