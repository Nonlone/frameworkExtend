package per.nonlone.common.service;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import per.nonlone.common.exception.OutServiceException;
import per.nonlone.utils.StringUtils;
import per.nonlone.utils.http.OkHttpClientUtils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.function.Function;

@Slf4j
@Service
public abstract class BaseOutService {


    public static final String MSG = "msg";

    public static final String MESSAGE = "message";

    public static final String CODE = "code";

    @Setter
    private Function<Callable<?>, Callable<?>> decorator;

    @Setter
    private BeanConvertor beanConvertor;


    /**
     * 默认Response 解析器
     */
    public interface BeanConvertor {

        <T> String serialize(T t) throws IOException;

        /**
         * 转换成Map
         * @param t
         * @param <T>
         * @return
         * @throws IOException
         */
        <T> Map<String, Object> deserializeToMap(T t) throws IOException;

        /**
         * 反序列化
         *
         * @param responseBody
         * @param classOfT
         * @param <T>
         * @return
         */
        <T> T deserialize(String responseBody, Class<T> classOfT) throws IOException;

        /**
         * 反序列化
         *
         * @param responseBody
         * @param type
         * @param <T>
         * @return
         */
        <T> T deserialize(String responseBody, Type type) throws IOException;
    }


    /**
     * 构造传反序列化工具
     * @param beanConvertor
     */
    protected BaseOutService(BeanConvertor beanConvertor) {
        this.beanConvertor = beanConvertor;
    }


    protected BaseOutService(BeanConvertor beanConvertor, Function<Callable<?>, Callable<?>> decorator) {
        this(beanConvertor);
        this.decorator = decorator;
    }


    /**
     * 包装并执行
     * @param callable
     * @param <T>
     * @return
     * @throws Exception
     */
    protected <T> T doOperateWithDecorator(final Callable<T> callable) throws Exception {
        Callable<T> executor = callable;
        if (Objects.nonNull(decorator)) {
            executor = (Callable<T>) decorator.apply(executor);
        }
        return callable.call();
    }



    /**
     * 获取异常类
     *
     * @param exception
     * @param runtimeException
     * @return
     */
    protected OutServiceException changeToOutServiceException(Exception exception, RuntimeException runtimeException) {
        if (exception instanceof OutServiceException) {
            return (OutServiceException) exception;
        }
        throw runtimeException;
    }

