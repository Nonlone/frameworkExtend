package com.feitai.base.mybatis.multisource;

import com.feitai.base.mybatis.MultipleDataSource;
import com.feitai.base.mybatis.interceptor.ConnectionSignature;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.jdbc.datasource.DataSourceUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class ClassPrefixMyBatisDataSourceSelector extends BaseMyBatisDataSourceSelector {

    /**
     * 类前缀数据池Key映射
     */
    private Map<String, String> classPrefixKeyMap;

    private Map<Class<?>, String> classKeyMap = new HashMap<>();

    public ClassPrefixMyBatisDataSourceSelector(MultipleDataSource multipleDataSource, ConcurrentHashMap<String, ConnectionSignature> connectionSignatureMap, Map<String, String> classPrefixKeyMap) {
        super(multipleDataSource, connectionSignatureMap);
        this.classPrefixKeyMap = classPrefixKeyMap;
    }


    @Override
    public DataSource getDataSource(Class<?> mapperClass, Connection connection) {
        // 新数据源
        Connection signConnection = null;
        DataSource dataSource = null;
        try {
            String key = getKey(mapperClass);
            if (StringUtils.isBlank(key)) {
                //Key为空 直接返回
                return null;
            }
            if (log.isDebugEnabled()) {
                log.debug("getDataSource key<{}>", key);
            }
            if (Objects.isNull(multipleDataSource.getDataSouce(key))) {
                // 不存在对应数据源
                return null;
            }
            dataSource = multipleDataSource.getDataSouce(key);
            signConnection = dataSource.getConnection();
            ConnectionSignature newConnectionSingSignature = new ConnectionSignature(signConnection);

            // 替换数据源
            if (!multipleDataSource.getDataSourceKey().equals(key)
                    || multipleDataSource.getDataSourceKey().equals(key) && !newConnectionSingSignature.equals(new ConnectionSignature(connection))) {
                // ThreadLocal 不同，直接返回 或者 key 相同，检查连接签名不同
                dataSourceThreadLocal.set(dataSource);
                return dataSource;
            }
            return null;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            if (ObjectUtils.allNotNull(signConnection, dataSource)) {
                // 释放 签名连接
                DataSourceUtils.releaseConnection(signConnection, dataSource);
            }
        }
    }

    /**
     * 根据类获取Key
     *
     * @param classOfT
     * @return
     */
    private String getKey(Class<?> classOfT) {
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


    /**
     * 获取指定类数据源
     *
     * @param classOfT
     * @return
     */
    @Override
    public DataSource getDataSource(Class<?> classOfT) {
        String key = getKey(classOfT);
        if (StringUtils.isNotBlank(key) && multipleDataSource.hasDataSourceKey(key)) {
            return multipleDataSource.getDataSouce(key);
        }
        return null;
    }

    /**
     * 获取指定类数据源
     *
     * @param classOfT
     * @return
     */
    public String getDataSourceKey(Class<?> classOfT) {
        String key = getKey(classOfT);
        if (StringUtils.isNotBlank(key) && multipleDataSource.hasDataSourceKey(key)) {
            return key;
        }
        return null;
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
            this.dotsOfClassName = com.feitai.utils.StringUtils.countMatches(className, ".");
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
