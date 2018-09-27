package com.feitai.base.mq;

import com.alibaba.fastjson.JSON;
import com.feitai.base.exception.ValidationException;
import com.feitai.utils.ObjectUtils;
import com.feitai.utils.ValidateUtils;
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
 * @Description 类说明:MQ基础监听者(描述)
 * @since 2016年12月19日
 */
@Slf4j
public abstract class BaseMqListenter<T> implements ChannelAwareMessageListener {

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
        T t = parseBody(acceptableMessageBody, classOfT);
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
        try {
            onHandleMessage(t);
        } catch (Exception e) {
            log.error(String.format("onHandlerMessage error classOfT<%s> bean<%s>", classOfT.getName(), JSON.toJSONString(t)), e);
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
    protected T parseBody(String messageBody, Class<?> classOfT) {
        return (T) JSON.parseObject(messageBody, classOfT);
    }


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

}