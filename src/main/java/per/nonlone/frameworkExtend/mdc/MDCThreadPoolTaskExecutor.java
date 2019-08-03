package per.nonlone.frameworkExtend.mdc;

import org.slf4j.MDC;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.concurrent.ListenableFuture;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

public class MDCThreadPoolTaskExecutor extends ThreadPoolTaskExecutor {

    @Override
    public void execute(Runnable command) {
        super.execute(decorateContextAware(command));
    }

    @Override
    public void execute(Runnable task, long startTimeout) {
        super.execute(decorateContextAware(task), startTimeout);
    }

    @Override
    public Future<?> submit(Runnable task) {
        return super.submit(decorateContextAware(task));
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        return super.submit(decorateContextAware(task));
    }

    @Override
    public ListenableFuture<?> submitListenable(Runnable task) {
        return super.submitListenable(decorateContextAware(task));
    }

    @Override
    public <T> ListenableFuture<T> submitListenable(Callable<T> task) {
        return super.submitListenable(decorateContextAware(task));
    }


    private Runnable decorateContextAware(Runnable command) {
        final Map<String, String> contextMap = MDC.getCopyOfContextMap();
        return () -> MDCDecorator.run(contextMap, () -> command.run());
    }

    private <T> Callable<T> decorateContextAware(Callable<T> command) {
        final Map<String, String> contextMap = MDC.getCopyOfContextMap();
        return () -> MDCDecorator.<T>run(contextMap, () -> command.call());
    }


}
