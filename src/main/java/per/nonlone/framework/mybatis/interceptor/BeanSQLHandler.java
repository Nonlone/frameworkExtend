package per.nonlone.framework.mybatis.interceptor;

import org.apache.ibatis.mapping.BoundSql;

public interface BeanSQLHandler<T> {

    Class<T> getAutoBeanConstraintClass();

    void handleBoundSqlAndParameterObject(BoundSql boundSql, T t);
}
