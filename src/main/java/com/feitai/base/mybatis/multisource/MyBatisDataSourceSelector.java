package com.feitai.base.mybatis.multisource;

import javax.sql.DataSource;
import java.sql.Connection;

public interface MyBatisDataSourceSelector {

    /**
     * 根据Mapper类，获取数据库连接，获取数据库连接
     * @param mapperClass Mapper类
     * @param connection 原来Connection连接
     * @return connection 新的Connection连接
     */
    Connection getConnection(Class<?> mapperClass, Connection connection);

    /**
     * 根据Mapper类，获取数据库连接，获取数据源
     * @param mapperClass Mapper类
     * @param connection 原来Connection连接
     * @return connection 新的Connection连接
     */
    DataSource getDataSource(Class<?> mapperClass, Connection connection);


    /**
     * 根据Mapper类，获取数据源
     * @param mapperClass
     * @return
     */
    DataSource getDataSource(Class<?> mapperClass);


    /**
     * 关闭连接
     * @return
     */
    void close();
}
