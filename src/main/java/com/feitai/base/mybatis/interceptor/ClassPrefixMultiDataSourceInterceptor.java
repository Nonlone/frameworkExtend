package com.feitai.base.mybatis.interceptor;

import com.feitai.base.mybatis.MultipleDataSource;
import com.feitai.base.mybatis.SqlMapper;
import com.feitai.utils.StringUtils;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;
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
public class ClassPrefixMultiDataSourceInterceptor extends AbstractMultiDataSourceInterceptor {

    /**
     * 类前缀数据池Key映射
     */
    private Map<String, String> classPrefixKeyMap;

    private Map<Class<?>, String> classKeyMap = new HashMap<>();

    public ClassPrefixMultiDataSourceInterceptor(MultipleDataSource multipleDataSource, ConcurrentHashMap<String, ConnectionSignature> connectionSignatureMap, Map<String, String> classPrefixKeyMap) {
        super(multipleDataSource, connectionSignatureMap);
        this.classPrefixKeyMap = classPrefixKeyMap;
    }


    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        if (multipleDataSource == null) {
            log.error("op<intercept> multipleDataSource is null");
            Object result = invocation.proceed();
            return result;
        }
        if (log.isDebugEnabled()) {
            log.debug("op<intercept> before Invocation.proceed()");
        }
        Class<?> mapperClass = getMapperClass(invocation);
        if (mapperClass == null) {
            log.warn("op<intercept> mapperClass is null");
            Object result = invocation.proceed();
            return result;
        }
        if (SqlMapper.class.equals(mapperClass)) {
            if (log.isDebugEnabled()) {
                log.debug("op<intercept> SqlMapper not to intercept");
            }
            Object result = invocation.proceed();
            return result;
        }
        String key = getKey(mapperClass);
        // 替换数据源
        if (StringUtils.isNotBlank(key)) {
            multipleDataSource.setDataSourceKey(key);
        }
        Object result = invocation.proceed();
        if (log.isDebugEnabled()) {
            log.debug("op<intercept> after Invocation.proceed()");
        }
        return result;
    }

    /**
     * 根据类获取Key
     *
     * @param classOfT
     * @return
     */
    public String getKey(Class<?> classOfT) {
        String key = null;
        boolean isCheckClassPrefixMap = true;
        // 查询缓存类映射
        if (classKeyMap.containsKey(classOfT)) {
            isCheckClassPrefixMap = false;
            key = classKeyMap.get(classOfT);
        }
        // 查询前缀映射
        if (isCheckClassPrefixMap) {
            ClassPrefixMatcher classPrefixMatcher = null;
            String mapperClassString = classOfT.getName();
            for (Map.Entry<String, String> entry : classPrefixKeyMap.entrySet()) {
                if (mapperClassString.startsWith(entry.getKey())) {
                    ClassPrefixMatcher tempMatcher = new ClassPrefixMatcher(entry.getKey());
                    if (tempMatcher.matchThan(classPrefixMatcher)) {
                        classPrefixMatcher = tempMatcher;
                        key = entry.getValue();
                        // 缓存到映射
                        classKeyMap.put(classOfT, entry.getValue());
                    }
                }
            }
        }
        return key;
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {

    }

    /**
     * 类前缀比较类
     */
    private class ClassPrefixMatcher {
        /**
         * ClassName点数
         */
        private int dotsOfClassName;

        /**
         * ClassName长度数
         */
        private int lengthOfCalssName;

        public ClassPrefixMatcher(String className) {
            this.dotsOfClassName = StringUtils.countMatches(className, ".");
            this.lengthOfCalssName = className.length();
        }

        public ClassPrefixMatcher(int dotsOfClassName, int lengthOfCalssName) {
            this.dotsOfClassName = dotsOfClassName;
            this.lengthOfCalssName = lengthOfCalssName;
        }

        /*
         * 比较两个Matcher
         */
        public boolean matchThan(ClassPrefixMatcher matcher) {
            if (matcher == null) {
                return true;
            }
            if (this.dotsOfClassName > matcher.dotsOfClassName) {
                return true;
            }
            if (this.lengthOfCalssName > matcher.lengthOfCalssName) {
                return true;
            }
            return false;
        }
    }

}
