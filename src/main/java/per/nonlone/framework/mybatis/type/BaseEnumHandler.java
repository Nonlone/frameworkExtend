package per.nonlone.framework.mybatis.type;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public abstract class BaseEnumHandler<E extends BaseEnum> extends BaseTypeHandler<E> {

    /**
     * 枚举类
     */
    private Class<E> type;

    /**
     * 枚举值
     */
    private E[] enums;

    private boolean isAllowNull;

    protected BaseEnumHandler(Class<E> type) {
        this(type, false);
    }

    /**
     * 设置配置文件设置的转换类以及枚举类内容，供其他方法更便捷高效的实现
     *
     * @param type        配置文件中设置的转换类
     * @param isAllowNull 判断枚举转换是否接受 null
     */
    protected BaseEnumHandler(Class<E> type, boolean isAllowNull) {
        this.isAllowNull = isAllowNull;
        if (type == null) {
            throw new IllegalArgumentException("Type argument cannot be null");
        }
        this.type = type;
        this.enums = type.getEnumConstants();
        if (this.enums == null) {
            throw new IllegalArgumentException(type.getSimpleName() + " does not represent an enum type.");
        }
    }


    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, E parameter, JdbcType jdbcType) throws SQLException {
        ps.setObject(i, parameter.getValue(), jdbcType.TYPE_CODE);
    }

    @Override
    public E getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String i = rs.getString(columnName);
        if (rs.wasNull()) {
            return null;
        } else {
            // 根据数据库中的value值
            return getEnum(i);
        }
    }

    @Override
    public E getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String i = rs.getString(columnIndex);
        if (rs.wasNull()) {
            return null;
        } else {
            // 根据数据库中的value值
            return getEnum(i);
        }
    }

    @Override
    public E getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String i = cs.getString(columnIndex);
        if (cs.wasNull()) {
            return null;
        } else {
            // 根据数据库中的value值
            return getEnum(i);
        }
    }

    /**
     * 枚举类型转换，由于构造函数获取了枚举的子类enums，让遍历更加高效快捷
     *
     * @param value 数据库中存储的自定义value属性
     * @return value对应的枚举类
     */
    private E getEnum(String value) {
        for (E e : enums) {
            if (e.getValue().toString().equals(value)) {
                return e;
            }
        }
        if (isAllowNull) {
            return null;
        }
        throw new IllegalArgumentException("未知的枚举类型：" + value + ",请核对" + type.getSimpleName());
    }
}
