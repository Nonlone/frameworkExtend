package com.feitai.backend.base.config;

import com.feitai.backend.base.mybatis.AutoBeanHandler;
import com.feitai.backend.base.mybatis.AutoBeanIntecepter;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class MyBatisConfiguration {

    @Bean
    public AutoBeanHandler autoBeanHandler(){
        return new AutoModelHandler();
    }

    @Bean
    public SqlSessionFactory sqlSessionFactory(DataSource dataSource, AutoBeanHandler autoBeanHandler) throws Exception {
        SqlSessionFactoryBean sqlSessionFactoryBean = new SqlSessionFactoryBean();
        sqlSessionFactoryBean.setDataSource(dataSource);
        // 加入拦截器
        sqlSessionFactoryBean.setPlugins(new Interceptor[]{
                new AutoBeanIntecepter(autoBeanHandler)
        });
        return sqlSessionFactoryBean.getObject();
    }

//    @Bean
    public SqlSessionTemplate sessionTemplate(SqlSessionFactory sqlSessionFactory) {
        return new SqlSessionTemplate(sqlSessionFactory);
    }

}
