package per.nonlone.framework.mdc;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import per.nonlone.framework.mdc.annotation.MDCEntry;
import per.nonlone.framework.mdc.annotation.MDCMap;
import per.nonlone.utils.CollectionUtils;
import per.nonlone.utils.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Component
@Aspect
public class MDCAspect {


    @Around("@annotation(mdcMap)")
    public Object around(ProceedingJoinPoint proceedingJoinPoint, MDCMap mdcMap) throws Throwable {
        if (log.isDebugEnabled()) {
            log.debug("pointCutMDC around " + proceedingJoinPoint.getSignature());
        }
        Map<String, String> contextMap = new HashMap<String, String>();
        if (Objects.nonNull(mdcMap) && ArrayUtils.isNotEmpty(mdcMap.value())) {
            if (ArrayUtils.isNotEmpty(mdcMap.value())) {
                for (MDCEntry mdcEntry : mdcMap.value()) {
                    if (StringUtils.isNotBlank(mdcEntry.key())
                            && StringUtils.isNotBlank(mdcEntry.value())) {
                        contextMap.put(mdcEntry.key(), mdcEntry.value());
                    }
                }
            }
            // 包裹执行
            if (CollectionUtils.isNotEmpty(contextMap)) {
                return MDCDecorator.run(contextMap, () -> {
                    try {
                        return proceedingJoinPoint.proceed();
                    } catch (Throwable throwable) {
                        if (throwable instanceof Exception) {
                            throw (Exception) throwable;
                        } else {
                            throw new Exception(throwable);
                        }
                    }
                });
            }
            return proceedingJoinPoint.proceed();
        }
        return proceedingJoinPoint.proceed();
    }
}
