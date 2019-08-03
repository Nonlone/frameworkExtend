package per.nonlone.frameworkExtend.mybatis.multisource;

import lombok.Data;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import per.nonlone.frameworkExtend.mybatis.interceptor.BaseInterceptor;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;

/**
 * MyBatis 自动分配数据库连接插件，需要注入多数据池数据源选择器，在 Executor commit,rollback 方法l拦截，用于标记事务结束
 */

@Slf4j
@Data
@Intercepts({
        @Signature(type = Executor.class, method = MultiDataSourceExecutorFinishInteceptor.COMMIT_METHOD, args = {boolean.class}),
        @Signature(type = Executor.class, method = MultiDataSourceExecutorFinishInteceptor.ROLLBACK_METHOD, args = {boolean.class})
})
public class MultiDataSourceExecutorFinishInteceptor extends BaseInterceptor {

    static final String COMMIT_METHOD = "commit";

    static final String ROLLBACK_METHOD = "rollback";

    private MyBatisDataSourceSelector myBatisDataSourceSelector;

    public MultiDataSourceExecutorFinishInteceptor(@NonNull MyBatisDataSourceSelector myBatisDataSourceSelector) {
        this.myBatisDataSourceSelector = myBatisDataSourceSelector;
    }

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        Object result = invocation.proceed();
        if (log.isDebugEnabled()) {
            log.debug(String.format("intercept finish Invocation.proceed() costTime<%sms>", stopWatch.getTime()));
        }
        // 标记提交
        stopWatch.reset();
        stopWatch.start();

        Connection connection = myBatisDataSourceSelector.getCurrentConnection();
        if (Objects.nonNull(connection) && !connection.isClosed() && !connection.getAutoCommit()) {
            if (TransactionSynchronizationManager.isActualTransactionActive()
                    || !connection.getAutoCommit()) {
                myBatisDataSourceSelector.markFirstTranscationFinish();
            }
            Method method = invocation.getMethod();
            boolean isCommit = true;
            if (!COMMIT_METHOD.equals(method.getName())) {
                isCommit = false;
            }
            try {
                if (isCommit) {
                    connection.commit();
                } else {
                    connection.rollback();
                }
            } catch (SQLException e) {
                log.error(String.format("intercept finish commit or rollback error isCommit<%s>", isCommit), e);
            } finally {
                stopWatch.stop();
                if (log.isDebugEnabled()) {
                    log.debug(String.format("process connection commit or rollback isCommit<%s> costTime<%sms>", isCommit, stopWatch.getTime()));
                }
            }
        }else{
            if(log.isDebugEnabled()){
                log.debug(String.format("connection<%s> is null or is close or is not AutoCommit",connection,
                        Objects.nonNull(connection)?connection.isClosed():"false",
                        Objects.nonNull(connection)?connection.getAutoCommit():"false"));
            }
        }
        return result;
    }


}
