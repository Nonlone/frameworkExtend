package com.feitai.base.mybatis.multisource;

import com.feitai.base.mybatis.MultipleDataSource;
import com.feitai.base.mybatis.annotation.DataSource;
import com.feitai.base.mybatis.interceptor.ConnectionSignature;
import com.feitai.utils.ObjectUtils;
import com.feitai.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.datasource.DataSourceUtils;
import tk.mybatis.mapper.common.Mapper;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class AnnotationMyBatisDataSourceSelector extends BaseMyBatisDataSourceSelector {


    protected AnnotationMyBatisDataSourceSelector(MultipleDataSource multipleDataSource, ConcurrentHashMap<String, ConnectionSignature> connectionSignatureMap) {
        super(multipleDataSource, connectionSignatureMap);
    }

    /**
     * 根据类获取Key
     *
     * @param classOfT
     * @return
     */
    private String getKey(Class<?> classOfT) {
        String key = null;
        if (!classOfT.isAnnotationPresent(DataSource.class)) {
            if (log.isDebugEnabled()) {
                log.debug("getConnection  Mapper Class does not has DataSource Annotation ");
            }
            return null;
        }
        DataSource dataSource = classOfT.getAnnotation(DataSource.class);
        if (null != dataSource && StringUtils.isNotBlank(dataSource.value())) {
            key = dataSource.value();
        }
        return key;
    }

    @Override
    public javax.sql.DataSource getDataSource(Class<?> mapperClass, Connection connection) {
        String key = getKey(mapperClass);
        if (StringUtils.isNotBlank(key)
                && Objects.nonNull(multipleDataSource.getDataSouce(key))) {
            javax.sql.DataSource sqlDataSource = null;
            Connection signConnection = null;
            try {
                signConnection = sqlDataSource.getConnection();
                if (!key.equals(multipleDataSource.getDataSourceKey())
                        || !new ConnectionSignature(signConnection).equals(new ConnectionSignature(connection))) {
                    dataSourceThreadLocal.set(sqlDataSource);
                    return sqlDataSource;
                }
                return null;
            } catch (SQLException sqle) {
                log.error("getConnection error ", sqle);
            } finally {
                if (ObjectUtils.allNotNull(signConnection, sqlDataSource)) {
                    DataSourceUtils.releaseConnection(signConnection, sqlDataSource);
                }
            }
        }
        return null;
    }

    @Override
    public javax.sql.DataSource getDataSource(Class<?> mapperClass) {
        String key = getKey(mapperClass);
        if(StringUtils.isNotBlank(key)
            && Objects.nonNull(multipleDataSource.getDataSouce(key))){
            return multipleDataSource.getDataSouce(key);
        }
        return null;
    }
}
