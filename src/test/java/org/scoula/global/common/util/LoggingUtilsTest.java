package org.scoula.global.common.util;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.function.Supplier;

import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("LoggingUtils 테스트")
class LoggingUtilsTest {

      @Mock private Logger mockLogger;

      @Nested
      @DisplayName("logOperation 메서드 테스트")
      class LogOperationTest {

          @Test
          @DisplayName("정상 작업 실행 - 성공 로깅")
          void logOperation_Success() {
              // Given
              String operationName = "testOperation";
              Runnable action = mock(Runnable.class);

              // When
              LoggingUtils.logOperation(mockLogger, operationName, action);

              // Then
              verify(action).run();
              verify(mockLogger).debug("작업 시작: {}", operationName);
              verify(mockLogger).debug(eq("작업 완료: {} (소요시간: {}ms)"), eq(operationName), anyLong());
          }

          @Test
          @DisplayName("예외 발생시 에러 로깅 및 예외 재발생")
          void logOperation_ExceptionThrown() {
              // Given
              String operationName = "failingOperation";
              RuntimeException expectedException = new RuntimeException("Test exception");
              Runnable action =
                      () -> {
                          throw expectedException;
                      };

              // When & Then
              RuntimeException thrownException =
                      assertThrows(
                              RuntimeException.class,
                              () -> LoggingUtils.logOperation(mockLogger, operationName, action));

              assertSame(expectedException, thrownException);
              verify(mockLogger).debug("작업 시작: {}", operationName);
              verify(mockLogger)
                      .error(
                              eq("작업 실패: {} (소요시간: {}ms) - {}"),
                              eq(operationName),
                              anyLong(),
                              eq("Test exception"),
                              eq(expectedException));
          }

          @Test
          @DisplayName("null 작업명으로 실행")
          void logOperation_NullOperationName() {
              // Given
              String operationName = null;
              Runnable action = mock(Runnable.class);

              // When
              LoggingUtils.logOperation(mockLogger, operationName, action);

              // Then
              verify(action).run();
              verify(mockLogger).debug("작업 시작: {}", operationName);
          }
      }

      @Nested
      @DisplayName("logOperationWithResult 메서드 테스트")
      class LogOperationWithResultTest {

          @Test
          @DisplayName("정상 작업 실행 - 결과 반환 및 성공 로깅")
          void logOperationWithResult_Success() {
              // Given
              String operationName = "testOperation";
              String expectedResult = "testResult";
              Supplier<String> action = () -> expectedResult;

              // When
              String result = LoggingUtils.logOperationWithResult(mockLogger, operationName, action);

              // Then
              assertEquals(expectedResult, result);
              verify(mockLogger).debug("작업 시작: {}", operationName);
              verify(mockLogger).debug(eq("작업 완료: {} (소요시간: {}ms)"), eq(operationName), anyLong());
          }

          @Test
          @DisplayName("예외 발생시 에러 로깅 및 예외 재발생")
          void logOperationWithResult_ExceptionThrown() {
              // Given
              String operationName = "failingOperation";
              IllegalArgumentException expectedException =
                      new IllegalArgumentException("Invalid argument");
              Supplier<String> action =
                      () -> {
                          throw expectedException;
                      };

              // When & Then
              IllegalArgumentException thrownException =
                      assertThrows(
                              IllegalArgumentException.class,
                              () ->
                                      LoggingUtils.logOperationWithResult(
                                              mockLogger, operationName, action));

              assertSame(expectedException, thrownException);
              verify(mockLogger).debug("작업 시작: {}", operationName);
              verify(mockLogger)
                      .error(
                              eq("작업 실패: {} (소요시간: {}ms) - {}"),
                              eq(operationName),
                              anyLong(),
                              eq("Invalid argument"),
                              eq(expectedException));
          }

          @Test
          @DisplayName("null 반환값 처리")
          void logOperationWithResult_NullResult() {
              // Given
              String operationName = "nullResultOperation";
              Supplier<String> action = () -> null;

              // When
              String result = LoggingUtils.logOperationWithResult(mockLogger, operationName, action);

              // Then
              assertNull(result);
              verify(mockLogger).debug("작업 시작: {}", operationName);
              verify(mockLogger).debug(eq("작업 완료: {} (소요시간: {}ms)"), eq(operationName), anyLong());
          }
      }

