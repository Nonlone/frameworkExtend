package per.nonlone.framework.mq;

import com.alibaba.fastjson.JSON;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import per.nonlone.framework.exception.ValidationException;
import per.nonlone.utils.ObjectUtils;
import per.nonlone.utils.ValidateUtils;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.ChannelAwareMessageListener;
import org.springframework.util.CollectionUtils;

import javax.validation.ConstraintViolation;
import java.io.IOException;
import java.util.Set;

/**
 * @version V1.0
 * @Description 类说明:MQ基础监听者，MessageRecover用于处理 getBodyMessage 方法异常，或者其他Mq处理异常（非业务处理异常）
 * @since 2016年12月19日
 */
@Slf4j
public abstract class BaseMqListenter<T> implements ChannelAwareMessageListener, MessageRecoverer {

    /**
     * 擦除类
     */
    protected Class<?> classOfT;

    public BaseMqListenter() {
        // 泛型擦除
        classOfT = ObjectUtils.<T>getGenericClass(getClass());
        log.info("{} mq listenter started!", classOfT.getName());
    }

    /**
     * 包裹消费
     *
     * @param message
     * @param channel
     */
    @Override
    public void onMessage(Message message, Channel channel) {
        String acceptableMessageBody = getMessageBody(message);
        if (log.isDebugEnabled()) {
            log.debug(String.format("class<%s> >>  getMessageBody message<%s>", classOfT.getName(), acceptableMessageBody));
        }
        T t = null;
        try {
            t = parseBody(acceptableMessageBody, classOfT);
            if (log.isDebugEnabled()) {
                log.debug(String.format("class<%s> >>  parseBody message<%s>", classOfT.getName(), JSON.toJSONString(t)));
            }
            if (t != null && (needJSRValidation(message) || needJSRValidation(t))) {
                //进行JSR校验
                Set<ConstraintViolation<T>> validateResultSet = ValidateUtils.validate(t);
                if (!CollectionUtils.isEmpty(validateResultSet)) {
                    throw new ValidationException(classOfT, ValidateUtils.validateResultToString(validateResultSet));
                }
            }
            onHandleMessage(t);
        } catch (Exception e) {
            log.error(String.format("onHandlerMessage error classOfT<%s> bean<%s>", classOfT.getName(), JSON.toJSONString(t)), e);
            onHandleMessageException(t,e);
        } finally {
            try {
                channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
            } catch (IOException ioe) {
                log.error(String.format("channel.basicAck error class<%s> deliverTag<%s>", classOfT.getName(), message.getMessageProperties().getDeliveryTag()), ioe);
            }
        }
    }


    /**
     * 获取消息体，提供加解密，压缩功能复写
     *
     * @param message
     * @return
     */
    protected String getMessageBody(Message message) {
        return new String(message.getBody());
    }

    /**
     * 自定义反序列化
     *
     * @param messageBody
     * @return
     */
    abstract protected T parseBody(String messageBody, Class<?> classOfT);


    /**
     * 是否进行JSR校验
     *
     * @param message
     * @return
     */
    protected boolean needJSRValidation(Message message) {
        return false;
    }

    /**
     * 是否进行JSR校验
     *
     * @param t
     * @return
     */
    protected boolean needJSRValidation(T t) {
        return false;
    }

    /**
     * 具体Bean处理抽象方法
     *
     * @param t
     */
    protected abstract void onHandleMessage(T t) throws Exception;


    /**
     * 处理 onHandleMessage 方法抛出的异常
     * @param t
     * @param throwable
     */
    protected void onHandleMessageException(T t,Throwable throwable){
        return;
    }

}