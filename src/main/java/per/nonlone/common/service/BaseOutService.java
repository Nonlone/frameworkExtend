package per.nonlone.common.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.vavr.control.Try;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.framework.AopContext;
import org.springframework.stereotype.Service;
import per.nonlone.common.exception.OutServiceException;
import per.nonlone.utils.StringUtils;
import per.nonlone.utils.http.OkHttpClientUtils;
import per.nonlone.utils.jackson.JacksonUtils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.regex.Pattern;

@Slf4j
@Service
public abstract class BaseOutService {


    public static final String MSG = "msg";

    public static final String MESSAGE = "message";

    public static final String CODE = "code";

    protected static final Pattern PATTERN = Pattern.compile("[\\u4e00-\\u9fa5]+");

    protected ObjectMapper objectMapper = JacksonUtils.getCachedNormalInstance();


    /**
     * 重试器
     */
    private final static Retry retry = Retry.of(BaseOutService.class.getName(), RetryConfig.custom()
            .intervalFunction(IntervalFunction.ofRandomized(Duration.ofMillis(100)))
            .retryOnException(t->{
                // 非 OutServiceException 及其子类重试
                return !OutServiceException.class.isAssignableFrom(t.getClass());
            })
//            .ignoreExceptions(OutServiceException.class)
            .build());

    /**
     * 断路器
     */
    private final static CircuitBreaker circuitBreaker = CircuitBreaker.of(BaseOutService.class.getName(), CircuitBreakerConfig.custom()
            .ignoreException(t->{
                return OutServiceException.class.isAssignableFrom(t.getClass());
            })
            .build());


    /**
     * 获取重试器
     * @return
     */
    protected Retry getRetry(){
        return retry;
    }

    /**
     * 获取断路器
     * @return
     */
    protected CircuitBreaker getCircuitBreaker(){
        return circuitBreaker;
    }




    /**
     * 获取ObjectMapper
     *
     * @return
     */
    protected ObjectMapper getObjectMapper() {
        return this.objectMapper;
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
     * 获取服务自身
     *
     * @return
     */
    @Deprecated
    protected BaseOutService getThis() {
        try {
            if (AopContext.currentProxy() instanceof BaseOutService) {
                return (BaseOutService) AopContext.currentProxy();
            }
        } catch (IllegalStateException ise) {
            // Aop 服务为空
            log.warn(String.format("getThis() is useless but error %s",ise.getMessage()));
        }
        return this;
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
                return JacksonUtils.stringToObject(getObjectMapper(), t, type);
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
                        message = String.format(" operate parse error message <%s> %s  url<%s> request<%s>  response<%s>", e.getMessage(), addtionalMessage, url, JacksonUtils.toJSONString(request), responseBody);
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
                message = String.format(" operate fail %s url<%s> request<%s> response<%s>", addtionalMessage, url, JacksonUtils.toJSONString(request), JacksonUtils.toJSONString(response));
                log.warn(message);
                throw buildOutServiceException(responseBody, message, null);
            } catch (IOException ioe) {
                message = String.format(" operate io error message<%s> %s  url<%s> request<%s> ", ioe.getMessage(), addtionalMessage, url, JacksonUtils.toJSONString(request));
                log.error(message, ioe);
                throw new OutServiceException(message, ioe);
            }
        };
        // 获取重试器包裹
        if(Objects.nonNull(getRetry())){
            callable = Retry.decorateCallable(getRetry(),callable);
        }
        // 获取短路器包裹
        if(Objects.nonNull(getCircuitBreaker())){
            callable = getCircuitBreaker().decorateCallable(callable);
        }
        // 执行结果
        Try<R> result = Try.<R>ofCallable(callable);
        if(result.isSuccess()){
            return result.get();
        }else if(result.isFailure()){
            if(OutServiceException.class.isInstance(result.failed().get())){
                throw (OutServiceException)result.failed().get();
            }else{
                throw new OutServiceException(result.failed().get());
            }
        }
        throw new OutServiceException();
    }


    public Map<String, String> convertToRequestParameters(Object request) throws UnsupportedEncodingException {
        return convertToRequestParameters(request, true, StandardCharsets.UTF_8);
    }

    public Map<String, String> convertToRequestParameters(Object request, boolean isEncode, Charset charset) throws UnsupportedEncodingException {
        Map<String, String> params = new HashMap<>();
        for (Map.Entry<String, Object> entry : JacksonUtils.toJSONMap(getObjectMapper(), request).entrySet()) {
            if (Objects.nonNull(entry.getValue()) && (entry.getValue() instanceof String) && StringUtils.isNotBlank((String) entry.getValue())) {
                params.put(entry.getKey(), isEncode ? URLEncoder.encode((String) entry.getValue(), charset.name()) : (String) entry.getValue());
            } else if (Objects.nonNull(entry.getValue()) && !(entry.getValue() instanceof String)) {
                params.put(entry.getKey(), isEncode ? URLEncoder.encode(entry.getValue().toString(), charset.name()) : entry.getValue().toString());
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
                    return JacksonUtils.stringToObject(getObjectMapper(), t, type);
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
                return JacksonUtils.stringToObject(getObjectMapper(), t, type);
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
        Callable<R> callable = ()->{
            String message;
            try {
                T response = null;
                String responseBody = OkHttpClientUtils.postReturnBody(url, request);
                if (StringUtils.isNotBlank(responseBody)) {
                    try {
                        response = parseFunction.apply(responseBody);
                    } catch (Exception e) {
                        message = String.format(" operateWithForm parse error message<%s> %s  url<%s> request<%s> response<%s>", e.getMessage(), addtionalMessage, url, JacksonUtils.toJSONString(request), responseBody);
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
                message = String.format(" operateWithForm fail %s url<%s> request<%s> response<%s>", addtionalMessage, url, JacksonUtils.toJSONString(request), JacksonUtils.toJSONString(response));
                log.warn(message);
                throw buildOutServiceException(responseBody, message, null);
            } catch (IOException e) {
                message = String.format(" operateWithForm io error message<%s> %s  url<%s> request<%s> ", e.getMessage(), addtionalMessage, url, JacksonUtils.toJSONString(request));
                log.error(message, e);
                throw new OutServiceException(message, e);
            }
        };
        Try<R> result = Try.<R>ofCallable(CircuitBreaker.decorateCallable(circuitBreaker, Retry.decorateCallable(retry, callable)));
        if(result.isSuccess()){
            return result.get();
        }else if(result.isFailure()){
            if(OutServiceException.class.isInstance(result.failed().get())){
                throw (OutServiceException)result.failed().get();
            }else{
                throw new OutServiceException(result.failed().get());
            }
        }
        throw new OutServiceException();

    }



    private OutServiceException buildOutServiceException(String responseBody, String details, Exception e) {
        Map<String, Object> json = null;
        try {
            json = JacksonUtils.toJSONMap(getObjectMapper(), responseBody);
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
        if (StringUtils.isNotBlank(outServiceException.getMessage()) && !PATTERN.matcher(outServiceException.getMessage()).find()) {
            outServiceException.setResponseMessage(null);
        }
        return outServiceException;
    }
}