      @Nested
      @DisplayName("logApiRequest 메서드 테스트")
      class LogApiRequestTest {

          @Test
          @DisplayName("사용자 ID가 있는 API 요청 로깅")
          void logApiRequest_WithUserId() {
              // Given
              String method = "GET";
              String endpoint = "/api/users";
              Long userId = 123L;

              // When
              LoggingUtils.logApiRequest(mockLogger, method, endpoint, userId);

              // Then
              verify(mockLogger).info("API 요청: {} {} (사용자: {})", method, endpoint, userId);
          }

          @Test
          @DisplayName("사용자 ID가 없는 API 요청 로깅")
          void logApiRequest_WithoutUserId() {
              // Given
              String method = "POST";
              String endpoint = "/api/login";
              Long userId = null;

              // When
              LoggingUtils.logApiRequest(mockLogger, method, endpoint, userId);

              // Then
              verify(mockLogger).info("API 요청: {} {}", method, endpoint);
          }

          @ParameterizedTest
          @CsvSource({
              "GET, /api/users",
              "POST, /api/auth/login",
              "PUT, /api/users/123",
              "DELETE, /api/users/456"
          })
          @DisplayName("다양한 HTTP 메서드와 엔드포인트 로깅")
          void logApiRequest_VariousMethods(String method, String endpoint) {
              // When
              LoggingUtils.logApiRequest(mockLogger, method, endpoint, null);

              // Then
              verify(mockLogger).info("API 요청: {} {}", method, endpoint);
          }
      }

      @Nested
      @DisplayName("logApiResponse 메서드 테스트")
      class LogApiResponseTest {

          @ParameterizedTest
          @ValueSource(ints = {200, 201, 204, 299})
          @DisplayName("성공 응답 로깅 (2xx)")
          void logApiResponse_Success(int statusCode) {
              // Given
              String method = "GET";
              String endpoint = "/api/users";
              long elapsedTime = 150L;

              // When
              LoggingUtils.logApiResponse(mockLogger, method, endpoint, statusCode, elapsedTime);

              // Then
              verify(mockLogger)
                      .info("API 응답: {} {} - {} ({}ms)", method, endpoint, statusCode, elapsedTime);
          }

          @ParameterizedTest
          @ValueSource(ints = {400, 401, 403, 404, 422, 499})
          @DisplayName("클라이언트 오류 응답 로깅 (4xx)")
          void logApiResponse_ClientError(int statusCode) {
              // Given
              String method = "POST";
              String endpoint = "/api/login";
              long elapsedTime = 75L;

              // When
              LoggingUtils.logApiResponse(mockLogger, method, endpoint, statusCode, elapsedTime);

              // Then
              verify(mockLogger)
                      .warn(
                              "API 클라이언트 오류: {} {} - {} ({}ms)",
                              method,
                              endpoint,
                              statusCode,
                              elapsedTime);
          }

          @ParameterizedTest
          @ValueSource(ints = {500, 502, 503, 504, 599})
          @DisplayName("서버 오류 응답 로깅 (5xx)")
          void logApiResponse_ServerError(int statusCode) {
              // Given
              String method = "PUT";
              String endpoint = "/api/users/123";
              long elapsedTime = 5000L;

              // When
              LoggingUtils.logApiResponse(mockLogger, method, endpoint, statusCode, elapsedTime);

              // Then
              verify(mockLogger)
                      .error(
                              "API 서버 오류: {} {} - {} ({}ms)",
                              method,
                              endpoint,
                              statusCode,
                              elapsedTime);
          }

          @Test
          @DisplayName("기타 상태 코드 (3xx) 처리")
          void logApiResponse_RedirectionStatus() {
              // Given
              String method = "GET";
              String endpoint = "/api/redirect";
              int statusCode = 301;
              long elapsedTime = 50L;

              // When
              LoggingUtils.logApiResponse(mockLogger, method, endpoint, statusCode, elapsedTime);

              // Then
              verifyNoInteractions(mockLogger); // 2xx, 4xx, 5xx가 아닌 경우 로깅하지 않음
          }
      }

      @Nested
      @DisplayName("logPerformanceWarning 메서드 테스트")
      class LogPerformanceWarningTest {

