package org.scoula.global.common.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("API 응답 DTO 단위 테스트")
class ApiResponseTest {

      @Test
      @DisplayName("성공 응답 생성 - 데이터 없이")
      void success_ShouldCreateSuccessResponseWithoutData() {
          // when
          ApiResponse<String> response = ApiResponse.success();

          // then
          assertThat(response.isSuccess()).isTrue();
          assertThat(response.getData()).isNull();
          assertThat(response.getMessage()).isNull();
          assertThat(response.getError()).isNull();
          assertThat(response.getTimestamp()).isNotNull();
      }

      @Test
      @DisplayName("성공 응답 생성 - 데이터와 함께")
      void success_ShouldCreateSuccessResponseWithData() {
          // given
          String testData = "test data";

          // when
          ApiResponse<String> response = ApiResponse.success(testData);

          // then
          assertThat(response.isSuccess()).isTrue();
          assertThat(response.getData()).isEqualTo(testData);
          assertThat(response.getMessage()).isNull();
          assertThat(response.getError()).isNull();
          assertThat(response.getTimestamp()).isNotNull();
      }

      @Test
      @DisplayName("성공 응답 생성 - 데이터와 메시지 함께")
      void success_ShouldCreateSuccessResponseWithDataAndMessage() {
          // given
          String testData = "test data";
          String testMessage = "success message";

          // when
          ApiResponse<String> response = ApiResponse.success(testData, testMessage);

          // then
          assertThat(response.isSuccess()).isTrue();
          assertThat(response.getData()).isEqualTo(testData);
          assertThat(response.getMessage()).isEqualTo(testMessage);
          assertThat(response.getError()).isNull();
          assertThat(response.getTimestamp()).isNotNull();
      }

      @Test
      @DisplayName("에러 응답 생성 - 메시지만")
      void error_ShouldCreateErrorResponseWithMessage() {
          // given
          String errorMessage = "error occurred";

          // when
          ApiResponse<String> response = ApiResponse.error(errorMessage);

          // then
          assertThat(response.isSuccess()).isFalse();
          assertThat(response.getData()).isNull();
          assertThat(response.getMessage()).isEqualTo(errorMessage);
          assertThat(response.getError()).isNull();
          assertThat(response.getTimestamp()).isNotNull();
      }

      @Test
      @DisplayName("에러 응답 생성 - 코드와 메시지")
      void error_ShouldCreateErrorResponseWithCodeAndMessage() {
          // given
          String errorCode = "ERR001";
          String errorMessage = "error occurred";

          // when
          ApiResponse<String> response = ApiResponse.error(errorCode, errorMessage);

          // then
          assertThat(response.isSuccess()).isFalse();
          assertThat(response.getData()).isNull();
          assertThat(response.getMessage()).isEqualTo(errorMessage);
          assertThat(response.getError()).isNotNull();
          assertThat(response.getError().getCode()).isEqualTo(errorCode);
          assertThat(response.getError().getReason()).isEqualTo(errorMessage);
          assertThat(response.getTimestamp()).isNotNull();
      }

      @Test
      @DisplayName("ErrorDetails 빌더 테스트")
      void errorDetails_ShouldBuildCorrectly() {
          // given
          String code = "ERR001";
          String field = "username";
          String rejectedValue = "invalid";
          String reason = "Username is invalid";

          // when
          ApiResponse.ErrorDetails errorDetails =
                  ApiResponse.ErrorDetails.builder()
                          .code(code)
                          .field(field)
                          .rejectedValue(rejectedValue)
                          .reason(reason)
                          .build();

          // then
          assertThat(errorDetails.getCode()).isEqualTo(code);
          assertThat(errorDetails.getField()).isEqualTo(field);
          assertThat(errorDetails.getRejectedValue()).isEqualTo(rejectedValue);
          assertThat(errorDetails.getReason()).isEqualTo(reason);
      }
}
