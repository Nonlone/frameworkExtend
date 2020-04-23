package per.nonlone.common.configure;

import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Signature;
import per.nonlone.common.service.BaseModel;
import per.nonlone.framework.mybatis.interceptor.BeanSQLHandler;
import per.nonlone.framework.mybatis.interceptor.BeanSQLInterceptor;

import java.sql.Connection;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Objects;

/**
 * 自动审计时间拦截器
 */
@Slf4j
@Intercepts({
        @Signature(type = StatementHandler.class, method = "prepare", args = {Connection.class, Integer.class})
})
public class ModelAutoDateInterceptor extends BeanSQLInterceptor {

    public ModelAutoDateInterceptor() {
        super(new BeanSQLHandler() {
            @Override
            public Class getAutoBeanConstraintClass() {
                return BaseModel.class;
            }

            @Override
            public void handleBoundSqlAndParameterObject(BoundSql boundSql, Object o) {
                BaseModel baseModel = (BaseModel) o;
                if (boundSql.getSql().toLowerCase().startsWith("insert")) {
                    if (Objects.isNull(baseModel.getCreateTime())) {
                        baseModel.setCreateTime(Date.from(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant()));
                    }
                    if (Objects.isNull(baseModel.getUpdateTime())) {
                        baseModel.setUpdateTime(Date.from(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant()));
                    }
                } else if (boundSql.getSql().toLowerCase().startsWith("update")) {
                    baseModel.setUpdateTime(Date.from(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant()));
                }
            }
        });
    }
}
