package com.feitai.base.mybatis;

import com.feitai.utils.ObjectUtils;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import javax.sql.DataSource;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 多数据池数据源
 */
public class MultipleDataSource extends AbstractRoutingDataSource {

    private static final ThreadLocal<String> dataSourceKey = new InheritableThreadLocal<String>();

    private static final Set<String> dataSourceKeySet = new HashSet<String>();

    public MultipleDataSource(Map<Object, Object> targetDataSources, DataSource defaultDataSource) {
        setTargetDataSources(targetDataSources);
        setDefaultTargetDataSource(defaultDataSource);
        // 暂存一份KeySet，作为判断使用
        Set<Object> keySet = targetDataSources.keySet();
        keySet.forEach((t) -> {
            if (t instanceof String) {
                dataSourceKeySet.add((String) t);
            }
        });
    }

    public boolean hasDataSourceKey(String key) {
        return dataSourceKeySet.contains(key);
    }

    public String getDataSourceKey() {
        return dataSourceKey.get();
    }

    public void setDataSourceKey(String dataSource) {
        dataSourceKey.set(dataSource);
    }

    @Override
    protected Object determineCurrentLookupKey() {
        return dataSourceKey.get();
    }

    /**
     * 获取数据源
     * @param key
     * @return
     */
    public DataSource getDataSouce(String key) {
        if (hasDataSourceKey(key)) {
            Map<Object, Object> targetDataSource = (Map<Object, Object>) ObjectUtils.getFieldValue(this, "targetDataSources");
            return (DataSource) targetDataSource.get(key);
        }
        return null;
    }

}
