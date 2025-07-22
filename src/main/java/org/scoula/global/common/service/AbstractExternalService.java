package org.scoula.global.common.service;

import java.time.Duration;
import java.util.function.Supplier;

import org.scoula.global.common.exception.BusinessException;
import org.scoula.global.common.exception.CommonErrorCode;
import org.springframework.retry.RetryContext;
import org.springframework.retry.support.RetryTemplate;

import lombok.extern.log4j.Log4j2;

/** 외부 서비스 연동을 위한 공통 기능을 제공하는 추상 클래스 재시도, 로깅, 예외 처리 등의 공통 패턴을 정의 */
@Log4j2
public abstract class AbstractExternalService extends AbstractValidationService {

      private static final int DEFAULT_MAX_ATTEMPTS = 3;
      private static final Duration DEFAULT_RETRY_DELAY = Duration.ofSeconds(2); // 재시도 간격 증가

      /** 재시도 로직을 포함한 외부 서비스 호출 */
      protected <T> T executeWithRetry(Supplier<T> operation, String operationName) {
          return executeWithRetry(operation, operationName, DEFAULT_MAX_ATTEMPTS);
      }

      /** 재시도 횟수를 지정한 외부 서비스 호출 */
      protected <T> T executeWithRetry(Supplier<T> operation, String operationName, int maxAttempts) {
          RetryTemplate retryTemplate =
                  RetryTemplate.builder()
                          .maxAttempts(maxAttempts)
                          .exponentialBackoff(DEFAULT_RETRY_DELAY.toMillis(), 2, 10000) // 지수 백오프
                          .retryOn(Exception.class)
                          .build();

          try {
              return retryTemplate.execute(
                      context -> {
                          logRetryAttempt(context, operationName);
                          T result = operation.get();
                          logOperationSuccess(operationName, context.getRetryCount());
                          return result;
                      });
          } catch (Exception e) {
              logOperationFailure(operationName, maxAttempts, e);
              throw new BusinessException(
                      CommonErrorCode.EXTERNAL_SERVICE_ERROR, operationName + " 작업이 실패했습니다", e);
          }
      }

      /** 외부 서비스 호출 시 안전한 실행 (재시도 없음) */
      protected <T> T executeSafely(Supplier<T> operation, String operationName) {
          try {
              log.debug("Executing operation: {}", operationName);
              T result = operation.get();
              log.debug("Operation completed successfully: {}", operationName);
              return result;
          } catch (BusinessException e) {
              // BusinessException은 이미 적절히 처리된 예외이므로 그대로 다시 throw
              log.error("Operation failed: {}", operationName, e);
              throw e;
          } catch (Exception e) {
              log.error("Operation failed: {}", operationName, e);
              throw new BusinessException(
                      CommonErrorCode.EXTERNAL_SERVICE_ERROR, operationName + " 작업 중 오류가 발생했습니다", e);
          }
      }

      /** 비동기 작업의 결과를 기다리는 메서드 */
      protected <T> T waitForResult(
              Supplier<T> resultSupplier,
              String operationName,
              Duration timeout,
              Duration checkInterval) {
          long startTime = System.currentTimeMillis();
          long timeoutMillis = timeout.toMillis();
          long intervalMillis = checkInterval.toMillis();

          while (System.currentTimeMillis() - startTime < timeoutMillis) {
              try {
                  T result = resultSupplier.get();
                  if (result != null) {
                      log.debug("Async operation completed: {}", operationName);
                      return result;
                  }
                  Thread.sleep(intervalMillis);
              } catch (InterruptedException e) {
                  Thread.currentThread().interrupt();
                  throw new BusinessException(
                          CommonErrorCode.EXTERNAL_SERVICE_ERROR, operationName + " 작업이 중단되었습니다");
              } catch (Exception e) {
                  log.warn("Error while waiting for {}: {}", operationName, e.getMessage());
              }
          }

          throw new BusinessException(
                  CommonErrorCode.EXTERNAL_SERVICE_ERROR, operationName + " 작업이 시간 초과되었습니다");
      }

      /** 서비스 상태 확인 */
      protected boolean isServiceHealthy(Supplier<Boolean> healthCheck, String serviceName) {
          try {
              boolean healthy = healthCheck.get();
              log.debug(
                      "Service {} health check: {}", serviceName, healthy ? "HEALTHY" : "UNHEALTHY");
              return healthy;
          } catch (Exception e) {
              log.warn("Health check failed for service {}: {}", serviceName, e.getMessage());
              return false;
          }
      }

      /** 재시도 시도 로깅 */
      private void logRetryAttempt(RetryContext context, String operationName) {
          if (context.getRetryCount() > 0) {
              // 첫 번째 재시도만 WARN, 이후는 DEBUG
              if (context.getRetryCount() == 1) {
                  log.warn(
                          "Retrying operation '{}' - attempt {}",
                          operationName,
                          context.getRetryCount());
              } else {
                  log.debug(
                          "Retrying operation '{}' - attempt {}",
                          operationName,
                          context.getRetryCount());
              }
          } else {
              log.debug("Executing operation: {}", operationName);
          }
      }

      /** 성공 로깅 */
      private void logOperationSuccess(String operationName, int retryCount) {
          if (retryCount > 0) {
              log.info("Operation '{}' succeeded after {} retries", operationName, retryCount);
          } else {
              log.debug("Operation '{}' completed successfully", operationName);
          }
      }

      /** 실패 로깅 */
      private void logOperationFailure(String operationName, int maxAttempts, Exception e) {
          log.error(
                  "Operation '{}' failed after {} attempts: {}",
                  operationName,
                  maxAttempts,
                  e.getMessage(),
                  e);
      }

      /** 연산 결과 상세 로깅 */
      protected void logOperationResult(String operation, boolean success, String details) {
          if (success) {
              log.info("Operation '{}' completed successfully. Details: {}", operation, details);
          } else {
              log.warn("Operation '{}' failed. Details: {}", operation, details);
          }
      }
}
