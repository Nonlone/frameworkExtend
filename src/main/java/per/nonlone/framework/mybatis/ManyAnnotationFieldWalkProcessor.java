package per.nonlone.framework.mybatis;

import per.nonlone.utils.ObjectUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import tk.mybatis.mapper.MapperException;
import tk.mybatis.mapper.common.Mapper;
import tk.mybatis.mapper.entity.Example;
import tk.mybatis.mapper.util.Sqls;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

/**
 * 多个实体处理类
 */
@Slf4j
public class ManyAnnotationFieldWalkProcessor implements ObjectUtils.FieldWalkProcessor {

    private ApplicationContext applicationContext;

    public ManyAnnotationFieldWalkProcessor(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public boolean isEffected(Field field, Object object) {
        boolean result = true;
        if (!field.isAnnotationPresent(Many.class)
                || field.getType().isPrimitive()
                || Modifier.isStatic(field.getType().getModifiers())
                || !(field.getType().isArray() || Collection.class.isAssignableFrom(field.getType()))) {
            // 判断类型，只要为数组或者集合类型，即可
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
        Many many = field.getAnnotation(Many.class);
        Class<?> classOfMapper = many.classOfMapper();
        String sourceField = many.sourceField();
        String targerField = many.targetField();
        Class<?> sourceClass = many.classOfEntity();
        Mapper mapper = (Mapper) applicationContext.getBean(classOfMapper);
        if (mapper == null) {
            return null;
        }
        try {
            Object source = ObjectUtils.getFieldValue(object, sourceField);
            if(log.isDebugEnabled()){
                log.debug(String.format("process class<%s> field<%s> source<%s>", object.getClass(), field.getName(), source));
            }
            if (Objects.isNull(source)) {
                return null;
            }
            Object value = mapper.selectByExample(Example.builder(sourceClass).andWhere(Sqls.custom().andEqualTo(targerField, source)).build());
            if (value != null && Collection.class.isAssignableFrom(value.getClass())) {
                Collection collection = (Collection) value;
                Iterator iterator = collection.iterator();
                while (iterator.hasNext()) {
                    Object subObject = sourceClass.cast(iterator.next());
                    ObjectUtils.fieldWalkProcess(subObject, fieldWalkProcessor);
                }
                return (List) value;
            }
        } catch (MapperException e) {
            log.error(String.format("mapper process class<%s> field<%s> error %s", object.getClass(), field.getName(), e.getMessage()), e);
        } catch (Exception e) {
            log.error(String.format("process class<%s> field<%s> error %s", object.getClass(), field.getName(), e.getMessage()), e);
        }
        return null;
    }

}
