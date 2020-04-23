package per.nonlone.common.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import per.nonlone.common.exception.AsyncException;
import per.nonlone.framework.log.annotation.Log;
import per.nonlone.utils.jackson.JacksonUtils;

import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.function.Function;

/**
 * 异步服务
 */
@Service
@Slf4j
@ConditionalOnWebApplication
public class AsyncService {

    @Async
    @Log
    public void execute(Runnable runnable) {
        execute(runnable, "");
    }

    @Async
    @Log
    public void execute(Runnable runnable, String message) {
        try {
            runnable.run();
            String logMessage;
            if (StringUtils.isNotBlank(message)) {
                logMessage = String.format("async execute %s ", message);
            } else {
                logMessage = String.format("async execute");
            }
            log.info(logMessage);
        } catch (Throwable throwable) {
            String errorMessage;
            if (StringUtils.isNotBlank(message)) {
                errorMessage = String.format("async execute %s error %s", message, throwable.getMessage());
            } else {
                errorMessage = String.format("async execute error %s", throwable.getMessage());
            }
            log.error(errorMessage, throwable);
            throw new AsyncException(throwable);
        }
    }


    @Async
    @Log
    public <T> void execute(Callable<T> callable) {
        execute(callable, "");
    }

    @Async
    @Log
    public <T> void execute(Callable<T> callable, String message) {
        try {
            T t = callable.call();
            String logMessage;
            if (StringUtils.isNotBlank(message)) {
                logMessage = String.format("async execute %s  return<%s>", message, JacksonUtils.toJSONString(t));
            } else {
                logMessage = String.format("async execute  return<%s>", JacksonUtils.toJSONString(t));
            }
            log.info(logMessage);
        } catch (Throwable throwable) {
            String errorMessage;
            if (StringUtils.isNotBlank(message)) {
                errorMessage = String.format("async execute %s error %s", message, throwable.getMessage());
            } else {
                errorMessage = String.format("async execute error %s", throwable.getMessage());
            }
            log.error(errorMessage, throwable);
            throw new AsyncException(throwable);
        }
    }

    @Async
    @Log
    public <T, K> void execute(T t, Function<T, K> function) {
        execute(t, function, "");
    }


    @Async
    @Log
    public <T, K> void execute(T t, Function<T, K> function, String message) {
        if (Objects.isNull(t)) {
            final T tempT = (T) new Object();
            execute(() -> function.apply(tempT), message);
        } else {
            final T tempT = t;
            execute(() -> function.apply(tempT), message + String.format(" input<%s>", JacksonUtils.toJSONString(t)));
        }
    }


}
