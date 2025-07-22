package org.scoula.global.common.controller;

import java.util.function.Supplier;

import org.scoula.global.common.dto.ApiResponse;
import org.scoula.global.common.exception.BusinessException;
import org.scoula.global.common.exception.CommonErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;

import lombok.extern.log4j.Log4j2;

/** REST API 컨트롤러의 공통 기능을 제공하는 추상 클래스 표준화된 응답 처리, 예외 처리, 검증 로직을 제공 */
@Log4j2
public abstract class AbstractRestController {

      /** 성공 응답을 생성합니다. */
      protected <T> ResponseEntity<ApiResponse<T>> handleSuccess(T data, String message) {
          log.debug("Success response: {}", message);
          return ResponseEntity.ok(ApiResponse.success(data, message));
      }

      /** 성공 응답을 생성합니다. (기본 메시지) */
      protected <T> ResponseEntity<ApiResponse<T>> handleSuccess(T data) {
          return handleSuccess(data, "요청이 성공적으로 처리되었습니다");
      }

      /** 검증과 함께 비즈니스 로직을 실행합니다. */
      protected <T> ResponseEntity<ApiResponse<T>> handleWithValidation(
              Supplier<T> operation, String successMessage) {
          try {
              T result = operation.get();
              return handleSuccess(result, successMessage);
          } catch (Exception e) {
              return handleError(e);
          }
      }

      /** 표준 예외 처리와 함께 비즈니스 로직을 실행합니다. */
      protected <T> ResponseEntity<ApiResponse<T>> handleWithStandardExceptions(
              Supplier<T> operation, String operationName) {
          try {
              log.debug("Executing operation: {}", operationName);
              T result = operation.get();
              log.debug("Operation completed successfully: {}", operationName);
              return handleSuccess(result);
          } catch (BusinessException e) {
              log.warn("Business exception in {}: {}", operationName, e.getMessage());
              return ResponseEntity.status(e.getErrorCode().getHttpStatus())
                      .body(ApiResponse.error(e.getErrorCode().getCode(), e.getMessage()));
          } catch (Exception e) {
              log.error("Unexpected error in {}: {}", operationName, e.getMessage(), e);
              return handleError(e, operationName + " 처리 중 오류가 발생했습니다");
          }
      }

      /** 검증 결과를 확인하고 에러가 있으면 예외를 발생시킵니다. */
      protected void validateBinding(BindingResult bindingResult) {
          if (bindingResult.hasErrors()) {
              FieldError fieldError = bindingResult.getFieldError();
              String errorMessage =
                      fieldError != null ? fieldError.getDefaultMessage() : "입력값이 올바르지 않습니다";
              log.warn("Validation failed: {}", errorMessage);
              throw new BusinessException(CommonErrorCode.INVALID_INPUT_VALUE, errorMessage);
          }
      }

      /** 에러 응답을 생성합니다. */
      protected <T> ResponseEntity<ApiResponse<T>> handleError(Exception e, String message) {
          if (e instanceof BusinessException) {
              BusinessException businessException = (BusinessException) e;
              log.warn(
                      "Business exception: {} - {}",
                      businessException.getErrorCode().getCode(),
                      businessException.getMessage());
              return ResponseEntity.status(businessException.getErrorCode().getHttpStatus())
                      .body(
                              ApiResponse.error(
                                      businessException.getErrorCode().getCode(),
                                      businessException.getMessage()));
          }

          log.error("Unexpected error: {} - {}", message, e.getMessage(), e);
          return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                  .body(ApiResponse.error("INTERNAL_ERROR", message));
      }

      /** 에러 응답을 생성합니다. (기본 메시지) */
      protected <T> ResponseEntity<ApiResponse<T>> handleError(Exception e) {
          return handleError(e, "요청 처리 중 오류가 발생했습니다");
      }

      /** Bad Request 응답을 생성합니다. */
      protected <T> ResponseEntity<ApiResponse<T>> handleBadRequest(String message) {
          log.warn("Bad request: {}", message);
          return ResponseEntity.badRequest()
                  .body(ApiResponse.error(CommonErrorCode.INVALID_INPUT_VALUE.getCode(), message));
      }
}
