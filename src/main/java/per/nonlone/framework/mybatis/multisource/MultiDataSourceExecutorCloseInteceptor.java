package per.nonlone.framework.mybatis.multisource;

import lombok.Data;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import per.nonlone.framework.mybatis.interceptor.BaseInterceptor;

/**
 * MyBatis 自动分配数据库连接插件，需要注入多数据池数据源选择器，在 Executor close 方法l拦截，用于关闭自定义数据源
 */

@Slf4j
@Data
@Intercepts({
        @Signature(type = Executor.class, method = MultiDataSourceExecutorCloseInteceptor.CLOSE_METHOD, args = {boolean.class})
})
public class MultiDataSourceExecutorCloseInteceptor extends BaseInterceptor {

    static final String CLOSE_METHOD = "close";

    private MyBatisDataSourceSelector myBatisDataSourceSelector;

    public MultiDataSourceExecutorCloseInteceptor(@NonNull MyBatisDataSourceSelector myBatisDataSourceSelector) {
        this.myBatisDataSourceSelector = myBatisDataSourceSelector;
    }

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        Object result = invocation.proceed();
        stopWatch.stop();
        if (log.isDebugEnabled()) {
            log.debug(String.format("intercept close after Invocation.proceed() costTime<%sms>", stopWatch.getTime()));
        }
        myBatisDataSourceSelector.close();
        myBatisDataSourceSelector.clearThreadLocal();
        return result;
    }


}
