package per.nonlone.frameworkExtend.mybatis.multisource.interceptor;

import per.nonlone.frameworkExtend.mybatis.interceptor.BaseExecutorInterceptor;
import per.nonlone.frameworkExtend.mybatis.interceptor.ConnectionSignature;
import per.nonlone.frameworkExtend.mybatis.multisource.MyBatisDataSourceSelector;
import com.feitai.utils.ObjectUtils;
import lombok.Data;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
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
import org.springframework.jdbc.datasource.DataSourceUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.Objects;

/**
 * MyBatis 自动分配数据库连接插件，需要注入 Transaction 成员变量，在 Executor close 方法l拦截，用于关闭自定义数据源
 */

@Slf4j
@Data
@Intercepts({@Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class}),
        @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),
        @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class, CacheKey.class, BoundSql.class})
})
class MultiDataSourceExecutorInteceptor extends BaseExecutorInterceptor {

    private MyBatisDataSourceSelector myBatisDataSourceSelector;

    public MultiDataSourceExecutorInteceptor(@NonNull MyBatisDataSourceSelector myBatisDataSourceSelector) {
        this.myBatisDataSourceSelector = myBatisDataSourceSelector;
    }

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        BaseExecutor baseExecutor = getBaseExecutor(invocation);
        MappedStatement mappedStatement = getMappedStatement(invocation);
        if (Objects.nonNull(baseExecutor)
                && Objects.nonNull(mappedStatement)) {
            Class<?> mapperClass = getMapperClassFromMapperStatement(mappedStatement);
            Transaction transaction = (Transaction) ObjectUtils.getFieldValue(baseExecutor, "transaction");
            if (Objects.nonNull(transaction)) {
                // 修改 Transaction
                DataSource dataSource = (DataSource) ObjectUtils.getFieldValue(transaction, "dataSource");
                Connection connection = (Connection) ObjectUtils.getFieldValue(transaction, "connection");
                if (Objects.isNull(connection)) {
                    //connection 为空 ，直接切换 dataSource
                    Connection oldConenction = dataSource.getConnection();
                    ConnectionSignature oldConnectionSignature = new ConnectionSignature(oldConenction);
                    DataSource newDataSource = myBatisDataSourceSelector.getDataSource(mapperClass);
                    Connection newConenction = newDataSource.getConnection();
                    ConnectionSignature newConnectionSignature = new ConnectionSignature(newConenction);
                    if(!newConnectionSignature.checkEquals(oldConnectionSignature)){
                        // 连接签名不相同，设置数据源
                        ObjectUtils.setFieldValue(transaction,"dataSource",newDataSource);
                        // 释放连接
                        DataSourceUtils.releaseConnection(oldConenction,dataSource);
                        DataSourceUtils.releaseConnection(newConenction,newDataSource);
                    }
                }else{
                    // connection 不为空
                    ConnectionSignature oldConnectionSignature = new ConnectionSignature(connection);
                    Connection newConnection = myBatisDataSourceSelector.getConnection(mapperClass,connection);
                    ConnectionSignature newConnectionSignature = new ConnectionSignature(newConnection);
                    if(!newConnectionSignature.checkEquals(oldConnectionSignature)){
                        // 连接签名不相同，设置数据源

                        // 尝试提交事务，然后关闭数据源
                        transaction.commit();
                        transaction.close();

                        // 连接签名不相同，设置数据源
                        DataSource newDataSource = myBatisDataSourceSelector.getDataSource(mapperClass,connection);
                        ObjectUtils.setFieldValue(transaction,"dataSource",newDataSource);
                        ObjectUtils.setFieldValue(transaction,"connection",null);
                        // 释放连接
                        DataSourceUtils.releaseConnection(connection,dataSource);
                        DataSourceUtils.releaseConnection(newConnection,newDataSource);
                    }
                }
            }

        }
        Object result = invocation.proceed();
        if (log.isDebugEnabled()) {
            log.debug("op<intercept> after Invocation.proceed()");
        }
        return result;
    }




}
