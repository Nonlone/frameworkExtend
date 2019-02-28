package per.nonlone.frameworkExtend.mybatis.interceptor;

import per.nonlone.frameworkExtend.mybatis.MultipleDataSource;
import per.nonlone.frameworkExtend.mybatis.annotation.DataSource;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MyBatis 自动分配数据库连接插件，需要注入多数据池数据源
 */
@Slf4j
@Data
@Intercepts({@Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class}),
        @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),
        @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class,RowBounds.class,ResultHandler.class, CacheKey.class, BoundSql.class})
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
        Class<?> mapperClass = getMapperClassFromExecutor(invocation);
        if (mapperClass == null) {
            log.warn("op<intercept> mapperClass is null");
            Object result = invocation.proceed();
            return result;
        }
        if (!mapperClass.isAnnotationPresent(DataSource.class)) {
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
            String dataSourceKey = dataSource.value();
            if (!dataSourceKey.equals(multipleDataSource.getDataSourceKey())) {
                multipleDataSource.setDataSourceKey(dataSourceKey);
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
