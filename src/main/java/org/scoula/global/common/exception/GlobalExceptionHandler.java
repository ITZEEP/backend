package org.scoula.global.common.exception;

import java.util.List;
import java.util.stream.Collectors;

import org.scoula.global.common.dto.ApiResponse;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;

import lombok.extern.log4j.Log4j2;

/** 전역 예외 처리 핸들러 애플리케이션에서 발생하는 모든 예외를 처리하고 일관된 응답 형식을 제공합니다 */
@RestControllerAdvice
@Log4j2
public class GlobalExceptionHandler {

      @ExceptionHandler(BaseException.class)
      protected ResponseEntity<ApiResponse<Void>> handleBaseException(BaseException e) {
          log.error("기본 예외 발생: {}", e.getMessage(), e);

          ApiResponse.ErrorDetails errorDetails =
                  ApiResponse.ErrorDetails.builder()
                          .code(e.getErrorCode().getCode())
                          .reason(e.getMessage())
                          .build();

          ApiResponse<Void> response = ApiResponse.error(e.getMessage(), errorDetails);
          return ResponseEntity.status(e.getErrorCode().getHttpStatus()).body(response);
      }

      @ExceptionHandler(MethodArgumentNotValidException.class)
      protected ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValidException(
              MethodArgumentNotValidException e) {
          log.error("검증 오류: {}", e.getMessage());

          List<ApiResponse.ErrorDetails> errors =
                  e.getBindingResult().getFieldErrors().stream()
                          .map(this::createFieldErrorDetails)
                          .collect(Collectors.toList());

          ApiResponse.ErrorDetails errorDetails = errors.isEmpty() ? null : errors.get(0);
          ApiResponse<Void> response = ApiResponse.error("검증 실패", errorDetails);

          return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
      }

      @ExceptionHandler(BindException.class)
      protected ResponseEntity<ApiResponse<Void>> handleBindException(BindException e) {
          log.error("바인딩 오류: {}", e.getMessage());

          List<ApiResponse.ErrorDetails> errors =
                  e.getBindingResult().getFieldErrors().stream()
                          .map(this::createFieldErrorDetails)
                          .collect(Collectors.toList());

          ApiResponse.ErrorDetails errorDetails = errors.isEmpty() ? null : errors.get(0);
          ApiResponse<Void> response = ApiResponse.error("바인딩 실패", errorDetails);

          return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
      }

      @ExceptionHandler(MethodArgumentTypeMismatchException.class)
      protected ResponseEntity<ApiResponse<Void>> handleMethodArgumentTypeMismatchException(
              MethodArgumentTypeMismatchException e) {
          log.error("타입 불일치: {}", e.getMessage());

          ApiResponse.ErrorDetails errorDetails =
                  ApiResponse.ErrorDetails.builder()
                          .code(CommonErrorCode.INVALID_TYPE_VALUE.getCode())
                          .field(e.getName())
                          .rejectedValue(e.getValue())
                          .reason("타입 불일치")
                          .build();

          ApiResponse<Void> response = ApiResponse.error("유효하지 않은 타입 값", errorDetails);
          return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
      }

      @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
      protected ResponseEntity<ApiResponse<Void>> handleHttpRequestMethodNotSupportedException(
              HttpRequestMethodNotSupportedException e) {
          log.error("지원하지 않는 메서드: {}", e.getMessage());

          ApiResponse.ErrorDetails errorDetails =
                  ApiResponse.ErrorDetails.builder()
                          .code(CommonErrorCode.METHOD_NOT_ALLOWED.getCode())
                          .reason(e.getMessage())
                          .build();

          ApiResponse<Void> response = ApiResponse.error("허용되지 않은 메서드", errorDetails);
          return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(response);
      }

      @ExceptionHandler(AccessDeniedException.class)
      protected ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(
              AccessDeniedException e) {
          log.error("접근 거부: {}", e.getMessage());

          ApiResponse.ErrorDetails errorDetails =
                  ApiResponse.ErrorDetails.builder()
                          .code(CommonErrorCode.HANDLE_ACCESS_DENIED.getCode())
                          .reason("접근 거부")
                          .build();

          ApiResponse<Void> response = ApiResponse.error("접근 거부", errorDetails);
          return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
      }

