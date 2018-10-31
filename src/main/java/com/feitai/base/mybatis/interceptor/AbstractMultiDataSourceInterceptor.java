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
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mybatis 多数据源抽象拦截器
 */
@Slf4j
public abstract class AbstractMultiDataSourceInterceptor implements Interceptor {


    @Getter
    protected MultipleDataSource multipleDataSource;

    protected ConcurrentHashMap<String, ConnectionSignature> connectionSignatureMap;


    protected AbstractMultiDataSourceInterceptor(MultipleDataSource multipleDataSource, ConcurrentHashMap<String, ConnectionSignature> connectionSignatureMap) {
        this.multipleDataSource = multipleDataSource;
        this.connectionSignatureMap = connectionSignatureMap;
    }

    /**
     * 获取Mapper Class
     *
     * @param invocation
     * @return
     * @throws ClassNotFoundException
     */
    protected Class<?> getMapperClass(Invocation invocation) {
        Object target = invocation.getTarget();
        if (target instanceof RoutingStatementHandler) {
            RoutingStatementHandler handler = (RoutingStatementHandler) invocation.getTarget();
            StatementHandler delegate = (StatementHandler) ObjectUtils.getFieldValue(handler, "delegate");
            MappedStatement mappedStatement = (MappedStatement) ObjectUtils.getFieldValue(delegate, "mappedStatement");
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

    /**
     * 设置连接
     *
     * @param invocation
     * @param key
     * @throws SQLException
     */
    protected void setConnection(Invocation invocation, String key) throws SQLException {
        Object[] args = invocation.getArgs();
        Connection originConnection = (Connection) args[0];
        boolean isSwitch = false;
        // 判断是否切换
        if (multipleDataSource.hasDataSourceKey(key)) {
            if (!key.equals(multipleDataSource.getDataSourceKey())) {
                // key不同直接切换
                isSwitch = true;
            } else {
                // Key相同，尝试判断连接签名
                ConnectionSignature connectionSignature = connectionSignatureMap.get(key);
                ConnectionSignature originConnectionSignature = new ConnectionSignature(originConnection);
                if (!Objects.isNull(connectionSignature)
                        && !connectionSignature.equals(originConnectionSignature)) {
                    // 连接签名不同，标记切换
                    isSwitch = true;
                } else if (Objects.isNull(connectionSignature)) {
                    // 连接签名为空，尝试构建连接签名
                    Connection newConnection = multipleDataSource.getDataSouce(key).getConnection();
                    ConnectionSignature newConnectionSignature = new ConnectionSignature(newConnection);
                    connectionSignatureMap.put(key, connectionSignature);
                    if (newConnectionSignature.equals(originConnectionSignature)) {
                        isSwitch = true;
                    }
                }
            }
        }
        // 切换数据源
        if (isSwitch) {
            // 多数据源持有且Key不同时候切换
            multipleDataSource.setDataSourceKey(key);
            Connection newConnection = multipleDataSource.getConnection();
            args[0] = newConnection;
            ObjectUtils.setFieldValue(invocation, "args", args);
            if (log.isDebugEnabled()) {
                log.debug("op<intercept> change to datasource key: " + key);
            }
        }
    }
}
