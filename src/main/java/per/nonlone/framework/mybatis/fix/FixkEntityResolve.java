package per.nonlone.framework.mybatis.fix;

import lombok.extern.slf4j.Slf4j;
import tk.mybatis.mapper.annotation.ColumnType;
import tk.mybatis.mapper.entity.Config;
import tk.mybatis.mapper.entity.EntityColumn;
import tk.mybatis.mapper.entity.EntityField;
import tk.mybatis.mapper.entity.EntityTable;
import tk.mybatis.mapper.mapperhelper.resolve.DefaultEntityResolve;
import tk.mybatis.mapper.util.SimpleTypeUtil;

import javax.persistence.Column;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * 修复父子类字段重复问题
 */
@Slf4j
public class FixkEntityResolve extends DefaultEntityResolve {


    /**
     * 修复父子类字段问题
     *
     * @param entityClass
     * @param config
     * @return
     */
    @Override
    public EntityTable resolveEntity(Class<?> entityClass, Config config) {
        EntityTable entityTable = super.resolveEntity(entityClass, config);
        // 重新修改生成Field
        entityTable.setKeyProperties(new ArrayList<>());
        entityTable.setKeyColumns(new ArrayList<>());
        entityTable.setEntityClassColumns(new LinkedHashSet<EntityColumn>());
        entityTable.setEntityClassPKColumns(new LinkedHashSet<EntityColumn>());
        //处理所有列
        List<EntityField> fields = null;
        if (config.isEnableMethodAnnotation()) {
            fields = FixkJdk8FieldReplaceFieldHelper.getAll(entityClass);
        } else {
            fields = FixkJdk8FieldReplaceFieldHelper.getFields(entityClass);
        }
        for (EntityField field : fields) {
            //如果启用了简单类型，就做简单类型校验，如果不是简单类型，直接跳过
            //3.5.0 如果启用了枚举作为简单类型，就不会自动忽略枚举类型
            //4.0 如果标记了 Column 或 ColumnType 注解，也不忽略
            if (config.isUseSimpleType()
                    && !field.isAnnotationPresent(Column.class)
                    && !field.isAnnotationPresent(ColumnType.class)
                    && !(SimpleTypeUtil.isSimpleType(field.getJavaType())
                    ||
                    (config.isEnumAsSimpleType() && Enum.class.isAssignableFrom(field.getJavaType())))) {
                continue;
            }
            processField(entityTable, field, config, config.getStyle());
        }
        //当pk.size=0的时候使用所有列作为主键
        if (entityTable.getEntityClassPKColumns().size() == 0) {
            entityTable.setEntityClassPKColumns(entityTable.getEntityClassColumns());
        }
        entityTable.initPropertyMap();
        return entityTable;
    }
}
