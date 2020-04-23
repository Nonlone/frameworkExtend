package per.nonlone.framework.mybatis.multisource;

import lombok.Data;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.executor.BaseExecutor;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.transaction.Transaction;
import org.apache.ibatis.transaction.jdbc.JdbcTransaction;
import org.apache.ibatis.transaction.managed.ManagedTransaction;
import org.mybatis.spring.transaction.SpringManagedTransaction;
import org.springframework.jdbc.datasource.ConnectionHolder;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.datasource.SimpleConnectionHandle;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import per.nonlone.framework.datasource.MultipleDataSource;
import per.nonlone.framework.mybatis.exception.CrossConnectionTransactionException;
import per.nonlone.utils.ObjectUtils;
import per.nonlone.utils.StringUtils;

import javax.sql.DataSource;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MyBatis 自动分配数据库连接插件，需要注入 Transaction 成员变量，在 Executor close 方法l拦截，用于关闭自定义数据源
 */

@Slf4j
@Data
@Intercepts({@Signature(type = Executor.class, method = MultiDataSourceExecutorInteceptor.UPDATE_METHOD, args = {MappedStatement.class, Object.class}),
        @Signature(type = Executor.class, method = MultiDataSourceExecutorInteceptor.QUERY_METHOD, args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),
        @Signature(type = Executor.class, method = MultiDataSourceExecutorInteceptor.QUERY_METHOD, args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class, CacheKey.class, BoundSql.class})
})
public class MultiDataSourceExecutorInteceptor extends BaseExecutorInterceptor {

    static final String QUERY_METHOD = "query";

    static final String UPDATE_METHOD = "update";


    private static final String TRANSACTION = "transaction";

    /**
     * 成员变量常量，事务
     */
//    private static final Field FIELD_TRANSACTION = ObjectUtils.getAccessibleField(BaseExecutor.class, TRANSACTION);

    /**
     * 成员变量常量，数据源
     */
    private static final String DATASOURCE = "dataSource";

    /**
     * 成员变量常量，数据库连接
     */
    private static final String CONNECTION = "connection";

    /**
     * MapperClass 映射连接签名
     */
    private static final Map<Class<?>, MapperClassRepository> mapperClassCache = new ConcurrentHashMap<>();


    private static final Field FIELD_DATASOURCE_MANAGEDTRANSACTION = ObjectUtils.getAccessibleField(ManagedTransaction.class, DATASOURCE);


    private static final Field FIELD_DATASOURCE_JDBCTRANSACTION = ObjectUtils.getAccessibleField(JdbcTransaction.class, DATASOURCE);


    private static final Field FIELD_DATASOURCE_SPRINGMANAGEDTRANSACTION = ObjectUtils.getAccessibleField(SpringManagedTransaction.class, DATASOURCE);


    private static final Field FIELD_CONNECTION_MANAGEDTRANSACTION = ObjectUtils.getAccessibleField(ManagedTransaction.class, CONNECTION);


    private static final Field FIELD_CONNECTION_JDBCTRANSACTION = ObjectUtils.getAccessibleField(JdbcTransaction.class, CONNECTION);


    private static final Field FIELD_CONNECTION_SPRINGMANAGEDTRANSACTION = ObjectUtils.getAccessibleField(SpringManagedTransaction.class, CONNECTION);


    private static final Field FIELD_CONNETION_HANDLE_CONNECTION = ObjectUtils.getAccessibleField(SimpleConnectionHandle.class, CONNECTION);

    private static final Field FIELD_CURRENT_CONNECTION = ObjectUtils.getAccessibleField(ConnectionHolder.class, "currentConnection");

    private static final Field FIELD_CONNECTION_HANDLE = ObjectUtils.getAccessibleField(ConnectionHolder.class, "connectionHandle");

    private MyBatisDataSourceSelector myBatisDataSourceSelector;


    public MultiDataSourceExecutorInteceptor(@NonNull MyBatisDataSourceSelector myBatisDataSourceSelector) {
        this.myBatisDataSourceSelector = myBatisDataSourceSelector;
    }

