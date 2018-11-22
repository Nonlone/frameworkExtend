package com.feitai.base.configuration;

import com.ctrip.framework.apollo.Config;
import com.ctrip.framework.apollo.ConfigChangeListener;
import com.ctrip.framework.apollo.ConfigService;
import com.ctrip.framework.apollo.model.ConfigChange;
import com.ctrip.framework.apollo.model.ConfigChangeEvent;
import com.ctrip.framework.apollo.spring.annotation.EnableApolloConfig;
import com.feitai.base.annotion.ApolloAutoSync;
import com.feitai.base.annotion.ApolloAutoCallBack;
import com.feitai.base.annotion.ApolloNotAync;
import com.feitai.base.apollo.AbstractApolloAutoCallBack;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.CollectionUtils;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @Describe :
 * @Author: humin
 * @Date: 2018/11/20 18:15
 */
@Slf4j
@Configuration
public class ApolloAutoConfig implements SmartInitializingSingleton, ApplicationContextAware {

    private ApplicationContext applicationContext;

    @Override
    public void afterSingletonsInstantiated() {
        Map<String, Object> apolloAutoChangeMap = applicationContext.getBeansWithAnnotation(ApolloAutoSync.class);
        if (!CollectionUtils.isEmpty(apolloAutoChangeMap)) {

            //按 namespace -> 类集合 -> 元素属性集合 划分
            Map<String,Map<Object,Map<String,Field>>> listenterMap = new HashMap<>();

            //遍历类对象
            for (Map.Entry<String, Object> entry : apolloAutoChangeMap.entrySet()) {
                Object obj = entry.getValue();
                Class<?> clazz = obj.getClass();

                //不存在的可以直接applicaiotn取
                EnableApolloConfig enableApolloConfig = (EnableApolloConfig) clazz.getAnnotation(EnableApolloConfig.class);
                //namespace
                String[] namespaces = {"application"};
                if(enableApolloConfig!=null){
                    namespaces = enableApolloConfig.value();
                }

                ConfigurationProperties configurationProperties = (ConfigurationProperties) clazz.getAnnotation(ConfigurationProperties.class);
                //如果包含了前缀的
                String prefix = "";
                if(configurationProperties!=null){
                    prefix = configurationProperties.prefix();
                }

                //获取类下需要监听更新的apollo key
                Field[] fileds = clazz.getDeclaredFields();
                Map<String,Field> fieldMap = new HashMap<>();
                for(Field filed:fileds){
                    Value valueMark = filed.getAnnotation(Value.class);
                    //是否指定不刷新
                    ApolloNotAync notChange = filed.getAnnotation(ApolloNotAync.class);
                    if(notChange!=null){
                        continue;
                    }
                    String apolloKey = "";
                    //需要监听value指定的path
                    if(valueMark!=null){
                        String apolloValeName = valueMark.value();
                        apolloKey = apolloValeName.replace("${","").replace("}","");
                    }else{
                        String filedName = filed.getName();
                        apolloKey = prefix+"."+filedName;
                    }
                    fieldMap.put(apolloKey,filed);
                }

                //如果一个类有2个namespace的分开存储
                for(String namespace:namespaces){
                    Map<Object,Map<String,Field>> namespaceMap = listenterMap.get(namespace);
                    if(namespaceMap==null){
                        namespaceMap = new HashMap<>();
                    }
                    namespaceMap.put(obj,fieldMap);
                    listenterMap.put(namespace,namespaceMap);
                }
            }

            //循环初始化监听
            for(String namespace:listenterMap.keySet()){
                Map<Object,Map<String,Field>> objMap = listenterMap.get(namespace);
                Config config = ConfigService.getConfig(namespace);
                config.addChangeListener(new ConfigChangeListener() {
                    @Override
                    public void onChange(ConfigChangeEvent changeEvent) {
                        //发生变化的nameSpace
                        String nameSpace = changeEvent.getNamespace();

                        for(Map.Entry<Object,Map<String,Field>> entryObj : objMap.entrySet()){
                            Object obj = entryObj.getKey();
                            Map<String,Field> fieldMap = entryObj.getValue();

                            //先处理数据刷新的
                            for (String key : changeEvent.changedKeys()) {
                                if(fieldMap!=null&&fieldMap.size()>0){
                                    if(fieldMap.containsKey(key)){
                                        try {
                                            ConfigChange change = changeEvent.getChange(key);
                                            Field filed =  fieldMap.get(key);
                                            filed.setAccessible(true);
                                            filed.set(obj, change.getNewValue());
                                            log.info("[ApolloAuto] NameSpace:[{}] Key:[{}] OldVal:[{}] To NewVal:{} Success",nameSpace,key,change.getOldValue(),change.getNewValue());

                                            ApolloAutoCallBack callBack = filed.getAnnotation(ApolloAutoCallBack.class);
                                            if(callBack!=null){
                                                Class<?> callBackClazz = callBack.callBack();
                                                AbstractApolloAutoCallBack apollCallBack = getCallBackService(callBackClazz);
                                                if(apollCallBack!=null){
                                                    try {
                                                        apollCallBack.callBack(nameSpace,key,change.getOldValue(),change.getNewValue());
                                                    } catch (Exception e) {
                                                        log.error("[ApolloAuto] NameSpace:[{}] Key:[{}] Change Call Back Has Error",nameSpace,key,e);
                                                    }
                                                }
                                            }

                                        } catch (IllegalAccessException e) {
                                            log.error("[ApolloAuto] NameSpace:[{}] Key:[{}] Has Error",nameSpace,key,e);
                                        }
                                    }
                                }
                            }

                            //后处理类上的回调函数
                            ApolloAutoCallBack callBack = obj.getClass().getAnnotation(ApolloAutoCallBack.class);
                            if(callBack!=null){
                                Class<?> callBackClazz = callBack.callBack();
                                AbstractApolloAutoCallBack apollCallBack = getCallBackService(callBackClazz);
                                if(apollCallBack!=null){
                                    try {
                                        apollCallBack.callBack(nameSpace);
                                    } catch (Exception e) {
                                        log.error("[ApolloAuto] NameSpace:[{}] Change Call Back Has Error",nameSpace,e);
                                    }
                                }
                            }
                        }
                    }
                });
            }
        }
    }

    /**
     * 获取回调service
     * @param callBackClazz
     * @return
     */
    private AbstractApolloAutoCallBack getCallBackService(Class<?> callBackClazz){
        AbstractApolloAutoCallBack apolloChangeCallBack = null;
        if(callBackClazz!=null){
            try {
                if(!Objects.equals(callBackClazz.getSimpleName(),"Null")){
                    if(Objects.equals(callBackClazz.getSuperclass().getSimpleName(),AbstractApolloAutoCallBack.class.getSimpleName())){
                        apolloChangeCallBack = (AbstractApolloAutoCallBack) applicationContext.getBean(callBackClazz);
                    }
                }
            } catch (Exception e) {
                log.error("[ApolloAuto] Get CallBack Service[{}] Has Error",callBackClazz.getSimpleName(),e);
            }
        }
        return apolloChangeCallBack;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
