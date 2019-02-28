package per.nonlone.frameworkExtend.mybatis.multisource.interceptor;

import per.nonlone.frameworkExtend.mybatis.interceptor.BaseInterceptor;
import per.nonlone.frameworkExtend.mybatis.multisource.MyBatisDataSourceSelector;
import lombok.Data;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;

/**
 * MyBatis 自动分配数据库连接插件，需要注入多数据池数据源选择器，在 Executor close 方法l拦截，用于关闭自定义数据源
 */

@Slf4j
@Data
@Intercepts({
        @Signature(type = Executor.class, method = "close", args = {})
})
class MultiDataSourceExecutorCloseInteceptor extends BaseInterceptor {

    private MyBatisDataSourceSelector myBatisDataSourceSelector;

    public MultiDataSourceExecutorCloseInteceptor(@NonNull MyBatisDataSourceSelector myBatisDataSourceSelector) {
        this.myBatisDataSourceSelector = myBatisDataSourceSelector;
    }

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        myBatisDataSourceSelector.close();
        Object result = invocation.proceed();
        if (log.isDebugEnabled()) {
            log.debug("op<intercept> after Invocation.proceed()");
        }
        return result;
    }


}