    /**
     * 获取 DataSource
     *
     * @param transaction
     * @return
     * @throws IllegalAccessException
     */
    private DataSource getDataSourceFromTransaction(Transaction transaction) throws IllegalAccessException {
        if (transaction instanceof ManagedTransaction) {
            return (DataSource) FIELD_DATASOURCE_MANAGEDTRANSACTION.get(transaction);
        } else if (transaction instanceof JdbcTransaction) {
            return (DataSource) FIELD_DATASOURCE_JDBCTRANSACTION.get(transaction);
        } else if (transaction instanceof SpringManagedTransaction) {
            return (DataSource) FIELD_DATASOURCE_SPRINGMANAGEDTRANSACTION.get(transaction);
        } else {
            return (DataSource) ObjectUtils.getFieldValue(transaction, DATASOURCE);
        }
    }

    /**
     * 设置DataSource
     *
     * @param transaction
     * @param dataSource
     * @throws IllegalAccessException
     */
    private void setDataSourceToTransaction(Transaction transaction, DataSource dataSource) throws IllegalAccessException {
        if (transaction instanceof ManagedTransaction) {
            FIELD_DATASOURCE_MANAGEDTRANSACTION.set(transaction, dataSource);
        } else if (transaction instanceof JdbcTransaction) {
            FIELD_DATASOURCE_JDBCTRANSACTION.set(transaction, dataSource);
        } else if (transaction instanceof SpringManagedTransaction) {
            FIELD_DATASOURCE_SPRINGMANAGEDTRANSACTION.set(transaction, dataSource);
        } else {
            ObjectUtils.setFieldValue(transaction, DATASOURCE, dataSource);
        }
    }

    /**
     * 获取Connection， 注意不要使用 getter，会触发创建connection
     *
     * @param transaction
     * @return
     */
    private Connection getConnectionFromTransaction(Transaction transaction) throws IllegalAccessException {
        if (transaction instanceof ManagedTransaction) {
            return (Connection) FIELD_CONNECTION_MANAGEDTRANSACTION.get(transaction);
        } else if (transaction instanceof JdbcTransaction) {
            return (Connection) FIELD_CONNECTION_JDBCTRANSACTION.get(transaction);
        } else if (transaction instanceof SpringManagedTransaction) {
            return (Connection) FIELD_CONNECTION_SPRINGMANAGEDTRANSACTION.get(transaction);
        } else {
            return (Connection) ObjectUtils.getField(transaction, CONNECTION).get(CONNECTION);
        }
    }

    /**
     * 设置Connection
     *
     * @param transaction
     * @param connection
     * @throws IllegalAccessException
     */
    private void setConnectionToTransaction(Transaction transaction, Connection connection) throws IllegalAccessException {
        if (transaction instanceof ManagedTransaction) {
            FIELD_CONNECTION_MANAGEDTRANSACTION.set(transaction, connection);
        } else if (transaction instanceof JdbcTransaction) {
            FIELD_CONNECTION_JDBCTRANSACTION.set(transaction, connection);
        } else if (transaction instanceof SpringManagedTransaction) {
            FIELD_CONNECTION_SPRINGMANAGEDTRANSACTION.set(transaction, connection);
        } else {
            ObjectUtils.setFieldValue(transaction, CONNECTION, connection);
        }
    }

