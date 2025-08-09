package org.scoula.domain.home.exception;

import org.scoula.global.common.exception.IErrorCode;
import org.springframework.http.HttpStatus;

import lombok.RequiredArgsConstructor;

/**
 * 매물 도메인 에러 코드
 *
 * @author ITZeep Team
 * @since 1.0.0
 */
@RequiredArgsConstructor
public enum HomeErrorCode implements IErrorCode {
      // 본인 인증 관련 에러
      HOME_IDENTITY_VERIFICATION_FAILED("HOME_001", HttpStatus.BAD_REQUEST, "본인 인증에 실패하였습니다"),
      HOME_IDENTITY_NOT_FOUND("HOME_002", HttpStatus.NOT_FOUND, "본인 인증 정보를 찾을 수 없습니다"),
      HOME_IDENTITY_UPDATE_FAILED(
              "HOME_003", HttpStatus.INTERNAL_SERVER_ERROR, "본인 인증 정보 업데이트에 실패하였습니다"),
      HOME_IDENTITY_INSERT_FAILED(
              "HOME_004", HttpStatus.INTERNAL_SERVER_ERROR, "본인 인증 정보 저장에 실패하였습니다"),

      // 매물 관련 일반 에러
      HOME_NOT_FOUND("HOME_010", HttpStatus.NOT_FOUND, "매물을 찾을 수 없습니다"),
      HOME_ACCESS_DENIED("HOME_011", HttpStatus.FORBIDDEN, "매물에 대한 권한이 없습니다");

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
