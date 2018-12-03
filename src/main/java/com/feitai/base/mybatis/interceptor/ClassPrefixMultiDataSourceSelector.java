package com.feitai.base.mybatis.interceptor;

import com.feitai.base.mybatis.MultipleDataSource;
import com.feitai.base.mybatis.interceptor.ClassPrefixMultiDataSourceInterceptor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;

/**
 * 类前缀数据源选择器
 */
public class ClassPrefixMultiDataSourceSelector {

    public ClassPrefixMultiDataSourceSelector(ClassPrefixMultiDataSourceInterceptor classPrefixMultiDataSourceInterceptor, MultipleDataSource multipleDataSource) {
        this.classPrefixMultiDataSourceInterceptor = classPrefixMultiDataSourceInterceptor;
        this.multipleDataSource = multipleDataSource;
    }

    private ClassPrefixMultiDataSourceInterceptor classPrefixMultiDataSourceInterceptor;

    private MultipleDataSource multipleDataSource;

    /**
     * 获取指定类数据源
     *
     * @param classOfT
     * @return
     */
    public DataSource getDataSource(Class<?> classOfT) {
        String key = classPrefixMultiDataSourceInterceptor.getKey(classOfT);
        if (StringUtils.isNotBlank(key) && multipleDataSource.hasDataSourceKey(key)) {
            return multipleDataSource.getDataSouce(key);
        }
        return null;
    }

    /**
     * 获取指定类数据源
     *
     * @param classOfT
     * @return
     */
    public String getDataSourceKey(Class<?> classOfT) {
        String key = classPrefixMultiDataSourceInterceptor.getKey(classOfT);
        if (StringUtils.isNotBlank(key) && multipleDataSource.hasDataSourceKey(key)) {
            return key;
        }
        return null;
    }


}