    /**
     * 切换Connection
     *
     * @param transaction
     * @param dataSource
     * @param connection
     * @param mapperClass
     * @return
     * @throws Throwable
     */
    private boolean changeConnection(@NonNull Transaction transaction, DataSource dataSource, Connection connection, Class<?> mapperClass) throws Throwable {
        if (Objects.isNull(connection) && Objects.nonNull(dataSource)) {
            if (log.isDebugEnabled()) {
                log.debug(String.format("connection is null and dataSource is not null"));
            }
            //connection 为空 ，直接切换 dataSource
            if (dataSource instanceof MultipleDataSource) {
                MultipleDataSource multipleDataSource = (MultipleDataSource) dataSource;
                String currentKey = multipleDataSource.getCurrentLookupKey();
                String key = myBatisDataSourceSelector.getKey(mapperClass);
                if (StringUtils.isNotBlank(currentKey) && currentKey.equals(key)) {
                    // Key 相同，不需要切换
                    if (log.isDebugEnabled()) {
                        log.debug(String.format("key is equal not change MultipleDataSource key<%s> mapperClass<%s>", key, mapperClass.getName()));
                    }
                    return false;
                }
                if (transaction instanceof SpringManagedTransaction) {
                    if (log.isDebugEnabled()) {
                        log.debug(String.format("transaction is a instanceOf SpringManagedTransaction try fix connectionHolder"));
                    }
                    // 修改 TransactionSynchronizationManager 的缓存
                    ConnectionHolder connectionHolder = (ConnectionHolder) TransactionSynchronizationManager.getResource(dataSource);
                    if (connectionHolder != null &&
                            (Objects.nonNull(connectionHolder.getConnectionHandle()) || Objects.nonNull(connectionHolder.getConnection()))) {
                        // 重新封入connectionHolder
                        Connection storeConnection = connectionHolder.getConnection();
                        if (Objects.isNull(storeConnection)) {
                            storeConnection = connectionHolder.getConnectionHandle().getConnection();
                        }

                        ConnectionSignature storeConnectionSignature = new ConnectionSignature(storeConnection);
                        MapperClassRepository mapperClassRepository = getMapperClassRepository(mapperClass, storeConnectionSignature);
                        if (mapperClassRepository.isChanged()) {
                            // 确定更换连接
                            Connection newConnection = mapperClassRepository.getConnection();
                            myBatisDataSourceSelector.setThreadLocalConnection(newConnection);
                            myBatisDataSourceSelector.setThreadLocalDataSource(dataSource);
                            // 更新Connecton数据
                            newConnection.setAutoCommit(storeConnection.getAutoCommit());
                            newConnection.setTransactionIsolation(storeConnection.getTransactionIsolation());

                            //设置新值
                            if ( Objects.nonNull(connectionHolder.getConnection())) {
                                FIELD_CURRENT_CONNECTION.set(connectionHolder, newConnection);
                            }
                            if ( (connectionHolder.getConnectionHandle() instanceof SimpleConnectionHandle)
                                    && Objects.nonNull(connectionHolder.getConnectionHandle().getConnection())) {
                                FIELD_CONNETION_HANDLE_CONNECTION.set(connectionHolder.getConnectionHandle(), newConnection);
                            } else {
                                FIELD_CONNECTION_HANDLE.set(connectionHolder, new SimpleConnectionHandle(newConnection));
                            }

                            // 关闭原来连接
                            if (!storeConnection.isClosed()) {
                                if (log.isDebugEnabled()) {
                                    log.debug(String.format("DataSourceUtils.releaseConnection connection<%s> ConnectionSignature<%s>", storeConnection, storeConnectionSignature));
                                }
                                DataSourceUtils.releaseConnection(storeConnection, dataSource);
//                                storeConnection.close();
                            }


                            // 多数据源，直接标记新Key
                            multipleDataSource.setDataSourceKey(key);
                            if (log.isDebugEnabled()) {
                                log.debug(String.format("with connection in ConnectionHolder change MultipleDataSource key from<%s> to<%s> mapperClass<%s>", currentKey, key, mapperClass.getName()));
                            }
                        } else {
                            if (log.isDebugEnabled()) {
                                log.debug(String.format("mapperClassRepository is not changed,it is not need to fix "));
                            }
                        }

                    } else {
                        // 多数据源，直接标记新Key
                        multipleDataSource.setDataSourceKey(key);
                        if (log.isDebugEnabled()) {
                            log.debug(String.format("not connction in ConnectionHolder change MultipleDataSource key from<%s> to<%s> mapperClass<%s>", currentKey, key, mapperClass.getName()));
                        }
                    }
                } else {
                    // 多数据源，直接标记新Key
                    multipleDataSource.setDataSourceKey(key);
                    if (log.isDebugEnabled()) {
                        log.debug(String.format("not SpringManagedTransaction change MultipleDataSource key from<%s> to<%s> mapperClass<%s>", currentKey, key, mapperClass.getName()));
                    }
                }
                return true;
            } else {
                // 直接为数据源本身
                Connection oldConenction = dataSource.getConnection();
                ConnectionSignature oldConnectionSignature = new ConnectionSignature(oldConenction);
                MapperClassRepository mapperClassRepository = getMapperClassRepository(mapperClass, oldConnectionSignature);
                if (mapperClassRepository.isChanged()) {
                    DataSource newDataSource = mapperClassRepository.getDataSource();
                    // 连接签名不相同，设置数据源
                    setDataSourceToTransaction(transaction, newDataSource);
                    myBatisDataSourceSelector.setThreadLocalDataSource(newDataSource);
                    // 释放原来连接
                    if (log.isDebugEnabled()) {
                        log.debug(String.format(" DataSourceUtils.releaseConnection oldConnection<%s> connectionSignature<%s>", oldConenction, oldConnectionSignature));
                    }
                    DataSourceUtils.releaseConnection(oldConenction, dataSource);
                    // 释放新建连接
                    if (log.isDebugEnabled()) {
                        log.debug(String.format(" DataSourceUtils.releaseConnection newConnection<%s> connectionSignature<%s>", mapperClassRepository.getConnection(), mapperClassRepository.getConnectionSignature()));
                    }
                    DataSourceUtils.releaseConnection(mapperClassRepository.getConnection(), newDataSource);

                    if (log.isDebugEnabled()) {
                        log.debug(String.format("processInvocation change dataSource to from<%s> to<%s>", oldConnectionSignature.toString(), mapperClassRepository.toString()));
                    }
                    return true;
                } else {
                    if (log.isDebugEnabled()) {
                        log.debug(String.format("processInvocation not change dataSource to old<%s> new<%s>", oldConnectionSignature.toString(), mapperClassRepository.toString()));
                    }
                }
                return false;
            }
        } else {
            if (log.isDebugEnabled()) {
                log.debug(String.format("connection is not null and dataSource is not null"));
            }
            // connection 不为空
            ConnectionSignature oldConnectionSignature = new ConnectionSignature(connection);
            MapperClassRepository mapperClassRepository = getMapperClassRepository(mapperClass, oldConnectionSignature);
            if (mapperClassRepository.isChanged()) {
                Connection newConnection = mapperClassRepository.getConnection();
                // 更新Connecton数据
                newConnection.setAutoCommit(connection.getAutoCommit());
                newConnection.setTransactionIsolation(connection.getTransactionIsolation());
                // 尝试提交事务，然后关闭数据源
                if (!connection.isClosed()) {
                    if (log.isDebugEnabled()) {
                        log.debug(String.format(" DataSourceUtils.releaseConnection oldConnection<%s>  ConnectionSignature<%s>", connection, oldConnectionSignature));
                    }
                    DataSourceUtils.releaseConnection(connection, dataSource);
                }
                // transaction 类赋值 selector threadLocal赋值
                setConnectionToTransaction(transaction, newConnection);
                myBatisDataSourceSelector.setThreadLocalConnection(newConnection);

                // 设置数据源
                if (dataSource instanceof MultipleDataSource) {
                    ((MultipleDataSource) dataSource).setDataSourceKey(myBatisDataSourceSelector.getKey(mapperClass));
                    myBatisDataSourceSelector.setThreadLocalDataSource(dataSource);
                } else {
                    DataSource newDataSource = mapperClassRepository.getDataSource();
                    setDataSourceToTransaction(transaction, newDataSource);
                    myBatisDataSourceSelector.setThreadLocalDataSource(newDataSource);
                }
                if (log.isDebugEnabled()) {
                    log.debug(String.format("processInvocation change dataSource to from<%s> to<%s>", oldConnectionSignature.toString(), mapperClassRepository.toString()));
                }
                return true;
            } else {
                if (log.isDebugEnabled()) {
                    log.debug(String.format("processInvocation not change dataSource to old<%s> new<%s>", oldConnectionSignature.toString(), mapperClassRepository.toString()));
                }
            }
            return false;
        }
    }


    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        StopWatch stopWatch = new StopWatch();

