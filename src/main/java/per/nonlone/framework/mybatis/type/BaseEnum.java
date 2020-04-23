package per.nonlone.framework.mybatis.type;

public interface BaseEnum<E extends Enum<?>, T> {

    /**
     * 获取值
     *
     * @return
     */
    T getValue();

}
