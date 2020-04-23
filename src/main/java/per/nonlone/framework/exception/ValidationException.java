package per.nonlone.framework.exception;

/**
 * 检验异常
 */
public class ValidationException extends javax.validation.ValidationException {

    public ValidationException(Class<?> clazz, String message) {
        super(String.format("class<%s>" + message, clazz.getName()));
    }

    public ValidationException(String message) {
        super(message);
    }

    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }

    public ValidationException(Throwable cause) {
        super(cause);
    }
}
