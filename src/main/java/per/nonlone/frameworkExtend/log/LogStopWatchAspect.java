package per.nonlone.frameworkExtend.log;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import per.nonlone.frameworkExtend.log.annotation.LogStopWatch;

/**
 * LogStopWatch Aop类
 */
@Component
@Aspect
@Slf4j
public class LogStopWatchAspect {

    private Logger getLog(JoinPoint joinPoint) {
        return LoggerFactory.getLogger(joinPoint.getTarget().getClass());
    }


    @Around("@annotation(logStopWatch)")
    public Object around(ProceedingJoinPoint proceedingJoinPoint, LogStopWatch logStopWatch) throws Throwable {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        Object result = null;
        try {
            result = proceedingJoinPoint.proceed();
            return result;
        } catch (Throwable t) {
            throw t;
        } finally {
            stopWatch.stop();
            Logger logger = getLog(proceedingJoinPoint);
            if (logger.isDebugEnabled()) {
                log.debug(String.format("call class<%s> method<%s> costTime<%sms>", proceedingJoinPoint.getTarget().getClass(), proceedingJoinPoint.getSignature().getName(), stopWatch.getTime()));
            }
        }
    }
}
