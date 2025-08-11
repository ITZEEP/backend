package org.scoula.domain.verification.exception;

import org.scoula.global.common.exception.IErrorCode;
import org.springframework.http.HttpStatus;

import lombok.RequiredArgsConstructor;

/**
 * 본인인증 도메인 에러 코드
 *
 * @author ITZeep Team
 * @since 1.0.0
 */
@RequiredArgsConstructor
public enum VerificationErrorCode implements IErrorCode {
      // 인증 관련 에러
      VERIFICATION_FAILED("VRF_001", HttpStatus.BAD_REQUEST, "본인인증에 실패했습니다"),
      INVALID_AUTH_KEY("VRF_002", HttpStatus.UNAUTHORIZED, "유효하지 않은 인증키입니다"),
      VERIFICATION_SERVICE_UNAVAILABLE(
              "VRF_003", HttpStatus.SERVICE_UNAVAILABLE, "본인인증 서비스를 사용할 수 없습니다"),
      VERIFICATION_TIMEOUT("VRF_004", HttpStatus.REQUEST_TIMEOUT, "본인인증 요청 시간이 초과되었습니다"),

      // API 통신 에러
      API_COMMUNICATION_ERROR("VRF_005", HttpStatus.INTERNAL_SERVER_ERROR, "API 통신 중 오류가 발생했습니다"),
      API_RESPONSE_PARSE_ERROR("VRF_006", HttpStatus.INTERNAL_SERVER_ERROR, "API 응답 파싱 중 오류가 발생했습니다");

      private final String code;
      private final HttpStatus httpStatus;
      private final String message;

      @Override
      public String getCode() {
          return code;
      }

      @Override
      public HttpStatus getHttpStatus() {
          return httpStatus;
      }

      @Override
      public String getMessage() {
          return message;
      }
}
