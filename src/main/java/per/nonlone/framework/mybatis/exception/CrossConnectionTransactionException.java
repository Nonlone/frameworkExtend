package per.nonlone.framework.mybatis.exception;

/**
 * 跨连接事务
 */
public class CrossConnectionTransactionException extends AbortTransacationException {

    public CrossConnectionTransactionException() {
    }

    public CrossConnectionTransactionException(String message) {
        super(message);
    }

    public CrossConnectionTransactionException(String message, Throwable cause) {
        super(message, cause);
    }

    public CrossConnectionTransactionException(Throwable cause) {
        super(cause);
    }

    public CrossConnectionTransactionException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}
