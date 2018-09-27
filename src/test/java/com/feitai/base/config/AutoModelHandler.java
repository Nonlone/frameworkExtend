package com.feitai.base.config;

import com.feitai.base.model.BaseModel;
import com.feitai.base.mybatis.AutoBeanHandler;
import com.feitai.utils.ObjectUtils;
import org.apache.ibatis.mapping.BoundSql;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class AutoModelHandler implements AutoBeanHandler<BaseModel> {

    @Override
    public Class<BaseModel> getAutoBeanConstraintClass() {
        return ObjectUtils.getGenericClass(getClass());
    }

    @Override
    public void handleBoundSqlAndParameterObject(BoundSql boundSql, BaseModel baseModel) {
        if (boundSql.getSql().toUpperCase().startsWith("INSERT")) {
            insertAutoSetup(baseModel);
        } else if (boundSql.getSql().toUpperCase().startsWith("UPDATE")) {
            updateAutoSetup(baseModel);
        }
    }


    private void insertAutoSetup(BaseModel baseModel) {
        baseModel.setCreatedTime(new Date());
        baseModel.setUpdateTime(new Date());
    }

    private void updateAutoSetup(BaseModel baseModel) {
        baseModel.setUpdateTime(new Date());
    }
}