        // 获取MyBatis 基础执行器
        BaseExecutor baseExecutor = getBaseExecutor(invocation);
        // 映射MappedStatement
        MappedStatement mappedStatement = getMappedStatement(invocation);

        if (Objects.nonNull(baseExecutor)
                && Objects.nonNull(mappedStatement)) {
            // 或者映射 MapperClass
            Class<?> mapperClass = getMapperClassFromMapperStatement(mappedStatement);
            // 获取事务包裹类
            Transaction transaction = baseExecutor.getTransaction();
            if (Objects.nonNull(transaction)) {

                // 修改 Transaction
                DataSource dataSource = getDataSourceFromTransaction(transaction);
                Connection connection = getConnectionFromTransaction(transaction);

                boolean isTransaction = TransactionSynchronizationManager.isActualTransactionActive();
                boolean isFirstTransaction = myBatisDataSourceSelector.isFirstTranscation();
                if (Objects.nonNull(connection)) {
                    isTransaction &= !connection.getAutoCommit();
                }

                if (!isTransaction) {
                    //非事务状态下切换
                    stopWatch.reset();
                    stopWatch.start();
                    boolean changeResult = changeConnection(transaction, dataSource, connection, mapperClass);
                    stopWatch.stop();
                    if (log.isDebugEnabled()) {
                        log.debug(String.format("noTransaction changeConnection result<%s> costTime<%sms>", changeResult, stopWatch.getTime()));
                    }
                } else {
                    if (isFirstTransaction) {
                        // 事务状态下首次切换
                        myBatisDataSourceSelector.markFirstTranscationBegin();
                        stopWatch.reset();
                        stopWatch.start();
                        boolean changeResult = changeConnection(transaction, dataSource, connection, mapperClass);
                        stopWatch.stop();
                        if (log.isDebugEnabled()) {
                            log.debug(String.format("first Transaction changeConnection result<%s> costTime<%sms>", changeResult, stopWatch.getTime()));
                        }
                    } else {
                        // 事务状态非首次切换，尝试切换，发现真实切换，抛出跨库事务异常
                        stopWatch.reset();
                        stopWatch.start();
                        if (changeConnection(transaction, dataSource, connection, mapperClass)) {
                            stopWatch.stop();
                            if (log.isDebugEnabled()) {
                                log.debug(String.format("check changeConnection  is true costTime<%sms>", stopWatch.getTime()));
                            }
                            String message = String.format("intercept mapperClass<%s> error", mapperClass.getName());
                            // 不存在未提交数据时候，不会触发commit或者rollback，尝试清除事务标记
                            if (!myBatisDataSourceSelector.isTransactionDirty()
                                    && myBatisDataSourceSelector.markFirstTranscationFinish()) {
                                myBatisDataSourceSelector.close();
                                myBatisDataSourceSelector.clearThreadLocal();
                            }
                            throw new CrossConnectionTransactionException(message);
                        } else {
                            stopWatch.stop();
                            if (log.isDebugEnabled()) {
                                log.debug(String.format("check processInvocation  is false costTime<%sms>", stopWatch.getTime()));
                            }
                        }
                    }

                    // 事务状态下update，标记存在脏数据
                    String methodName = invocation.getMethod().getName();
                    if (UPDATE_METHOD.equals(methodName)) {
                        myBatisDataSourceSelector.markTransactionDirty();
                    }
                }
            }
        }

