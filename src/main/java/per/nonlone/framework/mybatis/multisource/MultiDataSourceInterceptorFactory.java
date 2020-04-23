package per.nonlone.framework.mybatis.multisource;

import lombok.NonNull;
import org.apache.ibatis.plugin.Interceptor;
import org.springframework.beans.factory.FactoryBean;

import java.util.ArrayList;
import java.util.List;

/**
 * 多数据源拦截链工厂类，使用时直接 @Bean 构建即可
 */
public class MultiDataSourceInterceptorFactory implements FactoryBean<List<Interceptor>> {


    private MyBatisDataSourceSelector myBatisDataSourceSelector;

    public MultiDataSourceInterceptorFactory(@NonNull MyBatisDataSourceSelector myBatisDataSourceSelector) {
        this.myBatisDataSourceSelector = myBatisDataSourceSelector;
    }


    @Override
    public List<Interceptor> getObject() throws Exception {
        List<Interceptor> interceptorList = new ArrayList<>();
        interceptorList.add(new MultiDataSourceExecutorCloseInteceptor(myBatisDataSourceSelector));
        interceptorList.add(new MultiDataSourceExecutorInteceptor(myBatisDataSourceSelector));
        return interceptorList;
    }

    @Override
    public Class<?> getObjectType() {
        return List.class;
    }

}
