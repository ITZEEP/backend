package org.scoula.global.common.aop;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import lombok.extern.log4j.Log4j2;

/** 로깅 AOP 클래스 컸트롤러, 서비스, 매퍼 메서드의 실행 로그를 자동으로 기록합니다 */
@Aspect
@Component
@Log4j2
public class LoggingAspect {

      @Pointcut("execution(* org.scoula..controller.*.*(..))")
      public void controllerMethods() {}

      @Pointcut("execution(* org.scoula..service.*.*(..))")
      public void serviceMethods() {}

      @Pointcut("execution(* org.scoula..mapper.*.*(..))")
      public void mapperMethods() {}

      @Before("controllerMethods()")
      public void logControllerEntry(JoinPoint joinPoint) {
          String methodName = joinPoint.getSignature().getName();
          String className = joinPoint.getTarget().getClass().getSimpleName();
          Object[] args = joinPoint.getArgs();

          log.info("==> 컸트롤러: {}.{}() 호출, 인수: {}", className, methodName, args);
      }

      @Around("serviceMethods()")
      public Object logServiceExecution(ProceedingJoinPoint joinPoint) throws Throwable {
          String methodName = joinPoint.getSignature().getName();
          String className = joinPoint.getTarget().getClass().getSimpleName();

          long startTime = System.currentTimeMillis();
          log.debug("==> 서비스: {}.{}() 시작", className, methodName);

          try {
              Object result = joinPoint.proceed();
              long endTime = System.currentTimeMillis();
              long executionTime = endTime - startTime;

              if (executionTime > 1000) {
                  log.warn("SLOW SERVICE: {}.{}() 완료 ({}ms)", className, methodName, executionTime);
              } else {
                  log.debug("<== 서비스: {}.{}() 완료 ({}ms)", className, methodName, executionTime);
              }
              return result;
          } catch (Exception e) {
              long endTime = System.currentTimeMillis();
              log.error(
                      "<== 서비스: {}.{}() 실패 ({}ms), 오류: {}",
                      className,
                      methodName,
                      (endTime - startTime),
                      e.getMessage());
              throw e;
          }
      }

      @AfterReturning(pointcut = "controllerMethods()", returning = "result")
      public void logControllerReturn(JoinPoint joinPoint, Object result) {
          String methodName = joinPoint.getSignature().getName();
          String className = joinPoint.getTarget().getClass().getSimpleName();

          log.info(
                  "<== 컸트롤러: {}.{}() 반환: {}",
                  className,
                  methodName,
                  result != null ? result.getClass().getSimpleName() : "null");
      }

      @AfterThrowing(pointcut = "controllerMethods() || serviceMethods()", throwing = "exception")
      public void logException(JoinPoint joinPoint, Exception exception) {
          String methodName = joinPoint.getSignature().getName();
          String className = joinPoint.getTarget().getClass().getSimpleName();

          log.error(
                  "!!! {}.{}()에서 예외 발생: {}",
                  className,
                  methodName,
                  exception.getMessage(),
                  exception);
      }

      @Around("mapperMethods()")
      public Object logMapperExecution(ProceedingJoinPoint joinPoint) throws Throwable {
          String methodName = joinPoint.getSignature().getName();
          String className = joinPoint.getTarget().getClass().getSimpleName();

          log.debug("==> 매퍼: {}.{}() 실행 중", className, methodName);

          try {
              Object result = joinPoint.proceed();
              log.debug("<== 매퍼: {}.{}() 완료", className, methodName);
              return result;
          } catch (Exception e) {
              log.error("<== 매퍼: {}.{}() 실패: {}", className, methodName, e.getMessage());
              throw e;
          }
      }
}
