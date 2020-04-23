package per.nonlone.common.exception;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 外部服务调用异常
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper=false)
public class OutServiceException extends RuntimeException  {

    /**
     * 返回错误码
     */
    protected Integer code;

    /**
     * 返回内容
     */
    protected String response;

    /**
     * 返回内容解析
     */
    protected String responseMessage;

    /**
     * 返回详细
     */
    protected String details;


    public OutServiceException(Throwable throwable) {
        super(throwable);
    }

    public OutServiceException(String message) {
        super(message);
    }

    public OutServiceException(String message, Throwable cause) {
        super(message, cause);
    }


    public OutServiceException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