    /**
     * 外部请求操作，默认不接受Null
     *
     * @param url
     * @param request
     * @param type
     * @param handleFunction
     * @param addtionalMessage
     * @param <T>
     * @param <R>
     * @return
     */
    public <T, R> R operate(String url, Object request, Type type, Function<T, R> handleFunction, String addtionalMessage) {
        return this.operate(url, request, handleFunction, t -> {
            try {
                return beanConvertor.deserialize(t, type);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }, false, addtionalMessage);
    }

    /**
     * 请求外部操作
     *
     * @param request
     * @param handleFunction
     * @param parseFunction
     * @param addtionalMessage
     * @param <T>
     * @param <R>
     * @return
     */
    public <T, R> R operate(String url, Object request, Function<T, R> handleFunction, Function<String, T> parseFunction, boolean isAcceptNull, String addtionalMessage) {
        Callable<R> callable = () -> {
            String message;
            try {
                T response = null;
                String responseBody = OkHttpClientUtils.postReturnBody(url, request);
                if (StringUtils.isNotBlank(responseBody)) {
                    try {
                        response = parseFunction.apply(responseBody);
                    } catch (Exception e) {
                        message = String.format(" operate parse error message <%s> %s  url<%s> request<%s>  response<%s>", e.getMessage(), addtionalMessage, url, beanConvertor.serialize(request), responseBody);
                        log.error(message, e);
                        throw buildOutServiceException(responseBody, message, e);
                    }
                }
                R r = null;
                if (Objects.nonNull(response)) {
                    r = handleFunction.apply(response);
                }
                if (isAcceptNull || Objects.nonNull(r)) {
                    if (RequestRecordable.class.isInstance(r)) {
                        ((RequestRecordable) r).setRequest(((RequestRecordable) r).parseRequest(request));
                    }
                    if (ResponseRecordable.class.isInstance(r)) {
                        ((ResponseRecordable) r).setResponse(responseBody);
                    }
                    return r;
                }
                message = String.format(" operate fail %s url<%s> request<%s> response<%s>", addtionalMessage, url, beanConvertor.serialize(request), beanConvertor.serialize(response));
                log.warn(message);
                throw buildOutServiceException(responseBody, message, null);
            } catch (IOException ioe) {
                message = String.format(" operate io error message<%s> %s  url<%s> request<%s> ", ioe.getMessage(), addtionalMessage, url, beanConvertor.serialize(request));
                log.error(message, ioe);
                throw new OutServiceException(message, ioe);
            }
        };
        try {
            // 包装执行
            return doOperateWithDecorator(callable);
        } catch (Exception e) {
            throw new OutServiceException(e);
        }
    }


    public Map<String, String> convertToRequestParameters(Object request) throws IOException {
        return convertToRequestParameters(request, true, StandardCharsets.UTF_8);
    }

    /**
     * g构建请求参数
     * @param request
     * @param isEncode
     * @param charset
     * @return
     * @throws UnsupportedEncodingException
     */
    public Map<String, String> convertToRequestParameters(Object request, boolean isEncode, Charset charset) throws IOException {
        Map<String, String> params = new HashMap<>();
        for (Map.Entry<String, Object> entry : beanConvertor.deserializeToMap(request).entrySet()) {
            if (Objects.nonNull(entry.getValue()) && (entry.getValue() instanceof String) && StringUtils.isNotBlank((String) entry.getValue())) {
                params.put(entry.getKey(), isEncode
                        ? URLEncoder.encode((String) entry.getValue(), charset.name())
                        : (String) entry.getValue());
            } else if (Objects.nonNull(entry.getValue()) && !(entry.getValue() instanceof String)) {
                params.put(entry.getKey(), isEncode
                        ? URLEncoder.encode(entry.getValue().toString(), charset.name())
                        : entry.getValue().toString());
            }
        }
        return params;
    }


    /**
     * 请求操作，默认进行编码转换，不接受Null
     *
     * @param url
     * @param request
     * @param type
     * @param function
     * @param addtionalMessage
     * @param <T>
     * @param <R>
     * @return
     */
    public <T, R> R operateWithForm(String url, Object request, Type type, Function<T, R> function, String addtionalMessage) {
        try {
            Map<String, String> params = convertToRequestParameters(request);
            return operateWithForm(url, params, function, t -> {
                try {
                    return beanConvertor.deserialize(t, type);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }, false, addtionalMessage);
        } catch (Exception e) {
            throw new OutServiceException(e);
        }
    }


    /**
     * 请求操作，默认不接受Null
     *
     * @param url
     * @param request
     * @param type
     * @param function
     * @param addtionalMessage
     * @param <T>
     * @param <R>
     * @return
     */
    public <T, R> R operateWithForm(String url, Map<String, String> request, Type type, Function<T, R> function, String addtionalMessage) {
        return operateWithForm(url, request, function, t -> {
            try {
                return beanConvertor.deserialize(t, type);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }, false, addtionalMessage);
    }

    /**
     * 请求操作
     *
     * @param request
     * @param handleFunction
     * @param parseFunction
     * @param addtionalMessage
     * @param <T>
     * @param <R>
     * @return
     */
    public <T, R> R operateWithForm(String url, Map<String, String> request, Function<T, R> handleFunction, Function<String, T> parseFunction, boolean isAcceptNull, String addtionalMessage) {
        Callable<R> callable = () -> {
            String message;
            try {
                T response = null;
                String responseBody = OkHttpClientUtils.postReturnBody(url, request);
                if (StringUtils.isNotBlank(responseBody)) {
                    try {
                        response = parseFunction.apply(responseBody);
                    } catch (Exception e) {
                        message = String.format(" operateWithForm parse error message<%s> %s  url<%s> request<%s> response<%s>", e.getMessage(), addtionalMessage, url, beanConvertor.serialize(request), responseBody);
                        log.error(message, e);
                        throw buildOutServiceException(responseBody, message, e);
                    }
                }
                R r = null;
                if (Objects.nonNull(response)) {
                    r = handleFunction.apply(response);
                }
                if (isAcceptNull || Objects.nonNull(r)) {
                    if (RequestRecordable.class.isInstance(r)) {
                        ((RequestRecordable) r).setRequest(((RequestRecordable) r).parseRequest(request));
                    }
                    if (ResponseRecordable.class.isInstance(r)) {
                        ((ResponseRecordable) r).setResponse(responseBody);
                    }
                    return r;
                }
                message = String.format(" operateWithForm fail %s url<%s> request<%s> response<%s>", addtionalMessage, url, beanConvertor.serialize(request), beanConvertor.serialize(response));
                log.warn(message);
                throw buildOutServiceException(responseBody, message, null);
            } catch (IOException e) {
                message = String.format(" operateWithForm io error message<%s> %s  url<%s> request<%s> ", e.getMessage(), addtionalMessage, url, beanConvertor.serialize(request));
                log.error(message, e);
                throw new OutServiceException(message, e);
            }
        };
        try {
            // 包装执行
            return doOperateWithDecorator(callable);
        } catch (Exception e) {
            throw new OutServiceException(e);
        }
    }


    private OutServiceException buildOutServiceException(String responseBody, String details, Exception e) {
        Map<String, Object> json = null;
        try {
            json = beanConvertor.deserializeToMap(responseBody);
        } catch (IOException ex) {
            log.error(String.format("toJSONMap error %s resposneBody<%s>", ex.getMessage(), responseBody), ex);
            OutServiceException outServiceException = new OutServiceException();
            outServiceException.setResponseMessage(responseBody);
            outServiceException.setDetails(details);
            return outServiceException;
        }
        OutServiceException outServiceException;
        if (Objects.nonNull(e)) {
            outServiceException = new OutServiceException(e);
        } else {
            outServiceException = new OutServiceException();
        }
        outServiceException.setDetails(details);
        if (json.containsKey(CODE)) {
            if (json.get(CODE) instanceof String) {
                outServiceException.setCode(Integer.parseInt((String) json.get(CODE)));
            } else {
                outServiceException.setCode((Integer) json.get(CODE));
            }
        }
        if (json.containsKey(MSG)) {
            outServiceException.setResponseMessage((String) json.get(MSG));
        } else if (json.containsKey(MESSAGE)) {
            outServiceException.setResponseMessage((String) json.get(MESSAGE));
        }
        outServiceException.setResponse(responseBody);
        if (StringUtils.isNotBlank(outServiceException.getMessage())) {
            outServiceException.setResponseMessage(null);
        }
        return outServiceException;
    }
}
