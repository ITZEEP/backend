package org.scoula.global.common.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.scoula.global.common.dto.ApiResponse;
import org.scoula.global.common.exception.BusinessException;
import org.scoula.global.common.exception.CommonErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@DisplayName("추상 REST 컨트롤러 단위 테스트")
class AbstractRestControllerTest {

      private TestRestController testController;

      // AbstractRestController를 상속받은 테스트용 컨트롤러
      private static class TestRestController extends AbstractRestController {

          public ResponseEntity<ApiResponse<String>> testHandleSuccess(String data, String message) {
              return handleSuccess(data, message);
          }

          public ResponseEntity<ApiResponse<String>> testHandleError(Exception e, String message) {
              return handleError(e, message);
          }

          public ResponseEntity<ApiResponse<String>> testHandleBadRequest(String message) {
              return handleBadRequest(message);
          }

          public ResponseEntity<ApiResponse<String>> testHandleWithStandardExceptions(
                  Supplier<String> operation, String operationName) {
              return handleWithStandardExceptions(operation, operationName);
          }
      }

      @BeforeEach
      void setUp() {
          testController = new TestRestController();
      }

      @Test
      @DisplayName("성공 응답 처리 - 데이터와 메시지가 포함된 응답 반환")
      void handleSuccess_ShouldReturnSuccessResponse() {
          // given
          String data = "test data";
          String message = "성공 메시지";

          // when
          ResponseEntity<ApiResponse<String>> response =
                  testController.testHandleSuccess(data, message);

          // then
          assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
          assertThat(response.getBody()).isNotNull();
          assertThat(response.getBody().isSuccess()).isTrue();
          assertThat(response.getBody().getData()).isEqualTo(data);
          assertThat(response.getBody().getMessage()).isEqualTo(message);
      }

      @Test
      @DisplayName("표준 예외 처리 - 정상 실행 시 성공 응답 반환")
      void handleWithStandardExceptions_ShouldReturnSuccessForNormalOperation() {
          // given
          Supplier<String> operation = () -> "success result";
          String operationName = "test operation";

          // when
          ResponseEntity<ApiResponse<String>> response =
                  testController.testHandleWithStandardExceptions(operation, operationName);

          // then
          assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
          assertThat(response.getBody()).isNotNull();
          assertThat(response.getBody().isSuccess()).isTrue();
          assertThat(response.getBody().getData()).isEqualTo("success result");
      }

      @Test
      @DisplayName("표준 예외 처리 - BusinessException 발생 시 에러 응답 반환")
      void handleWithStandardExceptions_ShouldHandleBusinessException() {
          // given
          Supplier<String> operation =
                  () -> {
                      throw new BusinessException(CommonErrorCode.INVALID_INPUT_VALUE, "비즈니스 에러");
                  };
          String operationName = "test operation";

          // when
          ResponseEntity<ApiResponse<String>> response =
                  testController.testHandleWithStandardExceptions(operation, operationName);

          // then
          assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
          assertThat(response.getBody()).isNotNull();
          assertThat(response.getBody().isSuccess()).isFalse();
          assertThat(response.getBody().getError()).isNotNull();
          assertThat(response.getBody().getError().getCode()).isEqualTo("C001");
      }

      @Test
      @DisplayName("표준 예외 처리 - 일반 Exception 발생 시 500 에러 응답 반환")
      void handleWithStandardExceptions_ShouldHandleGeneralException() {
          // given
          Supplier<String> operation =
                  () -> {
                      throw new RuntimeException("일반 에러");
                  };
          String operationName = "test operation";

          // when
          ResponseEntity<ApiResponse<String>> response =
                  testController.testHandleWithStandardExceptions(operation, operationName);

          // then
          assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
          assertThat(response.getBody()).isNotNull();
          assertThat(response.getBody().isSuccess()).isFalse();
          assertThat(response.getBody().getMessage()).contains("test operation 처리 중 오류가 발생했습니다");
      }

      @Test
      @DisplayName("잘못된 요청 처리 - Bad Request 응답 반환")
      void handleBadRequest_ShouldReturnBadRequestResponse() {
          // given
          String message = "잘못된 요청";

          // when
          ResponseEntity<ApiResponse<String>> response = testController.testHandleBadRequest(message);

          // then
          assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
          assertThat(response.getBody()).isNotNull();
          assertThat(response.getBody().isSuccess()).isFalse();
          assertThat(response.getBody().getError()).isNotNull();
          assertThat(response.getBody().getError().getCode()).isEqualTo("C001");
      }
}
