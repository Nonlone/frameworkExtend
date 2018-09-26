package com.feitai.backend.base.mybatis;

import com.feitai.utils.ObjectUtils;
import com.feitai.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import tk.mybatis.mapper.MapperException;
import tk.mybatis.mapper.common.Mapper;
import tk.mybatis.mapper.entity.Example;
import tk.mybatis.mapper.util.Sqls;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;

/**
 * 单个实体映射处理类
 */
@Slf4j
public class OneAnnotationFieldWalkProcessor implements ObjectUtils.FieldWalkProcessor {

    private ApplicationContext applicationContext;

    public OneAnnotationFieldWalkProcessor(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public boolean isEffected(Field field, Object object) {
        boolean result = true;
        if (!field.isAnnotationPresent(One.class)
                || field.getType().isArray()
                || field.getType().isPrimitive()
                || Modifier.isStatic(field.getType().getModifiers())
                || Collection.class.isAssignableFrom(field.getType())) {
            // 判断类型
            result = false;
        }
        try {
            Object value = field.get(object);
            if (value != null) {
                result = false;
            }
        } catch (IllegalAccessException e) {
            log.error(String.format("isEffected class<%s> field<%s> error %s", object.getClass(), field.getName(), e.getMessage()), e);
        }
        if (log.isDebugEnabled()) {
            log.debug(String.format("class<%s> field<%s> isEffected<%s>", object.getClass(), field.getName(), result));
        }
        return result;
    }

    @Override
    public Object process(Field field, Object object, ObjectUtils.FieldWalkProcessor fieldWalkProcessor) {
        One one = field.getAnnotation(One.class);
        Class<?> classOfMapper = one.classOfMapper();
        String sourceField = one.sourceField();
        String targerField = one.targetField();
        Mapper mapper = (Mapper) applicationContext.getBean(classOfMapper);
        if (mapper == null) {
            return null;
        }
        try {
            Object source = ObjectUtils.getFieldValue(object,sourceField);
            log.info(String.format("process class<%s> field<%s> source<%s>", object.getClass(), field.getName(), source));
            Object value = mapper.selectOneByExample(Example.builder(field.getType()).andWhere(Sqls.custom().andEqualTo(targerField, source)).build());
            if (value != null) {
                return ObjectUtils.fieldWalkProcess(value, fieldWalkProcessor);
            }
        } catch (MapperException e) {
            log.error(String.format("mapper process class<%s> field<%s> error %s", object.getClass(), field.getName(), e.getMessage()), e);
        } catch (Exception e){
            log.error(String.format("process class<%s> field<%s> error %s", object.getClass(), field.getName(), e.getMessage()), e);
        }
        return null;
    }

}
