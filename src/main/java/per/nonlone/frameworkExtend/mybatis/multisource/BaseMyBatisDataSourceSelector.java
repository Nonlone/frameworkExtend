package per.nonlone.frameworkExtend.mybatis.multisource;

import per.nonlone.frameworkExtend.mybatis.MultipleDataSource;
import per.nonlone.frameworkExtend.mybatis.interceptor.ConnectionSignature;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.jdbc.datasource.DataSourceUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Mybatis 多数据源抽象拦截器
 */
@Slf4j
public abstract class BaseMyBatisDataSourceSelector implements MyBatisDataSourceSelector {


    @Getter
    protected MultipleDataSource multipleDataSource;

    protected ConcurrentHashMap<String, ConnectionSignature> connectionSignatureMap;

    protected ThreadLocal<Connection> connectionThreadLocal = new ThreadLocal<>();

    protected ThreadLocal<DataSource> dataSourceThreadLocal = new ThreadLocal<>();


    protected BaseMyBatisDataSourceSelector(MultipleDataSource multipleDataSource, ConcurrentHashMap<String, ConnectionSignature> connectionSignatureMap) {
        this.multipleDataSource = multipleDataSource;
        this.connectionSignatureMap = connectionSignatureMap;
    }


    @Override
    public Connection getConnection(Class<?> mapperClass, Connection connection) {
        javax.sql.DataSource dataSource = getDataSource(mapperClass,connection);
        if(Objects.nonNull(dataSource)){
            try {
                Connection newConnection = dataSource.getConnection();
                // 放入TheadLocal，供关闭时候使用
                dataSourceThreadLocal.set(dataSource);
                connectionThreadLocal.set(newConnection);
                return newConnection;
            } catch (SQLException sqle) {
                throw new RuntimeException(sqle);
            }
        }
        return connection;
    }

    /**
     * 关闭连接
     */
    @Override
    public void close() {
        // 判断是否有事务标记
        if (Objects.nonNull(connectionThreadLocal.get())) {
            // 提交事务
            try {
                connectionThreadLocal.get().commit();
            } catch (SQLException sqle) {
                throw new RuntimeException(sqle);
            }
        }
        // 关闭数据库连接
        if (ObjectUtils.allNotNull(dataSourceThreadLocal.get(), connectionThreadLocal.get())) {
            DataSourceUtils.releaseConnection(connectionThreadLocal.get(), dataSourceThreadLocal.get());
            dataSourceThreadLocal.remove();
            connectionThreadLocal.remove();
        } else if (ObjectUtils.allNotNull(connectionThreadLocal.get())) {
            try {
                Connection connection = connectionThreadLocal.get();
                connection.close();
            } catch (SQLException sqle) {
                throw new RuntimeException(sqle);
            } finally {
                connectionThreadLocal.remove();
            }
        }
    }
}
