package per.nonlone.common.configure;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import per.nonlone.common.service.AsyncService;
import per.nonlone.framework.mdc.MDCThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * @Async 线程池配置
 */
@Configuration
@Slf4j
public class AsyncThreadPoolTaskExecutorAutoConfiguration {

    public static int DEFAULT_TIMES_OF_CORE_POOL_SIZE = 2;

    public static int DEFAULT_TIMES_OF_MAX_POOL_SIZE = 10;

    public static int DEFAULT_TIMES_OF_MAX_QUEUE = 10;

    public static final Integer DEFAULT_KEEP_ALIVE_SECONDS = 600;

    private Integer corePoolSize = Runtime.getRuntime().availableProcessors() * DEFAULT_TIMES_OF_CORE_POOL_SIZE;

    private Integer maxPoolSize = corePoolSize * DEFAULT_TIMES_OF_MAX_POOL_SIZE;

    private Integer maxQueueCapacity = maxPoolSize * DEFAULT_TIMES_OF_MAX_QUEUE;

    @Primary
    @Bean
//    @ConditionalOnMissingBean(ThreadPoolExecutorTaskExecutor.class)
    @ConditionalOnBean(AsyncService.class)
    public ThreadPoolTaskExecutor threadPoolTaskExecutor() {
        MDCThreadPoolTaskExecutor executor = new MDCThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(maxQueueCapacity);
        executor.setKeepAliveSeconds(DEFAULT_KEEP_ALIVE_SECONDS);
        executor.setThreadNamePrefix("asyncThreadPool");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.initialize();
        log.info(String.format("init threadPoolTaskExecutor"));
        return executor;
    }

}
