package per.nonlone.frameworkExtend.mybatis.multisource;

import org.apache.ibatis.executor.BaseExecutor;
import org.apache.ibatis.executor.CachingExecutor;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Invocation;
import per.nonlone.frameworkExtend.mybatis.interceptor.BaseInterceptor;
import per.nonlone.utilsExtend.ObjectUtils;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public abstract class BaseExecutorInterceptor extends BaseInterceptor {

    /**
     * 判断该方法是否有 MappedStatement 参数
     */
    private static final Set<String> mappedStatementMethodSet = new HashSet<String>() {{
        this.add("update");
        this.add("query");
        this.add("queryCursor");
        this.add("createCacheKey");
        this.add("isCached");
        this.add("deferLoad");
    }};

    protected BaseExecutor getBaseExecutor(Invocation invocation) {
        if (invocation.getTarget() instanceof BaseExecutor) {
            return (BaseExecutor) invocation.getTarget();
        } else if (invocation.getTarget() instanceof CachingExecutor) {
            // 代理类
            CachingExecutor cachingExecutor = (CachingExecutor) invocation.getTarget();
            Executor delegate = (Executor) ObjectUtils.getFieldValue(cachingExecutor, "delegate");
            if (Objects.nonNull(delegate) && delegate instanceof BaseExecutor) {
                return (BaseExecutor) delegate;
            }
        }
        return null;
    }


    /**
     * 获取 MappedStatement
     *
     * @param invocation
     * @return
     */
    protected MappedStatement getMappedStatement(Invocation invocation) {
        String methocName = invocation.getMethod().getName();
        if (!mappedStatementMethodSet.contains(methocName)) {
            return null;
        }
        Object[] args = invocation.getArgs();
        if (args.length > 0 && Objects.nonNull(args[0])) {
            if (args[0] instanceof MappedStatement) {
                return (MappedStatement) args[0];
            }
        }
        return null;
    }
}
