package org.scoula.global.common.util;

import org.apache.logging.log4j.Logger;

/**
 * 공통 로깅 유틸리티 클래스
 *
 * <p>일관된 로깅 패턴을 제공하여 전체 애플리케이션에서 표준화된 로깅을 수행합니다.
 *
 * @author ITZeep Team
 * @since 1.0.0
 */
public class LoggingUtils {

      private LoggingUtils() {
          // 유틸리티 클래스는 인스턴스화 방지
      }

      /**
       * 작업 실행과 로깅을 함께 수행
       *
       * @param log 로거 인스턴스
       * @param operationName 작업 이름
       * @param action 실행할 작업
       */
      public static void logOperation(Logger log, String operationName, Runnable action) {
          log.debug("작업 시작: {}", operationName);
          long startTime = System.currentTimeMillis();

          try {
              action.run();
              long elapsedTime = System.currentTimeMillis() - startTime;
              log.debug("작업 완료: {} (소요시간: {}ms)", operationName, elapsedTime);
          } catch (Exception e) {
              long elapsedTime = System.currentTimeMillis() - startTime;
              log.error("작업 실패: {} (소요시간: {}ms) - {}", operationName, elapsedTime, e.getMessage(), e);
              throw e;
          }
      }

      /**
       * 작업 실행과 로깅을 함께 수행 (반환값 있음)
       *
       * @param log 로거 인스턴스
       * @param operationName 작업 이름
       * @param action 실행할 작업
       * @param <T> 반환 타입
       * @return 작업 실행 결과
       */
      public static <T> T logOperationWithResult(
              Logger log, String operationName, java.util.function.Supplier<T> action) {
          log.debug("작업 시작: {}", operationName);
          long startTime = System.currentTimeMillis();

          try {
              T result = action.get();
              long elapsedTime = System.currentTimeMillis() - startTime;
              log.debug("작업 완료: {} (소요시간: {}ms)", operationName, elapsedTime);
              return result;
          } catch (Exception e) {
              long elapsedTime = System.currentTimeMillis() - startTime;
              log.error("작업 실패: {} (소요시간: {}ms) - {}", operationName, elapsedTime, e.getMessage(), e);
              throw e;
          }
      }

      /**
       * API 요청 로깅
       *
       * @param log 로거 인스턴스
       * @param method HTTP 메서드
       * @param endpoint 엔드포인트
       * @param userId 사용자 ID (optional)
       */
      public static void logApiRequest(Logger log, String method, String endpoint, Long userId) {
          if (userId != null) {
              log.info("API 요청: {} {} (사용자: {})", method, endpoint, userId);
          } else {
              log.info("API 요청: {} {}", method, endpoint);
          }
      }

      /**
       * API 응답 로깅
       *
       * @param log 로거 인스턴스
       * @param method HTTP 메서드
       * @param endpoint 엔드포인트
       * @param statusCode 상태 코드
       * @param elapsedTime 소요 시간 (ms)
       */
      public static void logApiResponse(
              Logger log, String method, String endpoint, int statusCode, long elapsedTime) {
          if (statusCode >= 200 && statusCode < 300) {
              log.info("API 응답: {} {} - {} ({}ms)", method, endpoint, statusCode, elapsedTime);
          } else if (statusCode >= 400 && statusCode < 500) {
              log.warn("API 클라이언트 오류: {} {} - {} ({}ms)", method, endpoint, statusCode, elapsedTime);
          } else if (statusCode >= 500) {
              log.error("API 서버 오류: {} {} - {} ({}ms)", method, endpoint, statusCode, elapsedTime);
          }
      }

      /**
       * 성능 경고 로깅
       *
       * @param log 로거 인스턴스
       * @param operationName 작업 이름
       * @param elapsedTime 소요 시간
       * @param threshold 임계값
       */
      public static void logPerformanceWarning(
              Logger log, String operationName, long elapsedTime, long threshold) {
          if (elapsedTime > threshold) {
              log.warn("성능 경고: {} 작업이 {}ms 소요됨 (임계값: {}ms)", operationName, elapsedTime, threshold);
          }
      }

      /**
       * 보안 이벤트 로깅
       *
       * @param log 로거 인스턴스
       * @param eventType 이벤트 타입
       * @param userId 사용자 ID
       * @param details 상세 정보
       */
      public static void logSecurityEvent(Logger log, String eventType, Long userId, String details) {
          log.info(
                  "보안 이벤트: {} - 사용자: {} - {}",
                  eventType,
                  userId != null ? userId : "anonymous",
                  details);
      }

      /**
       * 외부 서비스 호출 로깅
       *
       * @param log 로거 인스턴스
       * @param serviceName 서비스 이름
       * @param operation 작업 내용
       * @param success 성공 여부
       */
      public static void logExternalServiceCall(
              Logger log, String serviceName, String operation, boolean success) {
          if (success) {
              log.info("외부 서비스 호출 성공: {} - {}", serviceName, operation);
          } else {
              log.error("외부 서비스 호출 실패: {} - {}", serviceName, operation);
          }
      }

      /**
       * 데이터 변경 감사 로깅
       *
       * @param log 로거 인스턴스
       * @param entityType 엔티티 타입
       * @param entityId 엔티티 ID
       * @param action 수행 작업
       * @param userId 수행 사용자
       */
      public static void logAudit(
              Logger log, String entityType, Long entityId, String action, Long userId) {
          log.info("감사 로그: {} {} - ID: {} - 사용자: {}", action, entityType, entityId, userId);
      }
}
