package per.nonlone.framework.log;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import org.springframework.stereotype.Component;
import per.nonlone.framework.log.annotation.Log;
import per.nonlone.framework.log.annotation.LogIngore;
import per.nonlone.utils.CollectionUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

/**
 * 日志Aop类
 */
@Component
@Aspect
@Slf4j
public class LogAspect {

    private static final String LOG_TEMPLATE_BEFORE = " call method<%s> arguments %s";

    private static final String LOG_TEMPLATE_AFTER = "call finish method<%s> arguments %s";

    private static final String LOG_TEMPALTE_THROWING = "call error %s method<%s> arguments %s";

    private static final String LOG_TEMPLATE_AROUND = "call method<%s> return<%s> arguments %s";


    /**
     * 前置通知输出日志
     *
     * @param joinPoint
     * @param logAnnotation
     */
    @Before("@annotation(logAnnotation)")
    public void doBefore(JoinPoint joinPoint, Log logAnnotation) {
        if (isLog(logAnnotation, LogPoint.BEFORE)) {
            String logMessage = String.format(LOG_TEMPLATE_BEFORE, joinPoint.getSignature().getName(), getArgumentsMessage(joinPoint));
            logMessage(getLog(joinPoint), logAnnotation.level(), logMessage);
        }
    }

    /**
     * 后置通知输出日志
     *
     * @param joinPoint
     * @param logAnnotation
     */
    @After("@annotation(logAnnotation)")
    public void doAfter(JoinPoint joinPoint, Log logAnnotation) {
        if (isLog(logAnnotation, LogPoint.AFTER)) {
            String logMessage = String.format(LOG_TEMPLATE_AFTER, joinPoint.getSignature().getName(), getArgumentsMessage(joinPoint));
            logMessage(getLog(joinPoint), logAnnotation.level(), logMessage);
        }
    }


    /**
     * 异常时输出日志
     *
     * @param joinPoint
     * @param throwable
     * @param logAnnotation
     */
    @AfterThrowing(pointcut = "@annotation(logAnnotation)", throwing = "throwable")
    public void doAfterThrowing(JoinPoint joinPoint, Throwable throwable, Log logAnnotation) {
        if (isLog(logAnnotation, LogPoint.THROWING)) {
            String logMessage = String.format(LOG_TEMPALTE_THROWING, throwable.getMessage(), joinPoint.getSignature().getName(), getArgumentsMessage(joinPoint));
            Logger logger = getLog(joinPoint);
            if (logger.isErrorEnabled()) {
                logger.error(logMessage, throwable);
            }
        }
    }


    @Around("@annotation(logAnnotation)")
    public Object around(ProceedingJoinPoint proceedingJoinPoint, Log logAnnotation) throws Throwable {
        Object result = null;
        try {
            result = proceedingJoinPoint.proceed();
            String logResult = "";
            if (isLog(logAnnotation, LogPoint.AROUND) && logAnnotation.isLogReturn()) {
                try {
                    logResult = JSON.toJSONString(result);
                } catch (JSONException jsone) {
                    log.error(String.format("json toJSONString error %s", jsone.getMessage()), jsone);
                    logResult = result.toString();
                }
            } else if (!logAnnotation.isLogReturn()) {
                logResult = result.getClass().getName();
            }
            String logMessage = String.format(LOG_TEMPLATE_AROUND, proceedingJoinPoint.getSignature().getName(), logResult, getArgumentsMessage(proceedingJoinPoint));
            logMessage(getLog(proceedingJoinPoint), logAnnotation.level(), logMessage);
            return result;
        } catch (Throwable t) {
            throw t;
        }
    }


    private Logger getLog(JoinPoint joinPoint) {
        return LoggerFactory.getLogger(joinPoint.getTarget().getClass());
    }

    private void logMessage(@NonNull Logger logger, @NonNull Level level, String message) {
        switch (level) {
            case ERROR:
                if (logger.isErrorEnabled()) {
                    logger.error(message);
                }
                break;
            case DEBUG:
                if (logger.isDebugEnabled()) {
                    logger.debug(message);
                }
                break;
            case INFO:
                if (logger.isInfoEnabled()) {
                    logger.info(message);
                }
                break;
            case TRACE:
                if (logger.isTraceEnabled()) {
                    logger.trace(message);
                }
                break;
            case WARN:
                if (logger.isWarnEnabled()) {
                    logger.warn(message);
                }
                break;
            default:
                break;
        }
    }


    private boolean isLog(Log logAnnotation, LogPoint logPoint) {
        for (LogPoint location : logAnnotation.logPoint()) {
            if (location.equals(logPoint)) {
                return true;
            }
        }
        return false;
    }

    private String getArgumentsMessage(JoinPoint joinPoint) {
        Set<Integer> logArgumentIndexSet = null;
        if (joinPoint.getSignature() instanceof MethodSignature) {
            MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
            Method method = methodSignature.getMethod();
            logArgumentIndexSet = new HashSet<>();
            Annotation[][] paramsAnnoArray = method.getParameterAnnotations();
            for (int i = 0; i < paramsAnnoArray.length; i++) {
                Annotation[] paramsAnnos = paramsAnnoArray[i];
                for (int j = 0; j < paramsAnnos.length; j++) {
                    Annotation annotation = paramsAnnos[j];
                    if (annotation instanceof LogIngore) {
                        break;
                    }
                    logArgumentIndexSet.add(i);
                }
            }
        }
        StringBuffer sb = new StringBuffer();
        Object[] args = joinPoint.getArgs();
        for (int i = 0; i < args.length; i++) {
            if (CollectionUtils.isNotEmpty(logArgumentIndexSet)
                    && !logArgumentIndexSet.contains(i)) {
                continue;
            }
            try {
                sb.append(String.format("arg[%d]<%s> ", i, JSON.toJSONString(args[i])));
            } catch (JSONException je) {
                sb.append(String.format("arg[%d]<%s> ", i, args[i].toString()));
            }
        }
        return sb.toString();
    }


}