          @Test
          @DisplayName("임계값 초과시 성능 경고 로깅")
          void logPerformanceWarning_ExceedsThreshold() {
              // Given
              String operationName = "slowOperation";
              long elapsedTime = 5000L;
              long threshold = 3000L;

              // When
              LoggingUtils.logPerformanceWarning(mockLogger, operationName, elapsedTime, threshold);

              // Then
              verify(mockLogger)
                      .warn(
                              "성능 경고: {} 작업이 {}ms 소요됨 (임계값: {}ms)",
                              operationName,
                              elapsedTime,
                              threshold);
          }

          @Test
          @DisplayName("임계값 이하일 때 로깅하지 않음")
          void logPerformanceWarning_WithinThreshold() {
              // Given
              String operationName = "fastOperation";
              long elapsedTime = 100L;
              long threshold = 1000L;

              // When
              LoggingUtils.logPerformanceWarning(mockLogger, operationName, elapsedTime, threshold);

              // Then
              verifyNoInteractions(mockLogger);
          }

          @Test
          @DisplayName("임계값과 정확히 같을 때 로깅하지 않음")
          void logPerformanceWarning_ExactThreshold() {
              // Given
              String operationName = "exactOperation";
              long elapsedTime = 1000L;
              long threshold = 1000L;

              // When
              LoggingUtils.logPerformanceWarning(mockLogger, operationName, elapsedTime, threshold);

              // Then
              verifyNoInteractions(mockLogger);
          }
      }

      @Nested
      @DisplayName("logSecurityEvent 메서드 테스트")
      class LogSecurityEventTest {

          @Test
          @DisplayName("사용자 ID가 있는 보안 이벤트 로깅")
          void logSecurityEvent_WithUserId() {
              // Given
              String eventType = "LOGIN_ATTEMPT";
              Long userId = 456L;
              String details = "Successful login from IP 192.168.1.100";

              // When
              LoggingUtils.logSecurityEvent(mockLogger, eventType, userId, details);

              // Then
              verify(mockLogger).info("보안 이벤트: {} - 사용자: {} - {}", eventType, userId, details);
          }

          @Test
          @DisplayName("사용자 ID가 없는 보안 이벤트 로깅")
          void logSecurityEvent_WithoutUserId() {
              // Given
              String eventType = "FAILED_LOGIN";
              Long userId = null;
              String details = "Failed login attempt from IP 192.168.1.200";

              // When
              LoggingUtils.logSecurityEvent(mockLogger, eventType, userId, details);

              // Then
              verify(mockLogger).info("보안 이벤트: {} - 사용자: {} - {}", eventType, "anonymous", details);
          }

          @ParameterizedTest
          @CsvSource({
              "PASSWORD_CHANGE, Password changed successfully",
              "ACCOUNT_LOCKED, Account locked due to multiple failed attempts",
              "SUSPICIOUS_ACTIVITY, Multiple requests from same IP"
          })
          @DisplayName("다양한 보안 이벤트 타입 로깅")
          void logSecurityEvent_VariousEventTypes(String eventType, String details) {
              // Given
              Long userId = 789L;

              // When
              LoggingUtils.logSecurityEvent(mockLogger, eventType, userId, details);

              // Then
              verify(mockLogger).info("보안 이벤트: {} - 사용자: {} - {}", eventType, userId, details);
          }
      }

      @Nested
      @DisplayName("logExternalServiceCall 메서드 테스트")
      class LogExternalServiceCallTest {

          @Test
          @DisplayName("외부 서비스 호출 성공 로깅")
          void logExternalServiceCall_Success() {
              // Given
              String serviceName = "PaymentService";
              String operation = "processPayment";
              boolean success = true;

              // When
              LoggingUtils.logExternalServiceCall(mockLogger, serviceName, operation, success);

              // Then
              verify(mockLogger).info("외부 서비스 호출 성공: {} - {}", serviceName, operation);
          }

          @Test
          @DisplayName("외부 서비스 호출 실패 로깅")
          void logExternalServiceCall_Failure() {
              // Given
              String serviceName = "EmailService";
              String operation = "sendNotification";
              boolean success = false;

              // When
              LoggingUtils.logExternalServiceCall(mockLogger, serviceName, operation, success);

              // Then
              verify(mockLogger).error("외부 서비스 호출 실패: {} - {}", serviceName, operation);
          }

