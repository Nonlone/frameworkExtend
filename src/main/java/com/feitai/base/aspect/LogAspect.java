package com.feitai.base.aspect;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.feitai.base.annotion.Log;
import com.feitai.base.annotion.LogLocation;

import lombok.extern.slf4j.Slf4j;

/**
 * @author linguocheng
 * @date 2019年1月18日
 */
@Component
@Aspect
@Slf4j
public class LogAspect {

	/**
	 * 前置通知输出日志
	 * 
	 * @param joinPoint
	 * @param logAnnotation
	 */
	@Before("@annotation(logAnnotation)")
	public void doBefore(JoinPoint joinPoint, Log logAnnotation) {
		try {
			if (isLog(logAnnotation, LogLocation.BEFORE)) {
				logByJoinPoint(joinPoint, logAnnotation);
			}
		} catch (Exception e) {
			log.error("LogAspect doBefore has error", e);
		}
	}

	private boolean isLog(Log logAnnotation, LogLocation logLocation) {
		for (LogLocation location : logAnnotation.logLocation()) {
			if (location.equals(logLocation)) {
				return true;
			}
		}
		return false;
	}

	private void logByJoinPoint(JoinPoint joinPoint, Log logAnnotation) {
		switch (logAnnotation.level()) {
		case ERROR:
			log.error(getMessage(joinPoint));
			break;
		case DEBUG:
			log.debug(getMessage(joinPoint));
			break;
		case INFO:
			log.info(getMessage(joinPoint));
			break;
		case TRACE:
			log.trace(getMessage(joinPoint));
			break;
		case WARN:
			log.warn(getMessage(joinPoint));
			break;
		default:
			break;
		}
	}

	private String getMessage(JoinPoint joinPoint) {
		joinPoint.getSignature().getName();
		StringBuffer sb = new StringBuffer();
		sb.append(joinPoint.getSignature().getDeclaringTypeName()).append("  ")
				.append(joinPoint.getSignature().getName()).append(" request params<");
		for (Object obj : joinPoint.getArgs()) {
			try {
				sb.append(JSONObject.toJSONString(obj)).append(",");
			} catch (JSONException je) {
				sb.append(obj.toString()).append(",");
			}

		}
		int splitLastIndex = sb.lastIndexOf(",");
		sb.replace(splitLastIndex, splitLastIndex + 1, ">");
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
		try {
			if (isLog(logAnnotation, LogLocation.THROWING)) {
				log.error(getMessage(joinPoint), e);
			}
		} catch (Exception exception) {
			log.error("LogAspect doAfterThrowing has error", exception);
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

		Object result = joinPoint.proceed();
		if (isLog(logAnnotation, LogLocation.AROUND)) {
			try {
				String message = getMessage(joinPoint);
				try {
		            if(result!=null){
					  message = message + ",result<" + JSONObject.toJSONString(result) + ">";
		            }
				} catch (JSONException jsone) {
					message = message + ",result<" + result.toString() + ">";
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
			} catch (Exception e) {
				log.error("LogAspect doAroud has error", e);
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
		try {
			if(isLog(logAnnotation, LogLocation.AFTER))
			logByJoinPoint(joinPoint, logAnnotation);
		} catch (Exception e) {
			log.error("LogAspect doAfter has error", e);
		}
	}
}
