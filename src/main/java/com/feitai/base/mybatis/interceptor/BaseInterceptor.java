package com.feitai.base.mybatis.interceptor;

import com.feitai.utils.ObjectUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.statement.BaseStatementHandler;
import org.apache.ibatis.executor.statement.RoutingStatementHandler;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Invocation;

import java.util.Objects;

/**
 * 拦截处理类
 */
@Slf4j
public abstract  class BaseInterceptor {

    /**
     * 获取 StatementHandler接口
     * @param invocation
     * @return
     */
    protected StatementHandler getStatementHandlerFromStatementHandler(Invocation invocation){
        if(invocation.getTarget() instanceof StatementHandler) {
            return (StatementHandler) invocation.getTarget();
        }
        return null;
    }

    protected MappedStatement getMappedStatementFromStatementHandler(Invocation invocation){
        StatementHandler target = getStatementHandlerFromStatementHandler(invocation);
        if (target instanceof RoutingStatementHandler) {
            RoutingStatementHandler handler = (RoutingStatementHandler) invocation.getTarget();
            StatementHandler delegate = (StatementHandler) ObjectUtils.getFieldValue(handler, "delegate");
            return (MappedStatement) ObjectUtils.getFieldValue(delegate, "mappedStatement");
        }else if( target instanceof BaseStatementHandler){
            return (MappedStatement) ObjectUtils.getFieldValue(target, "mappedStatement");
        }
        return null;
    }


    /**
     * 获取Mapper Class
     *
     * @param invocation
     * @return
     * @throws ClassNotFoundException
     */
    protected Class<?> getMapperClassFromExecutor(Invocation invocation) {
        MappedStatement mappedStatement =(MappedStatement) invocation.getArgs()[0];
        if(Objects.nonNull(mappedStatement)){
            String mapperClassPath = mappedStatement.getId().substring(0, mappedStatement.getId().lastIndexOf("."));
            try {
                Class<?> mapperClass = Class.forName(mapperClassPath);
                return mapperClass;
            } catch (ClassNotFoundException cnfe) {
                log.error(String.format("mappedStatement.Id<%s> mapperClassPath<%s>", mappedStatement.getId(), mapperClassPath), cnfe);
            }
        }
        return null;
    }

}
