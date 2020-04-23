package per.nonlone.framework.log;

/**
 * @author linguocheng
 * @date 2019年1月22日
 */
public enum LogPoint {
    /**
     * 方法之前
     */
    BEFORE,
    /**
     * 异常抛出
     */
    THROWING,
    /**
     * 环绕方法
     */
    AROUND,
    /**
     * 在方法之后
     */
    AFTER
}
