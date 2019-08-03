package per.nonlone.frameworkExtend.mdc;

import lombok.NonNull;
import org.slf4j.MDC;
import per.nonlone.utilsExtend.CollectionUtils;
import per.nonlone.utilsExtend.StringUtils;

import java.util.Map;
import java.util.concurrent.Callable;

/**
 * 附加MDC 装饰器
 *
 * @param <T>
 */
public abstract class MDCDecorator<T> {

    public static <T> T run(Map<String, String> newContextMap, @NonNull Callable<T> callable) throws Exception {
        if (CollectionUtils.isNotEmpty(newContextMap)) {
            // 缓存原来值
            Map<String, String> originalContextMap = MDC.getCopyOfContextMap();
            newContextMap.entrySet().forEach(entry -> {
                MDC.put(entry.getKey(), entry.getValue());
            });
            try {
                T t = callable.call();
                return t;
            } finally {
                newContextMap.keySet().forEach(k -> {
                    if (CollectionUtils.isNotEmpty(originalContextMap)
                            && originalContextMap.containsKey(k)) {
                        MDC.put(k, originalContextMap.get(k));
                    } else {
                        MDC.remove(k);
                    }
                });
            }
        }
        return callable.call();
    }


    public static <T> T run(String key, String value, @NonNull Callable<T> callable) throws Exception {
        if (StringUtils.isNotBlank(key) && StringUtils.isNotBlank(value)) {
            // MDC 内容原始值
            String originalValue = MDC.get(key);
            MDC.put(key, value);
            try {
                T t = callable.call();
                return t;
            } finally {
                if (StringUtils.isNotBlank(originalValue)) {
                    MDC.put(key, originalValue);
                } else {
                    MDC.remove(key);
                }
            }
        }
        return callable.call();
    }


    public static void run(Map<String, String> newContextMap, @NonNull Runnable runnable) {
        if (CollectionUtils.isNotEmpty(newContextMap)) {
            // 缓存原来值
            Map<String, String> originalContextMap = MDC.getCopyOfContextMap();
            newContextMap.entrySet().forEach(entry -> {
                MDC.put(entry.getKey(), entry.getValue());
            });
            try {
                runnable.run();
            } finally {
                newContextMap.keySet().forEach(k -> {
                    if (CollectionUtils.isNotEmpty(originalContextMap)
                            && originalContextMap.containsKey(k)) {
                        MDC.put(k, originalContextMap.get(k));
                    } else {
                        MDC.remove(k);
                    }
                });
            }
        } else {
            runnable.run();
        }
    }

    public static void run(String key, String value, @NonNull Runnable runnable) {
        if (StringUtils.isNotBlank(key) && StringUtils.isNotBlank(value)) {
            // MDC 内容原始值
            String originalValue = MDC.get(key);
            MDC.put(key, value);
            try {
                runnable.run();
            } finally {
                if (StringUtils.isNotBlank(originalValue)) {
                    MDC.put(key, originalValue);
                } else {
                    MDC.remove(key);
                }
            }
        } else {
            runnable.run();
        }
    }

}
