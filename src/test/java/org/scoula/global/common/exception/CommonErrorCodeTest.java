package org.scoula.global.common.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

@DisplayName("공통 에러 코드 단위 테스트")
class CommonErrorCodeTest {

      @Test
      @DisplayName("INVALID_INPUT_VALUE - 올바른 에러 정보 반환")
      void invalidInputValue_ShouldHaveCorrectErrorInfo() {
          // when
          CommonErrorCode errorCode = CommonErrorCode.INVALID_INPUT_VALUE;

          // then
          assertThat(errorCode.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
          assertThat(errorCode.getCode()).isEqualTo("C001");
          assertThat(errorCode.getMessage()).isEqualTo("잘못된 입력값입니다");
      }

      @Test
      @DisplayName("METHOD_NOT_ALLOWED - 올바른 에러 정보 반환")
      void methodNotAllowed_ShouldHaveCorrectErrorInfo() {
          // when
          CommonErrorCode errorCode = CommonErrorCode.METHOD_NOT_ALLOWED;

          // then
          assertThat(errorCode.getHttpStatus()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
          assertThat(errorCode.getCode()).isEqualTo("C002");
          assertThat(errorCode.getMessage()).isEqualTo("허용되지 않은 HTTP 메서드입니다");
      }

      @Test
      @DisplayName("ENTITY_NOT_FOUND - 올바른 에러 정보 반환")
      void entityNotFound_ShouldHaveCorrectErrorInfo() {
          // when
          CommonErrorCode errorCode = CommonErrorCode.ENTITY_NOT_FOUND;

          // then
          assertThat(errorCode.getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
          assertThat(errorCode.getCode()).isEqualTo("C003");
          assertThat(errorCode.getMessage()).isEqualTo("요청한 리소스를 찾을 수 없습니다");
      }

      @Test
      @DisplayName("INTERNAL_SERVER_ERROR - 올바른 에러 정보 반환")
      void internalServerError_ShouldHaveCorrectErrorInfo() {
          // when
          CommonErrorCode errorCode = CommonErrorCode.INTERNAL_SERVER_ERROR;

          // then
          assertThat(errorCode.getHttpStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
          assertThat(errorCode.getCode()).isEqualTo("C004");
          assertThat(errorCode.getMessage()).isEqualTo("내부 서버 오류가 발생했습니다");
      }

      @Test
      @DisplayName("INVALID_TYPE_VALUE - 올바른 에러 정보 반환")
      void invalidTypeValue_ShouldHaveCorrectErrorInfo() {
          // when
          CommonErrorCode errorCode = CommonErrorCode.INVALID_TYPE_VALUE;

          // then
          assertThat(errorCode.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
          assertThat(errorCode.getCode()).isEqualTo("C005");
          assertThat(errorCode.getMessage()).isEqualTo("타입이 올바르지 않습니다");
      }

      @Test
      @DisplayName("HANDLE_ACCESS_DENIED - 올바른 에러 정보 반환")
      void handleAccessDenied_ShouldHaveCorrectErrorInfo() {
          // when
          CommonErrorCode errorCode = CommonErrorCode.HANDLE_ACCESS_DENIED;

          // then
          assertThat(errorCode.getHttpStatus()).isEqualTo(HttpStatus.FORBIDDEN);
          assertThat(errorCode.getCode()).isEqualTo("C006");
          assertThat(errorCode.getMessage()).isEqualTo("접근이 거부되었습니다");
      }

      @Test
      @DisplayName("모든 에러 코드가 유니크한지 확인")
      void allErrorCodes_ShouldBeUnique() {
          // given
          CommonErrorCode[] errorCodes = CommonErrorCode.values();

          // when & then
          for (int i = 0; i < errorCodes.length; i++) {
              for (int j = i + 1; j < errorCodes.length; j++) {
                  assertThat(errorCodes[i].getCode()).isNotEqualTo(errorCodes[j].getCode());
              }
          }
      }

      @Test
      @DisplayName("모든 에러 코드가 null이 아닌지 확인")
      void allErrorCodes_ShouldNotBeNull() {
          // given
          CommonErrorCode[] errorCodes = CommonErrorCode.values();

          // when & then
          for (CommonErrorCode errorCode : errorCodes) {
              assertThat(errorCode.getCode()).isNotNull();
              assertThat(errorCode.getMessage()).isNotNull();
              assertThat(errorCode.getHttpStatus()).isNotNull();
          }
      }

      @Test
      @DisplayName("에러 코드가 C로 시작하는지 확인")
      void allErrorCodes_ShouldStartWithC() {
          // given
          CommonErrorCode[] errorCodes = CommonErrorCode.values();

          // when & then
          for (CommonErrorCode errorCode : errorCodes) {
              assertThat(errorCode.getCode()).matches("[A-Z]\\d{3}");
          }
      }
}
