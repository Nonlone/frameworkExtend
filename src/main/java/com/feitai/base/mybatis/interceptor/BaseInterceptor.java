package com.feitai.base.mybatis.interceptor;

import com.feitai.utils.ObjectUtils;
import org.apache.ibatis.executor.statement.BaseStatementHandler;
import org.apache.ibatis.executor.statement.RoutingStatementHandler;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Invocation;

/**
 * 拦截处理类
 */
public abstract  class BaseInterceptor {

    /**
     * 获取 StatementHandler接口
     * @param invocation
     * @return
     */
    protected StatementHandler getStatementHandler(Invocation invocation){
        if(invocation.getTarget() instanceof StatementHandler) {
            return (StatementHandler) invocation.getTarget();
        }
        return null;
    }

    protected MappedStatement getMappedStatement(Invocation invocation){
        StatementHandler target = getStatementHandler(invocation);
        if (target instanceof RoutingStatementHandler) {
            RoutingStatementHandler handler = (RoutingStatementHandler) invocation.getTarget();
            StatementHandler delegate = (StatementHandler) ObjectUtils.getFieldValue(handler, "delegate");
            return (MappedStatement) ObjectUtils.getFieldValue(delegate, "mappedStatement");
        }else if( target instanceof BaseStatementHandler){
            return (MappedStatement) ObjectUtils.getFieldValue(target, "mappedStatement");
        }
        return null;
    }

}