          @ParameterizedTest
          @CsvSource({
              "S3Service, uploadFile",
              "RedisService, cacheData",
              "DatabaseService, saveRecord"
          })
          @DisplayName("다양한 외부 서비스 호출 로깅")
          void logExternalServiceCall_VariousServices(String serviceName, String operation) {
              // When
              LoggingUtils.logExternalServiceCall(mockLogger, serviceName, operation, true);

              // Then
              verify(mockLogger).info("외부 서비스 호출 성공: {} - {}", serviceName, operation);
          }
      }

      @Nested
      @DisplayName("logAudit 메서드 테스트")
      class LogAuditTest {

          @Test
          @DisplayName("감사 로그 기록")
          void logAudit_StandardCase() {
              // Given
              String entityType = "User";
              Long entityId = 123L;
              String action = "CREATE";
              Long userId = 456L;

              // When
              LoggingUtils.logAudit(mockLogger, entityType, entityId, action, userId);

              // Then
              verify(mockLogger)
                      .info("감사 로그: {} {} - ID: {} - 사용자: {}", action, entityType, entityId, userId);
          }

          @ParameterizedTest
          @CsvSource({"User, 1, CREATE, 100", "Post, 2, UPDATE, 200", "Comment, 3, DELETE, 300"})
          @DisplayName("다양한 엔티티와 액션의 감사 로그")
          void logAudit_VariousEntitiesAndActions(
                  String entityType, Long entityId, String action, Long userId) {
              // When
              LoggingUtils.logAudit(mockLogger, entityType, entityId, action, userId);

              // Then
              verify(mockLogger)
                      .info("감사 로그: {} {} - ID: {} - 사용자: {}", action, entityType, entityId, userId);
          }

          @Test
          @DisplayName("null 엔티티 ID로 감사 로그")
          void logAudit_NullEntityId() {
              // Given
              String entityType = "System";
              Long entityId = null;
              String action = "STARTUP";
              Long userId = null;

              // When
              LoggingUtils.logAudit(mockLogger, entityType, entityId, action, userId);

              // Then
              verify(mockLogger)
                      .info("감사 로그: {} {} - ID: {} - 사용자: {}", action, entityType, entityId, userId);
          }
      }

      @Nested
      @DisplayName("생성자 및 특수 케이스 테스트")
      class ConstructorAndSpecialCasesTest {

          @Test
          @DisplayName("유틸리티 클래스 생성자 호출")
          void constructor_ShouldNotThrowException() {
              // When & Then
              assertDoesNotThrow(
                      () -> {
                          // 리플렉션을 통해 private 생성자 호출
                          java.lang.reflect.Constructor<LoggingUtils> constructor =
                                  LoggingUtils.class.getDeclaredConstructor();
                          constructor.setAccessible(true);
                          constructor.newInstance();
                      });
          }

          @Test
          @DisplayName("긴 작업명으로 로깅")
          void logOperation_LongOperationName() {
              // Given
              String longOperationName = "very".repeat(100) + "LongOperationName";
              Runnable action = mock(Runnable.class);

              // When
              LoggingUtils.logOperation(mockLogger, longOperationName, action);

              // Then
              verify(action).run();
              verify(mockLogger).debug("작업 시작: {}", longOperationName);
          }

          @Test
          @DisplayName("소요 시간이 측정되는지 확인")
          void logOperation_TimeMeasurement() throws InterruptedException {
              // Given
              String operationName = "timedOperation";
              Runnable action =
                      () -> {
                          try {
                              Thread.sleep(10); // 10ms 대기
                          } catch (InterruptedException e) {
                              Thread.currentThread().interrupt();
                          }
                      };

              // When
              LoggingUtils.logOperation(mockLogger, operationName, action);

              // Then
              ArgumentCaptor<Long> timeCaptor = ArgumentCaptor.forClass(Long.class);
              verify(mockLogger)
                      .debug(eq("작업 완료: {} (소요시간: {}ms)"), eq(operationName), timeCaptor.capture());

              Long capturedTime = timeCaptor.getValue();
              assertTrue(capturedTime >= 0, "소요 시간은 0 이상이어야 함");
          }
      }
}
