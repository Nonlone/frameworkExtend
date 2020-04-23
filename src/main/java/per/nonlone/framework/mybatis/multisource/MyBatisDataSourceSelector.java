package per.nonlone.framework.mybatis.multisource;

import javax.sql.DataSource;
import java.sql.Connection;

public interface MyBatisDataSourceSelector {

    /**
     * 获取当前连接
     *
     * @return
     */
    Connection getCurrentConnection();

    /**
     * 获取当前数据池
     *
     * @return
     */
    DataSource getCurrentDataSource();

    /**
     * 获取映射Key
     *
     * @param mapperClass
     * @return
     */
    String getKey(Class<?> mapperClass);

    /**
     * 根据Mapper类，获取数据库连接，获取数据库连接
     *
     * @param mapperClass Mapper类
     * @return connection 新的Connection连接
     */
    Connection getConnection(Class<?> mapperClass);

    /**
     * 根据Mapper类，获取数据源
     *
     * @param mapperClass
     * @return
     */
    DataSource getDataSource(Class<?> mapperClass);


    /**
     * 关闭连接
     *
     * @return
     */
    void close();


    /**
     * 是否首次事务
     *
     * @return
     */
    boolean isFirstTranscation();


    /**
     * 标记首次事务开始
     *
     * @return true为开始成功，false为开始事务失败
     */
    boolean markFirstTranscationBegin();


    /**
     * 标记首次事务结束
     *
     * @return
     */
    boolean markFirstTranscationFinish();


    /**
     * 清除缓存
     */
    void clearThreadLocal();

    /**
     * 设置 connection ThreacLocal
     *
     * @param connection
     */
    void setThreadLocalConnection(Connection connection);

    /**
     * 设置 dataSource ThreadLocal
     *
     * @param dataSource
     */
    void setThreadLocalDataSource(DataSource dataSource);

    /**
     * 标记事务出现 未提交数据
     *
     * @return
     */
    boolean markTransactionDirty();


    /**
     * 检查是否出现事务脏数据
     *
     * @return
     */
    boolean isTransactionDirty();

}
