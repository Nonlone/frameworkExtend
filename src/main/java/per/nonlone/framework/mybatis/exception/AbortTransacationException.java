package per.nonlone.framework.mybatis.exception;

/**
 * 打断事务异常
 */
public class AbortTransacationException extends RuntimeException {

    public AbortTransacationException() {
    }

    public AbortTransacationException(String message) {
        super(message);
    }

    public AbortTransacationException(String message, Throwable cause) {
        super(message, cause);
    }

    public AbortTransacationException(Throwable cause) {
        super(cause);
    }

    public AbortTransacationException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
