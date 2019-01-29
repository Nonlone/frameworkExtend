package com.feitai.base.aspect;

import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.feitai.base.annotion.Log;
import com.feitai.base.annotion.LogPoint;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * @author linguocheng
 * @date 2019年1月18日
 */
@Component
@Aspect
@Slf4j
public class LogAspect {

    /**
     * 计时器线程本地缓存
     */
    private final static ThreadLocal<StopWatch> stopWatchThreadLocal = new ThreadLocal<>();

    /**
     * 前置通知输出日志
     *
     * @param joinPoint
     * @param logAnnotation
     */
    @Before("@annotation(logAnnotation)")
    public void doBefore(JoinPoint joinPoint, Log logAnnotation) {
        if (isLog(logAnnotation, LogPoint.BEFORE)) {
            logByJoinPoint(joinPoint, logAnnotation);
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

    private void logByJoinPoint(JoinPoint joinPoint, Log logAnnotation) {
        switch (logAnnotation.level()) {
            case ERROR:
                log.error(getArgumentsMessage(joinPoint));
                break;
            case DEBUG:
                log.debug(getArgumentsMessage(joinPoint));
                break;
            case INFO:
                log.info(getArgumentsMessage(joinPoint));
                break;
            case TRACE:
                log.trace(getArgumentsMessage(joinPoint));
                break;
            case WARN:
                log.warn(getArgumentsMessage(joinPoint));
                break;
            default:
                break;
        }
    }

    private String getArgumentsMessage(JoinPoint joinPoint) {
        joinPoint.getSignature().getName();
        StringBuffer sb = new StringBuffer();
        sb.append(joinPoint.getSignature().getDeclaringTypeName()).append(" ")
                .append(joinPoint.getSignature().getName()).append(" ");
        Object[] args = joinPoint.getArgs();
        for (int i = 0; i < args.length; i++) {
            Object arg = args[i];
            sb.append("args" + i + "<");
            try {
                sb.append(JSONObject.toJSONString(arg));
            } catch (JSONException je) {
                sb.append(arg.toString());
            }
            sb.append("> ");
        }
        return sb.toString();
    }

    /**
     * 异常时输出日志
     *
     * @param joinPoint
     * @param e
     * @param logAnnotation
     */
    @AfterThrowing(pointcut = "@annotation(logAnnotation)", throwing = "e")
    public void doAfterThrowing(JoinPoint joinPoint, Throwable e, Log logAnnotation) {
        if (isLog(logAnnotation, LogPoint.THROWING)) {
            String message = getArgumentsMessage(joinPoint);
            if (logAnnotation.isStopWatch()) {
                StopWatch stopWatch = stopWatchThreadLocal.get();
                if (Objects.nonNull(stopWatch)) {
                    stopWatch.stop();
                    message += " stopWatch<" + stopWatch.getTime(logAnnotation.stopWatchUnit()) + ">" + logAnnotation.stopWatchUnit().name().toLowerCase();
                }
            }
            log.error(message, e);
        }
    }

    /**
     * 环线通知输出日志
     *
     * @param joinPoint
     * @param logAnnotation
     * @return
     * @throws Throwable
     */
    @Around("@annotation(logAnnotation)")
    public Object doAround(ProceedingJoinPoint joinPoint, Log logAnnotation) throws Throwable {
        StopWatch stopWatch = null;
        Object result = null;
        // 标记记录时间
        if (logAnnotation.isStopWatch()) {
            stopWatch = StopWatch.createStarted();
        }
        try {
            result = joinPoint.proceed();
        } catch (Throwable t) {
            throw t;
        } finally {
            if (stopWatch != null) {
                stopWatch.stop();
                stopWatchThreadLocal.set(stopWatch);
            }
        }
        if (isLog(logAnnotation, LogPoint.AROUND)) {
            String message = getArgumentsMessage(joinPoint);
            if (result != null) {
                try {
                    message = message + ", return<" + JSONObject.toJSONString(result) + ">";
                } catch (JSONException jsone) {
                    message = message + ", return<" + result.toString() + ">";
                }
            }
            if (logAnnotation.isStopWatch() && Objects.nonNull(stopWatch)) {
                message += " stopWatch<" + stopWatch.getTime(logAnnotation.stopWatchUnit()) + ">" + logAnnotation.stopWatchUnit().name().toLowerCase();
            }
            switch (logAnnotation.level()) {
                case ERROR:
                    log.error(message);
                    break;
                case DEBUG:
                    log.debug(message);
                    break;
                case INFO:
                    log.info(message);
                    break;
                case TRACE:
                    log.trace(message);
                    break;
                case WARN:
                    log.warn(message);
                    break;
                default:
                    break;
            }
        }
        return result;
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
            logByJoinPoint(joinPoint, logAnnotation);
        }
    }
}
