package per.nonlone.framework.mybatis.interceptor;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Signature;

import java.sql.Connection;


@Slf4j
@Intercepts({
        @Signature(type = StatementHandler.class, method = "prepare", args = {Connection.class, Integer.class})
})
public class BeanSQLInterceptor extends StatementHandlerInterceptor {

    private BeanSQLHandler beanSQLHandler;

    /**
     * 构造注入
     *
     * @param beanSQLHandler
     */
    public BeanSQLInterceptor(@NonNull BeanSQLHandler beanSQLHandler) {
        super(t->{
            StatementHandler handler = getStatementHandlerFromInvocationOfStatementHandler(t);
            BoundSql boundSql = handler.getBoundSql();
            Object object = boundSql.getParameterObject();
            Class<?> constrainClass = beanSQLHandler.getAutoBeanConstraintClass();
            if (constrainClass != null && constrainClass.isInstance(object)) {
                beanSQLHandler.handleBoundSqlAndParameterObject(boundSql, constrainClass.cast(object));
            }
            return null;
        });
        this.beanSQLHandler = beanSQLHandler;
    }
}