      @ExceptionHandler(MaxUploadSizeExceededException.class)
      protected ResponseEntity<ApiResponse<Void>> handleMaxUploadSizeExceededException(
              MaxUploadSizeExceededException e) {
          log.error("파일 크기 초과: {}", e.getMessage());

          ApiResponse.ErrorDetails errorDetails =
                  ApiResponse.ErrorDetails.builder()
                          .code(CommonErrorCode.INVALID_INPUT_VALUE.getCode())
                          .reason("파일 크기 초과")
                          .build();

          ApiResponse<Void> response = ApiResponse.error("파일 크기 초과", errorDetails);
          return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
      }

      @ExceptionHandler(HttpMessageNotReadableException.class)
      protected ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadableException(
              HttpMessageNotReadableException e) {
          log.error("메시지 읽기 불가: {}", e.getMessage());

          ApiResponse.ErrorDetails errorDetails =
                  ApiResponse.ErrorDetails.builder()
                          .code(CommonErrorCode.INVALID_JSON_FORMAT.getCode())
                          .reason("유효하지 않은 JSON 형식")
                          .build();

          ApiResponse<Void> response = ApiResponse.error("유효하지 않은 입력 형식", errorDetails);
          return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
      }

      @ExceptionHandler(DataAccessException.class)
      protected ResponseEntity<ApiResponse<Void>> handleDataAccessException(DataAccessException e) {
          log.error("데이터 접근 오류: {}", e.getMessage(), e);

          CommonErrorCode errorCode = CommonErrorCode.DATA_ACCESS_ERROR;
          if (e instanceof DataIntegrityViolationException) {
              errorCode = CommonErrorCode.DATA_INTEGRITY_VIOLATION;
          } else if (e instanceof DuplicateKeyException) {
              errorCode = CommonErrorCode.DUPLICATE_KEY_ERROR;
          }

          ApiResponse.ErrorDetails errorDetails =
                  ApiResponse.ErrorDetails.builder()
                          .code(errorCode.getCode())
                          .reason("데이터베이스 작업 실패")
                          .build();

          ApiResponse<Void> response = ApiResponse.error("데이터베이스 오류", errorDetails);
          return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
      }

      @ExceptionHandler(NoHandlerFoundException.class)
      protected ResponseEntity<ApiResponse<Void>> handleNoHandlerFoundException(
              NoHandlerFoundException e) {
          String requestUri = e.getRequestURL();

          // Swagger 리소스 요청인 경우 로깅 레벨을 DEBUG로 낮춤
          if (requestUri != null
                  && (requestUri.contains("swagger-resources")
                          || requestUri.contains("swagger-ui")
                          || requestUri.contains("v2/api-docs"))) {
              log.debug("Swagger 리소스 요청 실패 (무시됨): {}", requestUri);
              return ResponseEntity.notFound().build();
          }

          log.warn("핸들러를 찾을 수 없음: {}", e.getMessage());

          ApiResponse.ErrorDetails errorDetails =
                  ApiResponse.ErrorDetails.builder()
                          .code(CommonErrorCode.ENTITY_NOT_FOUND.getCode())
                          .reason("요청한 리소스를 찾을 수 없습니다")
                          .build();

          ApiResponse<Void> response = ApiResponse.error("리소스를 찾을 수 없습니다", errorDetails);
          return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
      }

      @ExceptionHandler(Exception.class)
      protected ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
          // NoHandlerFoundException은 별도로 처리하므로 제외
          if (e instanceof NoHandlerFoundException) {
              return handleNoHandlerFoundException((NoHandlerFoundException) e);
          }

          log.error("예상치 못한 오류: {}", e.getMessage(), e);

          ApiResponse.ErrorDetails errorDetails =
                  ApiResponse.ErrorDetails.builder()
                          .code(CommonErrorCode.INTERNAL_SERVER_ERROR.getCode())
                          .reason("내부 서버 오류")
                          .build();

          ApiResponse<Void> response = ApiResponse.error("내부 서버 오류", errorDetails);
          return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
      }

      private ApiResponse.ErrorDetails createFieldErrorDetails(FieldError fieldError) {
          return ApiResponse.ErrorDetails.builder()
                  .code(CommonErrorCode.INVALID_INPUT_VALUE.getCode())
                  .field(fieldError.getField())
                  .rejectedValue(fieldError.getRejectedValue())
                  .reason(fieldError.getDefaultMessage())
                  .build();
      }
}
