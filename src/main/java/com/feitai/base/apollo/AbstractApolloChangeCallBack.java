package com.feitai.base.apollo;

/**
 * @Describe : apollo自动刷新出发回调
 * @Author: humin
 * @Date: 2018/11/22 11:15
 */
public abstract class AbstractApolloChangeCallBack {

    /**
     * 类对应的nameSpace变更回调
     */
    public abstract void callBack(String nameSpace);

    /**
     * 属性value变更回调
     */
    public abstract void callBack(String nameSpace,String key,String oldVal,String newVal);
}
