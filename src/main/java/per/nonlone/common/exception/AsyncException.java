package per.nonlone.common.exception;

/**
 * 异步异常
 */
public class AsyncException extends RuntimeException {

    public AsyncException() {
    }

    public AsyncException(Throwable throwable) {
        super(throwable);
    }

    public AsyncException(String message) {
        super(message);
    }

    public AsyncException(String message, Throwable cause) {
        super(message, cause);
    }

    public AsyncException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
