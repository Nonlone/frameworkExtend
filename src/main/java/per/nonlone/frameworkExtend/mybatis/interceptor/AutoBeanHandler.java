package per.nonlone.frameworkExtend.mybatis.interceptor;

import org.apache.ibatis.mapping.BoundSql;

public interface AutoBeanHandler<T> {

    Class<T> getAutoBeanConstraintClass();

    void handleBoundSqlAndParameterObject(BoundSql boundSql, T t);
}
