package per.nonlone.framework.mybatis.multisource;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.jdbc.datasource.DataSourceUtils;
import per.nonlone.framework.datasource.MultipleDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;


/**
 * Mybatis 多数据源抽象拦截器
 */
@Slf4j
public abstract class BaseMyBatisDataSourceSelector implements MyBatisDataSourceSelector {


    @Getter
    protected MultipleDataSource multipleDataSource;

    protected ThreadLocal<Connection> connectionThreadLocal = new ThreadLocal<>();

    protected ThreadLocal<DataSource> dataSourceThreadLocal = new ThreadLocal<>();

    protected ThreadLocal<Boolean> firstTransactionThreadLocal = new ThreadLocal<>();

    protected ThreadLocal<Boolean> dirtyTransactionThreadLocal = new ThreadLocal<>();


    protected BaseMyBatisDataSourceSelector(MultipleDataSource multipleDataSource) {
        this.multipleDataSource = multipleDataSource;
    }

    /**
     * 获取当前连接
     *
     * @return
     */
    @Override
    public Connection getCurrentConnection() {
        return connectionThreadLocal.get();
    }

    /**
     * 获取当前数据池
     *
     * @return
     */
    @Override
    public DataSource getCurrentDataSource() {
        return dataSourceThreadLocal.get();
    }

    /**
     * 判断事务是否存在未提交数据
     *
     * @return
     */
    @Override
    public boolean isTransactionDirty() {
        if (Objects.nonNull(dirtyTransactionThreadLocal.get())) {
            return dirtyTransactionThreadLocal.get();
        }
        return false;
    }

    @Override
    public boolean markTransactionDirty() {
        if (!isTransactionDirty()) {
            dirtyTransactionThreadLocal.set(true);
            return true;
        }
        return false;
    }

    /**
     * 获取当前是否首次事务状态
     *
     * @return
     */
    @Override
    public synchronized boolean isFirstTranscation() {
        if (Objects.nonNull(firstTransactionThreadLocal.get())) {
            return firstTransactionThreadLocal.get();
        }
        return true;
    }

    /**
     * 标记首次事务开始
     *
     * @return
     */
    @Override
    public synchronized boolean markFirstTranscationBegin() {
        if (isFirstTranscation()) {
            firstTransactionThreadLocal.set(false);
            return true;
        }
        return false;
    }

    /**
     * 标记首次事务结束
     *
     * @return
     */
    @Override
    public synchronized boolean markFirstTranscationFinish() {
        if (!isFirstTranscation()) {
            firstTransactionThreadLocal.set(true);
            return true;
        }
        return false;
    }


    @Override
    public void setThreadLocalConnection(Connection connection) {
        connectionThreadLocal.set(connection);
    }

    @Override
    public void setThreadLocalDataSource(DataSource dataSource) {
        dataSourceThreadLocal.set(dataSource);
    }

    @Override
    public void clearThreadLocal() {
        dataSourceThreadLocal.remove();
        connectionThreadLocal.remove();
        firstTransactionThreadLocal.remove();
        dirtyTransactionThreadLocal.remove();
    }

    @Override
    public Connection getConnection(Class<?> mapperClass) {
        DataSource dataSource = getDataSource(mapperClass);
        if (Objects.nonNull(dataSource)) {
            try {
                Connection newConnection = dataSource.getConnection();
                return newConnection;
            } catch (SQLException sqle) {
                throw new RuntimeException(sqle);
            }
        }
        return null;
    }

    /**
     * 关闭连接
     */
    @Override
    public void close() {
        // 判断是否有事务标记
        DataSource dataSource = dataSourceThreadLocal.get();
        Connection connection = connectionThreadLocal.get();
        // 关闭数据库连接
        if (ObjectUtils.allNotNull(dataSource, connection)) {
            if (log.isDebugEnabled()) {
                log.debug(String.format(" DataSourceUtils.releaseConnection connection<%s>", connection));
            }
            DataSourceUtils.releaseConnection(connection, dataSource);
            connectionThreadLocal.remove();
        } else if (Objects.nonNull(connection)) {
            try {
                connection.close();
                connectionThreadLocal.remove();
            } catch (SQLException sqle) {
                throw new RuntimeException(sqle);
            }
            if (log.isDebugEnabled()) {
                log.debug(String.format("close connection without dataSource"));
            }
        } else {
            if (log.isDebugEnabled()) {
                log.debug(String.format("connection is null , can not close"));
            }
        }
    }
}
