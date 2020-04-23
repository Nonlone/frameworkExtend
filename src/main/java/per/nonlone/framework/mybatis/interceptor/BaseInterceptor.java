package per.nonlone.framework.mybatis.interceptor;

import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.statement.BaseStatementHandler;
import org.apache.ibatis.executor.statement.RoutingStatementHandler;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import per.nonlone.utils.ObjectUtils;

import java.util.Objects;
import java.util.Properties;

/**
 * 拦截处理类
 */
@Slf4j
public abstract class BaseInterceptor implements Interceptor {

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {

    }

    /**
     * 获取 StatementHandler接口
     *
     * @param invocation
     * @return
     */
    protected static StatementHandler getStatementHandlerFromInvocationOfStatementHandler(Invocation invocation) {
        if (invocation.getTarget() instanceof StatementHandler) {
            return (StatementHandler) invocation.getTarget();
        }
        return null;
    }

    protected static MappedStatement getMappedStatementFromStatementHandler(Invocation invocation) {
        StatementHandler target = getStatementHandlerFromInvocationOfStatementHandler(invocation);
        if (target instanceof RoutingStatementHandler) {
            RoutingStatementHandler handler = (RoutingStatementHandler) invocation.getTarget();
            StatementHandler delegate = (StatementHandler) ObjectUtils.getFieldValue(handler, "delegate");
            return (MappedStatement) ObjectUtils.getFieldValue(delegate, "mappedStatement");
        } else if (target instanceof BaseStatementHandler) {
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
    protected static Class<?> getMapperClassFromExecutor(Invocation invocation) {
        MappedStatement mappedStatement = (MappedStatement) invocation.getArgs()[0];
        if (Objects.nonNull(mappedStatement)) {
            return getMapperClassFromMapperStatement(mappedStatement);
        }
        return null;
    }


    /**
     * 获取Mapper Class
     *
     * @param mappedStatement
     * @return
     */
    protected static Class<?> getMapperClassFromMapperStatement(MappedStatement mappedStatement) {
        if (Objects.nonNull(mappedStatement)) {
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
