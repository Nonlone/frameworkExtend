package per.nonlone.common.service;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.vavr.control.Try;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import per.nonlone.common.exception.OutServiceException;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.Callable;

/**
 * 使用 resilience4j 增强
 */
@Slf4j
@Service
public abstract class BaseResilience4jOutService extends BaseOutService {


    public BaseResilience4jOutService() {
        super(t -> {
            Callable<?> callable = t;
            // 获取重试器包裹
            if (Objects.nonNull(retry)) {
                callable = Retry.decorateCallable(retry, callable);
            }
            // 获取短路器包裹
            if (Objects.nonNull(circuitBreaker)) {
                callable = circuitBreaker.decorateCallable(callable);
            }
            final Callable<?> finalCallable = callable;
            return () -> {
                // 执行结果
                Try result = Try.ofCallable(finalCallable);
                if (result.isSuccess()) {
                    return result.get();
                } else if (result.isFailure()) {
                    if (OutServiceException.class.isInstance(result.failed().get())) {
                        throw (OutServiceException) result.failed().get();
                    } else {
                        throw new OutServiceException(result.failed().getCause());
                    }
                }
                throw new OutServiceException();
            };
        });
    }


    /**
     * 重试器
     */
    @Setter
    protected static Retry retry = Retry.of(BaseOutService.class.getName(), RetryConfig.custom()
            .intervalFunction(IntervalFunction.ofRandomized(Duration.ofMillis(100)))
            .retryOnException(t -> !OutServiceException.class.isAssignableFrom(t.getClass()))
            .build());

    /**
     * 断路器
     */
    @Setter
    protected static CircuitBreaker circuitBreaker = CircuitBreaker.of(BaseOutService.class.getName(), CircuitBreakerConfig.custom()
            .ignoreException(t -> OutServiceException.class.isAssignableFrom(t.getClass()))
            .build());

}