        stopWatch.reset();
        stopWatch.start();
        try {
            Object result = invocation.proceed();
            stopWatch.stop();
            if (log.isDebugEnabled()) {
                log.debug(String.format("intercept  Invocation.proceed() costTime<%sms>", stopWatch.getTime()));
            }
            return result;
        } catch (Throwable throwable) {
            if (stopWatch.isStarted()) {
                stopWatch.stop();
            }
            if (log.isDebugEnabled()) {
                log.debug(String.format("intercept  Invocation.proceed() throw throwable costTime<%sms>", stopWatch.getTime()));
            }
            //关闭连接
            myBatisDataSourceSelector.close();
            //异常触发事务回滚，清理连接标记
            myBatisDataSourceSelector.clearThreadLocal();
            throw throwable;
        }
    }

    /**
     * 获取并缓存连接
     *
     * @param mapperClass
     * @return
     * @throws SQLException
     */
    private MapperClassRepository getMapperClassRepository(Class<?> mapperClass, ConnectionSignature oldConnectionSignature) throws SQLException {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        if (mapperClassCache.containsKey(mapperClass)) {
            stopWatch.stop();
            MapperClassRepository mapperClassRepository = mapperClassCache.get(mapperClass);
            if (log.isDebugEnabled()) {
                log.debug(String.format("getMapperClassRepository in cache costTime<%sms>", stopWatch.getTime()));
            }
            mapperClassRepository.setChanged(!oldConnectionSignature.checkEquals(mapperClassRepository.getConnectionSignature()));
            if (mapperClassRepository.isChanged()) {
                stopWatch.reset();
                stopWatch.start();
                mapperClassRepository.setConnection(mapperClassRepository.getDataSource().getConnection());
                if (log.isDebugEnabled()) {
                    log.debug(String.format("getConnection<%s> with isChanged is true in hit mapperClassCache", mapperClassRepository.getConnection()));
                }
                stopWatch.stop();
                if (log.isDebugEnabled()) {
                    log.debug(String.format("isChanged is true getConnection costTime<%sms>", stopWatch.getTime()));
                }
            }
            return mapperClassRepository;
        }

        stopWatch.reset();
        stopWatch.start();
        Connection newConnection = myBatisDataSourceSelector.getConnection(mapperClass);
        if (log.isDebugEnabled()) {
            log.debug(String.format("miss hit mapperClassCache generate ConnectionSignature with getConnection<%s>", newConnection));
        }
        stopWatch.stop();
        if (log.isDebugEnabled()) {
            log.debug(String.format("getConnection newConnection  costTime<%sms>", stopWatch.getTime()));
        }
        ConnectionSignature connectionSignature = new ConnectionSignature(newConnection);
        DataSource dataSource = myBatisDataSourceSelector.getDataSource(mapperClass);
        if (dataSource instanceof MultipleDataSource) {
            // 切换到正确的dataSource
            MultipleDataSource multipleDataSource = (MultipleDataSource) dataSource;
            dataSource = multipleDataSource.getDataSouce(myBatisDataSourceSelector.getKey(mapperClass));
        }
        MapperClassRepository mapperClassRepository = new MapperClassRepository(connectionSignature, dataSource);
        mapperClassRepository.setChanged(!oldConnectionSignature.checkEquals(connectionSignature));
        if (mapperClassRepository.isChanged()) {
            mapperClassRepository.setConnection(newConnection);
        } else {
            stopWatch.reset();
            stopWatch.start();
            //释放创建新连接
            if (log.isDebugEnabled()) {
                log.debug(String.format("isChanged is false, DataSourceUtils.releaseConnection connection<%s>", newConnection));
            }
            DataSourceUtils.releaseConnection(newConnection, dataSource);
            stopWatch.stop();
            if (log.isDebugEnabled()) {
                log.debug(String.format("releaseConnection newConnection  costTime<%sms>", stopWatch.getTime()));
            }
        }
        mapperClassCache.put(mapperClass, mapperClassRepository);
        return mapperClassRepository;
    }

    /**
     * MapperClass 缓存
     */
    private class MapperClassRepository {

        @Setter
        @Getter
        private boolean changed;

        @Getter
        @Setter
        private Connection connection;

        @Getter
        private ConnectionSignature connectionSignature;

        @Getter
        private DataSource dataSource;

        private MapperClassRepository(ConnectionSignature connectionSignature, DataSource dataSource) {
            this.connectionSignature = connectionSignature;
            this.dataSource = dataSource;
        }
    }
}
