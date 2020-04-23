package per.nonlone.framework.mdc;

import com.netflix.hystrix.strategy.concurrency.HystrixConcurrencyStrategy;
import org.slf4j.MDC;

import java.util.Map;
import java.util.concurrent.Callable;

public class MdcHystrixConcurrencyStrategy extends HystrixConcurrencyStrategy {

    @Override
    public <T> Callable<T> wrapCallable(Callable<T> callable) {
        final Map<String, String> contextMap = MDC.getCopyOfContextMap();
        return () -> MDCDecorator.<T>run(contextMap, () -> callable.call());
    }

}
